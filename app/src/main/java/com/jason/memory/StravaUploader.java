package com.jason.memory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import androidx.browser.customtabs.CustomTabsIntent;

public class StravaUploader {
    private static final String TAG = "StravaUploader";
    private static final String CLIENT_ID = "67174";
    private static final String CLIENT_SECRET = "11deb64d5fc70d28aed865992a6792f28edce3c6";
    private static final String AUTHORIZATION_ENDPOINT = "https://www.strava.com/oauth/mobile/authorize";
    private static final String TOKEN_ENDPOINT = "https://www.strava.com/oauth/token";
    private static final String UPLOAD_URL = "https://www.strava.com/api/v3/uploads";
    private static final String ACTIVITIES_URL = "https://www.strava.com/api/v3/activities";
    private static final String SCOPE = "activity:write,activity:read_all";
    public static final String REDIRECT_URI ="http://localhost:8080/callback";

    private Context context;
    private File gpxFile;
    private String activityName;
    private String activityDescription;
    private String activityType;

    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private ExecutorService executorService;

    public static final int AUTH_REQUEST_CODE = 1001;

    public interface StravaUploadCallback {
        void onStravaUploadComplete(boolean success);
    }

    private StravaUploadCallback stravaUploadCallback;


    public StravaUploader(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "--m-- StravaUploader initialized");
    }

    public File generateGpxFile(List<LocationData> locations) {
        Log.d(TAG, "--m-- Generating GPX file");
        if (locations == null || locations.isEmpty()) {
            Log.e(TAG, "--m-- No location data found for the activity");
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        File gpxFile = new File(Config.getTmpDir(),"activity.gpx");

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(gpxFile), StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<gpx version=\"1.1\" creator=\"MyActivity App\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
            writer.write("  <metadata>\n");
            writer.write("    <name>MyActivity Recorded Activity</name>\n");
            writer.write("    <time>" + dateFormat.format(new Date(locations.get(0).getTimestamp())) + "</time>\n");
            writer.write("  </metadata>\n");
            writer.write("  <trk>\n");
            writer.write("    <name>MyActivity Track</name>\n");
            writer.write("    <trkseg>\n");

            for (LocationData location : locations) {
                writer.write("      <trkpt lat=\"" + location.getLatitude() + "\" lon=\"" + location.getLongitude() + "\">\n");
                writer.write("        <ele>" + location.getAltitude() + "</ele>\n");
                writer.write("        <time>" + dateFormat.format(new Date(location.getTimestamp())) + "</time>\n");
                writer.write("      </trkpt>\n");
            }

            writer.write("    </trkseg>\n");
            writer.write("  </trk>\n");
            writer.write("</gpx>");

            Log.d(TAG, "--m-- GPX file generated successfully: " + gpxFile.getAbsolutePath());
            return gpxFile;
        } catch (IOException e) {
            Log.e(TAG, "--m-- Error generating GPX file", e);
            return null;
        }
    }

    public void authenticate(File gpxFile, String name, String description, String activityType, StravaUploadCallback callback) {
        Log.d(TAG, "--m-- Starting authentication process");
        this.gpxFile = gpxFile;
        this.activityName = name;
        this.activityDescription = description;
        this.activityType = activityType;
        this.stravaUploadCallback = callback;


        String authUrl = AUTHORIZATION_ENDPOINT + "?client_id=" + CLIENT_ID +
                "&response_type=code&redirect_uri=" + REDIRECT_URI +
                "&scope=" + SCOPE;
        Log.d(TAG, "--m-- Auth URL: " + authUrl);

        Intent intent = new Intent(context, StravaAuthActivity.class);
        intent.putExtra("AUTH_URL", authUrl);
        ((Activity) context).startActivityForResult(intent, AUTH_REQUEST_CODE);
        Log.d(TAG, "--m-- Started StravaAuthActivity for result");
    }

    private void startLocalServer() {
        Log.d(TAG, "--m-- Starting local server");
        executorService.submit(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                isServerRunning = true;
                Log.i(TAG, "--m-- Local server started on port 8080");

                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, "--m-- Client connected to local server");
                    handleClientRequest(clientSocket);
                }
            } catch (IOException e) {
                if(stopLocalServer_called) {
                    Log.e(TAG, "--m-- Stop local server called: " + e.getMessage());
                    showToast("Stop local server called: " + e.getMessage());
                    stopLocalServer_called = false;
                } else {
                    Log.e(TAG, "--m-- Error starting local server: " + e.getMessage());
                    showToast("Failed to start local server: " + e.getMessage());
                }
            }
        });
    }

    public void handleAuthResult(int resultCode, Intent data) {
        Log.d(TAG, "--m-- Handling auth result. Result code: " + resultCode);


        if (resultCode == Activity.RESULT_OK) {
            String authCode = data.getStringExtra("AUTH_CODE");
            if (authCode != null) {
                Log.d(TAG, "--m-- Authorization code received: " + authCode);
                showToast("Authorization code: " + authCode);
                exchangeAuthorizationCode(authCode);
            }
        } else {
            Log.e(TAG, "--m-- Authorization failed");
            showToast("Authorization failed");
            if (stravaUploadCallback != null) {
                stravaUploadCallback.onStravaUploadComplete(false);
            }
        }
    }

    private void handleClientRequest(Socket clientSocket) {
        Log.d(TAG, "--m-- Handling client request");
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()
        ) {
            String request = in.readLine();
            if (request != null && request.startsWith("GET /callback")) {
                String response = "HTTP/1.1 200 OK\r\n\r\nAuthorization successful! You can close this window.";
                out.write(response.getBytes(StandardCharsets.UTF_8));

                int codeIndex = request.indexOf("code=");
                if (codeIndex != -1) {
                    String code = request.substring(codeIndex + 5).split(" ")[0];
                    Log.d(TAG, "--m-- Authorization code received: " + code);
                    showToast("Authorization code: " + code);
                    exchangeAuthorizationCode(code);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "--m-- Error handling client request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "--m-- Error closing client socket: " + e.getMessage());
            }
        }
    }

    boolean stopLocalServer_called = false;
    private void stopLocalServer() {
        Log.d(TAG, "--m-- Stopping local server");
        isServerRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                stopLocalServer_called = true;
                serverSocket.close();
                Log.d(TAG, "--m-- Local server stopped");
            } catch (IOException e) {
                Log.e(TAG, "--m-- Error closing server socket: " + e.getMessage());
            }
        }
    }

    public void handleAuthorizationResponse(Uri uri) {
        Log.d(TAG, "--m-- Handling authorization response");
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                Log.d(TAG, "--m-- Authorization code received, exchanging for token");
                exchangeAuthorizationCode(code);
            } else {
                Log.e(TAG, "--m-- No authorization code found in the response");
                showToast("Authorization failed: No code received");
            }
        }
    }

    private void exchangeAuthorizationCode(String authCode) {
        Log.d(TAG, "--m-- Exchanging authorization code for access token");
        new Thread(() -> {
            try {
                URL url = new URL(TOKEN_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "client_id=" + CLIENT_ID +
                        "&client_secret=" + CLIENT_SECRET +
                        "&code=" + authCode +
                        "&grant_type=authorization_code";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = postData.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String accessToken = jsonResponse.getString("access_token");
                        Log.d(TAG, "--m-- Access token received, initiating activity upload");
                        String truncatedToken = accessToken.substring(0, 5) + "..." +
                                accessToken.substring(accessToken.length() - 5);

                        showToast("Access token received: " + truncatedToken + "\nUploading activity...");
                        uploadActivity(gpxFile, activityName, activityDescription, activityType, accessToken);
                    }
                } else {
                    Log.e(TAG, "--m-- Failed to get access token: " + responseCode);
                    showToast("Failed to get access token: " + responseCode);
                }
                stopLocalServer();

            } catch (IOException | JSONException e) {
                Log.e(TAG, "--m-- Error during token exchange", e);
                showToast("Error during token exchange: " + e.getMessage());
                if (stravaUploadCallback != null) {
                    stravaUploadCallback.onStravaUploadComplete(false);
                }
            }

        }).start();
    }

    private void uploadActivity(File gpxFile, String name, String description, String activityType, String accessToken) {
        Log.d(TAG, "--m-- Starting activity upload");
        new Thread(() -> {
            try {
                String boundary = "*****" + System.currentTimeMillis() + "*****";
                URL url = new URL(UPLOAD_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
                    os.writeBytes("--" + boundary + "\r\n");
                    os.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + gpxFile.getName() + "\"\r\n");
                    os.writeBytes("Content-Type: application/gpx+xml\r\n\r\n");

                    FileInputStream fileInputStream = new FileInputStream(gpxFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    fileInputStream.close();

                    os.writeBytes("\r\n--" + boundary + "\r\n");
                    os.writeBytes("Content-Disposition: form-data; name=\"data_type\"\r\n\r\n");
                    os.writeBytes("gpx\r\n");

                    os.writeBytes("--" + boundary + "\r\n");
                    os.writeBytes("Content-Disposition: form-data; name=\"name\"\r\n\r\n");
                    os.writeBytes(name + "\r\n");

                    os.writeBytes("--" + boundary + "\r\n");
                    os.writeBytes("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
                    os.writeBytes(description + "\r\n");

                    os.writeBytes("--" + boundary + "\r\n");
                    os.writeBytes("Content-Disposition: form-data; name=\"activity_type\"\r\n\r\n");
                    os.writeBytes(activityType + "\r\n");

                    os.writeBytes("--" + boundary + "--\r\n");
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String uploadId = jsonResponse.getString("id");
                    Log.d(TAG, "--m-- Upload initiated. Upload ID: " + uploadId);
                    checkUploadStatus(accessToken, uploadId);
                } else {
                    Log.e(TAG, "--m-- Failed to upload activity: " + responseCode);
                    showToast("Failed to upload activity: " + responseCode);
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "--m-- Error during activity upload", e);
                if (stravaUploadCallback != null) {
                    stravaUploadCallback.onStravaUploadComplete(false);
                }
            }
        }).start();
    }

    private void checkUploadStatus(String accessToken, String uploadId) throws IOException, JSONException {
        Log.d(TAG, "--m-- Checking upload status for ID: " + uploadId);
        for (int i = 0; i < 60; i++) {
            URL url = new URL(UPLOAD_URL + "/" + uploadId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.getString("status");
                Log.d(TAG, "--m-- Upload status: " + status);
                if ("Your activity is ready.".equals(status)) {
                    String activityId = jsonResponse.getString("activity_id");
                    Log.d(TAG, "--m-- Upload successful! Activity ID: " + activityId);
                    showToast("Upload successful! Activity ID: " + activityId);
                    getActivityDetails(accessToken, activityId);
                    return;
                } else if (jsonResponse.has("error") && !jsonResponse.isNull("error")) {
                    Log.e(TAG, "--m-- Upload failed: " + jsonResponse.getString("error"));
                    showToast("Upload failed: " + jsonResponse.getString("error"));
                    return;
                }
            }

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Log.e(TAG, "--m-- Sleep interrupted", e);
            }
        }

        Log.e(TAG, "--m-- Upload processing timed out");
        showToast("Upload processing timed out");
        if (stravaUploadCallback != null) {
            stravaUploadCallback.onStravaUploadComplete(false);
        }

    }

    private void getActivityDetails(String accessToken, String activityId) throws IOException, JSONException {
        Log.d(TAG, "--m-- Retrieving activity details for ID: " + activityId);
        URL url = new URL(ACTIVITIES_URL + "/" + activityId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject activityDetails = new JSONObject(response.toString());
            displayActivityDetails(activityDetails);
        } else {
            Log.e(TAG, "--m-- Failed to retrieve activity details: " + responseCode);
            showToast("Failed to retrieve activity details: " + responseCode);
        }
    }

    private void displayActivityDetails(JSONObject activityDetails) throws JSONException {
        Log.d(TAG, "--m-- Displaying activity details");
        String name = activityDetails.getString("name");
        String type = activityDetails.getString("type");
        double distance = activityDetails.getDouble("distance") / 1000; // Convert to km
        int movingTime = activityDetails.getInt("moving_time");
        int elapsedTime = activityDetails.getInt("elapsed_time");
        double elevationGain = activityDetails.getDouble("total_elevation_gain");
        String startDate = activityDetails.getString("start_date_local");
        String activityUrl = "https://www.strava.com/activities/" + activityDetails.getString("id");

        String details = String.format(
                "Activity uploaded successfully!\n\n" +
                        "Name: %s\n" +
                        "Type: %s\n" +
                        "Distance: %.2f km\n" +
                        "Moving Time: %d seconds\n" +
                        "Elapsed Time: %d seconds\n" +
                        "Elevation Gain: %.2f meters\n" +
                        "Start Date: %s\n" +
                        "Activity URL: %s",
                name, type, distance, movingTime, elapsedTime, elevationGain, startDate, activityUrl
        );

        Log.d(TAG, "--m-- Activity Details:\n" + details);

        if (stravaUploadCallback != null) {
            stravaUploadCallback.onStravaUploadComplete(true);
        }

        showToast("Activity uploaded successfully!");
        // You might want to show this information in a dialog or a new activity
    }

    private void showToast(final String message) {
        Log.d(TAG, "--m-- Showing toast: " + message);
        ((Activity) context).runOnUiThread(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }
}