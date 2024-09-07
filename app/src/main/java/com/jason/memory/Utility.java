package com.jason.memory;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utility {
    private static final String UPLOAD_URL = Config.UPLOAD_URL;

    public static void finalizeActivity(Context context, @NonNull DatabaseHelper dbHelper, StravaUploader stravaUploader,
                                        long activityId, long startTimestamp, long endTimestamp,
                                        FinalizeCallback callback) {
        LocationData startLocation = dbHelper.getFirstLocationAfterTimestamp(startTimestamp);
        LocationData endLocation = dbHelper.getLatestLocation();

        if (startLocation != null && endLocation != null) {
            double distance = calculateDistance(dbHelper, startTimestamp, endTimestamp);
            long elapsedTime = endTimestamp - startTimestamp;

            ActivityData currentActivity = dbHelper.getActivity(activityId);
            String address = (currentActivity != null) ? currentActivity.getAddress() : "";

            if (address.isEmpty()) {
                address = endLocation.getSimpleAddress(context);
            }

            dbHelper.updateActivity(activityId, endTimestamp, startLocation.getId(), endLocation.getId(), distance, elapsedTime, address);

            SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
            boolean uploadToServer = prefs.getBoolean(Config.PREF_UPLOAD_SERVER, false);
            boolean uploadToStrava = prefs.getBoolean(Config.PREF_UPLOAD_STRAVA, false);

            if (uploadToServer) {
                File savedFile = saveActivityToFile(context, currentActivity, dbHelper);
                if (savedFile != null) {
                    uploadFile(context, savedFile);
                }
            }

            if (uploadToStrava) {
                uploadToStrava(context, dbHelper, stravaUploader, currentActivity);
            }

            callback.onFinalize();
        } else {
            Toast.makeText(context, "Unable to save activity(" + activityId + "): location data missing", Toast.LENGTH_SHORT).show();
            callback.onFinalize();
        }
    }


    private static double calculateDistance(DatabaseHelper dbHelper, long startTimestamp, long endTimestamp) {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, endTimestamp);
        double totalDistance = 0;
        if(locations == null) return 0;
        if (locations.size() < 2) return 0;
        for (int i = 0; i < locations.size() - 1; i++) {
            LocationData start = locations.get(i);
            LocationData end = locations.get(i + 1);
            totalDistance += calculateDistanceBetweenPoints(start, end);
        }
        return totalDistance;
    }

    private static double calculateDistanceBetweenPoints(@NonNull LocationData start, @NonNull LocationData end) {
        double earthRadius = 6371; // in kilometers
        double dLat = Math.toRadians(end.getLatitude() - start.getLatitude());
        double dLon = Math.toRadians(end.getLongitude() - start.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c; // Distance in kilometers
    }

    @Nullable
    public static File saveActivityToFile(Context context, ActivityData activity, DatabaseHelper dbHelper) {
        if (activity == null) {
            Toast.makeText(context, "No activity data to save", Toast.LENGTH_SHORT).show();
            return null;
        }

        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = fileNameFormat.format(new Date(activity.getStartTimestamp())) + ".csv";

        File directory = Config.getDownloadDir();

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("x,y,d,t\n");

            List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());

            for (LocationData location : locations) {
                writer.append(String.format(Locale.US, "%.8f,%.8f,%s\n",
                        location.getLatitude(),
                        location.getLongitude(),
                        dateFormat.format(new Date(location.getTimestamp()))));
            }

            Toast.makeText(context, "Activity saved to " + fileName, Toast.LENGTH_SHORT).show();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static void uploadFile(Context context, File file) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection httpUrlConnection = null;
                try {
                    URL url = new URL(UPLOAD_URL);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setUseCaches(false);
                    httpUrlConnection.setDoInput(true);
                    httpUrlConnection.setDoOutput(true);
                    httpUrlConnection.setConnectTimeout(15000);

                    httpUrlConnection.setRequestMethod("POST");
                    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");

                    String boundary = "*****";
                    String crlf = "\r\n";
                    String twoHyphens = "--";

                    httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

                    request.writeBytes(twoHyphens + boundary + crlf);
                    request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + crlf);
                    request.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName()) + crlf);
                    request.writeBytes("Content-Transfer-Encoding: binary" + crlf);
                    request.writeBytes(crlf);
                    request.flush();

                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        request.write(buffer, 0, bytesRead);
                    }

                    request.writeBytes(crlf);
                    request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

                    request.flush();
                    request.close();

                    int responseCode = httpUrlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        return "Upload successful. Server response: " + response.toString();
                    } else {
                        return "Upload failed. Server returned: " + responseCode;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return "Upload failed: " + e.getMessage();
                } finally {
                    if (httpUrlConnection != null) {
                        httpUrlConnection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    public static void uploadToStrava(Context context, DatabaseHelper dbHelper, StravaUploader stravaUploader, ActivityData activity) {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
        File gpxFile = stravaUploader.generateGpxFile(locations);

        if (gpxFile != null) {
            stravaUploader.authenticate(gpxFile, activity.getName(),
                    "Activity recorded using MyActivity app", activity.getType());
        } else {
            Toast.makeText(context, "Unable to upload: GPX file generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    public interface FinalizeCallback {
        void onFinalize();
    }

    public interface DistanceCallback {
        void onDistanceCalculated(double distance);
    }

    public static String  calculateCalories(Context context, double distanceKm) {
        // Get the runner's weight from SharedPreferences or a constant
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        float weightKg = prefs.getFloat(Config.WEIGHT, 75f); // Default weight 75 kg if not set

        // MET value for running (varies based on speed, using an average value here)
        double metValue = 7.0;

        // Time in hours (assuming the distance is covered at a moderate pace)
        double timeHours = distanceKm / 10.0; // Assuming 10 km/h average speed

        // Calorie calculation formula
        double calories = metValue * weightKg * timeHours;
        return String.format(Locale.getDefault(), "%.0f", calories);
    }
}