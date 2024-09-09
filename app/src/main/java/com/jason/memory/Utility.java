package com.jason.memory;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utility {
    private static final String TAG = "Utility";
    private static final String UPLOAD_URL = Config.UPLOAD_URL;
    private static final String BASE_URL = Config.BASE_URL;
    private static final String UPLOAD_DIR = "upload/";
    private static final String FILE_LIST_URL = BASE_URL + "listM.php?ext=csv";
    private static final String FILE_JSON_LIST_URL = BASE_URL + "listM.php?ext=";

    public interface SyncCallback {
        void onSyncComplete(boolean success);
    }

    public static void downloadJsonAndMergeServerData(Context context, String ext, DatabaseHelper dbHelper, SyncCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    List<String> fileList = fetchJSONFileList(ext);
                    if (fileList.isEmpty()) {
                        return false;
                    }

                    for (String fileName : fileList) {
                        File downloadedFile = downloadFile(fileName);
                        if (downloadedFile != null) {
                            mergeMemoryItemsFromFile(context, dbHelper, downloadedFile);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error during sync: " + e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                callback.onSyncComplete(success);
            }
        }.execute();
    }

    private static void mergeMemoryItemsFromFile(Context context, DatabaseHelper dbHelper, File file) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            MemoryItem[] memoryItems = gson.fromJson(reader, MemoryItem[].class);
            for (MemoryItem memoryItem : memoryItems) {
                if (memoryItem != null && memoryItem.getMemoryText() != null) {
                    MemoryItem existingMemoryItem = dbHelper.getMemoryItemByText(memoryItem.getMemoryText());
                    if (existingMemoryItem == null) {
                        dbHelper.addMemoryItem(memoryItem);
                    } else {
                        // Merge data (you might want to implement a more sophisticated merging logic)
                        existingMemoryItem.setDate(memoryItem.getDate());
                        dbHelper.updateMemoryItem(existingMemoryItem);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Utility", "Error in mergeMemoryItemsFromFile: " + e.getMessage());
            throw e;
        }
    }

    private static List<String> fetchJSONFileList(String ext) throws IOException {
        URL url = new URL(FILE_JSON_LIST_URL + ext);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            List<String> fileList = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                fileList.add(line.trim());
            }
            return fileList;
        } finally {
            connection.disconnect();
        }
    }

    private static File downloadFile(String fileName) throws IOException {
        URL url = new URL(BASE_URL + UPLOAD_DIR + fileName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            File tempFile = File.createTempFile("download", ".json");
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } finally {
            connection.disconnect();
        }
    }

    private static void mergePlacesFromFile(Context context, DatabaseHelper dbHelper, File file) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            Place[] places = gson.fromJson(reader, Place[].class);
            for (Place place : places) {
                if (place != null && place.getName() != null) {
                    Place existingPlace = dbHelper.getPlaceByName(place.getName());
                    if (existingPlace == null) {
                        dbHelper.addPlace(place);
                    } else {
                        // Merge data (you might want to implement a more sophisticated merging logic)
                        existingPlace.setLastVisited(Math.max(existingPlace.getLastVisited(), place.getLastVisited()));
                        existingPlace.setNumberOfVisits(Math.max(existingPlace.getNumberOfVisits(), place.getNumberOfVisits()));
                        dbHelper.updatePlace(existingPlace);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Utility", "Error in mergePlacesFromFile: " + e.getMessage());
            throw e;
        }
    }

    private static List<String> fetchFileList() throws IOException {
        URL url = new URL(FILE_LIST_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            List<String> fileList = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                fileList.add(line.trim());
            }
            return fileList;
        } finally {
            connection.disconnect();
        }
    }

    public static void finalizeActivity(Context context, DatabaseHelper dbHelper, StravaUploader stravaUploader,
                                        long activityId, long startTimestamp, long endTimestamp,
                                        FinalizeCallback callback, StravaUploadCallback stravaCallback) {

        Log.d(TAG, "--m-- Finalizing activity: " + activityId);

        LocationData startLocation = dbHelper.getFirstLocationAfterTimestamp(startTimestamp);
        LocationData endLocation = dbHelper.getLatestLocation();

        if (startLocation != null && endLocation != null) {
            Log.d(TAG, "--m-- Start and end locations found");
            double distance = calculateDistance(dbHelper, startTimestamp, endTimestamp);
            long elapsedTime = endTimestamp - startTimestamp;

            Log.d(TAG, "--m-- Calculated distance: " + distance + ", elapsed time: " + elapsedTime);

            ActivityData currentActivity = dbHelper.getActivity(activityId);
            String address = (currentActivity != null) ? currentActivity.getAddress() : "";

            if (address.isEmpty()) {
                address = endLocation.getSimpleAddress(context);
                Log.d(TAG, "--m-- Using end location address: " + address);
            }

            dbHelper.updateActivity(activityId, endTimestamp, startLocation.getId(), endLocation.getId(), distance, elapsedTime, address);
            Log.d(TAG, "--m-- Activity updated in database");
            currentActivity = dbHelper.getActivity(activityId);

            SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
            boolean uploadToServer = prefs.getBoolean(Config.PREF_UPLOAD_SERVER, false);
            boolean uploadToStrava = prefs.getBoolean(Config.PREF_UPLOAD_STRAVA, false);

            Log.d(TAG, "--m-- Upload preferences: Server=" + uploadToServer + ", Strava=" + uploadToStrava);

            if (uploadToServer) {
                File savedFile = saveActivityToFile(context, currentActivity, dbHelper);
                if (savedFile != null) {
                    Log.d(TAG, "--m-- Activity saved to file, uploading to server");
                    uploadFile(context, savedFile);
                } else {
                    Log.e(TAG, "--m-- Failed to save activity to file");
                }
            }

            if (uploadToStrava) {
                Log.d(TAG, "--m-- Uploading to Strava");
                uploadToStrava(context, dbHelper, stravaUploader, currentActivity, stravaCallback);
            } else {
                stravaCallback.onStravaUploadComplete(false);
            }

            callback.onFinalize();
        } else {
            Log.e(TAG, "--m-- Unable to save activity(" + activityId + "): location data missing");
            Toast.makeText(context, "Unable to save activity(" + activityId + "): location data missing", Toast.LENGTH_SHORT).show();
            callback.onFinalize();
        }
    }

    public interface StravaUploadCallback {
        void onStravaUploadComplete(boolean success);
    }


    private static double calculateDistance(DatabaseHelper dbHelper, long startTimestamp, long endTimestamp) {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, endTimestamp);
        double totalDistance = 0;
        if(locations == null || locations.size() < 2) {
            Log.e(TAG, "--m-- Not enough locations to calculate distance");
            return 0;
        }
        for (int i = 0; i < locations.size() - 1; i++) {
            LocationData start = locations.get(i);
            LocationData end = locations.get(i + 1);
            double segmentDistance = calculateDistanceBetweenPoints(start, end);
            totalDistance += segmentDistance;
            Log.v(TAG, "--m-- Segment distance: " + segmentDistance + ", Total so far: " + totalDistance);
        }
        Log.d(TAG, "--m-- Total calculated distance: " + totalDistance);
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
            Log.e(TAG, "--m-- No activity data to save");
            Toast.makeText(context, "No activity data to save", Toast.LENGTH_SHORT).show();
            return null;
        }


        // Detailed logging of ActivityData
        Log.d(TAG, "--m-- Saving ActivityData: " +
                "\n ID: " + activity.getId() +
                "\n Filename: " + activity.getFilename() +
                "\n Type: " + activity.getType() +
                "\n Name: " + activity.getName() +
                "\n Start Timestamp: " + new Date(activity.getStartTimestamp()) +
                "\n End Timestamp: " + new Date(activity.getEndTimestamp()) +
                "\n Start Location ID: " + activity.getStartLocationId() +
                "\n End Location ID: " + activity.getEndLocationId() +
                "\n Distance: " + activity.getDistance() + " km" +
                "\n Elapsed Time: " + formatElapsedTime(activity.getElapsedTime()) +
                "\n Address: " + activity.getAddress()
        );

        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = fileNameFormat.format(new Date(activity.getStartTimestamp())) + ".csv";
        Log.d(TAG, "--m-- Saving activity to file: " + fileName);

        File directory = Config.getDownloadDir();

        if (!directory.exists()) {
            boolean dirCreated = directory.mkdirs();
            Log.d(TAG, "--m-- Download directory created: " + dirCreated);
        }

        File file = new File(directory, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("x,y,d,t\n");

            List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());

            if (locations == null || locations.size() < 2) {
                String message = "The activity is too short or has insufficient location data to save.";
                Log.e(TAG, "--m-- " + message);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                return null;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());

            for (LocationData location : locations) {
                writer.append(String.format(Locale.US, "%.8f,%.8f,%s\n",
                        location.getLatitude(),
                        location.getLongitude(),
                        dateFormat.format(new Date(location.getTimestamp()))));
            }

            Log.d(TAG, "--m-- Activity saved successfully to " + fileName);
            Toast.makeText(context, "Activity saved to " + fileName, Toast.LENGTH_SHORT).show();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "--m-- Failed to save activity: " + e.getMessage(), e);
            Toast.makeText(context, "Failed to save activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private static String formatElapsedTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
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
                        Log.d(TAG, "--m-- Upload successful. Server response: " + response.toString());
                        return "Upload successful. Server response: " + response.toString();
                    } else {
                        Log.e(TAG, "--m-- Upload failed. Server returned: " + responseCode);
                        return "Upload failed. Server returned: " + responseCode;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "--m-- Upload failed: " + e.getMessage(), e);
                    return "Upload failed: " + e.getMessage();
                } finally {
                    if (httpUrlConnection != null) {
                        httpUrlConnection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d(TAG, "--m-- Upload result: " + result);
                Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }


    public static void uploadToStrava(Context context, DatabaseHelper dbHelper, StravaUploader stravaUploader,
                                      ActivityData activity, StravaUploadCallback callback) {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
        File gpxFile = stravaUploader.generateGpxFile(locations);

        if (gpxFile != null) {
            Log.d(TAG, "--m-- GPX file generated, authenticating with Strava");
            stravaUploader.authenticate(gpxFile, activity.getName(),
                    "Activity recorded using MyActivity app", activity.getType(),
                    success -> {
                        if (success) {
                            Log.d(TAG, "--m-- Strava upload completed successfully");
                        } else {
                            Log.e(TAG, "--m-- Strava upload failed");
                        }
                        callback.onStravaUploadComplete(success);
                    });
        } else {
            Log.e(TAG, "--m-- Unable to upload: GPX file generation failed");
            Toast.makeText(context, "Unable to upload: GPX file generation failed", Toast.LENGTH_SHORT).show();
            callback.onStravaUploadComplete(false);
        }
    }


    public static String calculateCalories(Context context, double distanceKm) {
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        float weightKg = prefs.getFloat(Config.WEIGHT, 75f); // Default weight 75 kg if not set
        double metValue = 7.0;
        double timeHours = distanceKm / 10.0; // Assuming 10 km/h average speed
        double calories = metValue * weightKg * timeHours;
        Log.d(TAG, "--m-- Calculated calories: " + calories + " for distance: " + distanceKm + "km and weight: " + weightKg + "kg");
        return String.format(Locale.getDefault(), "%.0f", calories);
    }

    public interface FinalizeCallback {
        void onFinalize();
    }
}