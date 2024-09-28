package com.jason.daily;

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

    public static void uploadLocationsToServer(Context context, List<LocationData> locations, String fileName) {
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
                    request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + crlf);
                    request.writeBytes("Content-Type: text/plain" + crlf);
                    request.writeBytes(crlf);

                    for (LocationData location : locations) {
                        String locationString = String.format(Locale.US, "%.8f,%.8f,%.2f,%d\n",
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAltitude(),
                                location.getTimestamp());
                        request.writeBytes(locationString);
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

}