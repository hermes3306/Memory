package com.jason.memory;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://58.233.69.198/moment/";
    private static final String UPLOAD_DIR = "upload/";
    private static final String FILE_LIST_URL = BASE_URL + "list.php?ext=csv";

    private Button fileDownloadButton;
    private ProgressBar downloadProgressBar;
    private TextView downloadStatusTextView;
    private List<String> fileList = new ArrayList<>();
    private int currentFileIndex = 0;
    private long downloadId;

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                if (currentFileIndex < fileList.size() - 1) {
                    currentFileIndex++;
                    downloadFile(fileList.get(currentFileIndex));
                } else {
                    downloadStatusTextView.setText("모든 파일 다운로드 완료");
                    fileDownloadButton.setEnabled(true);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        fileDownloadButton = findViewById(R.id.fileDownloadButton);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        downloadStatusTextView = findViewById(R.id.downloadStatusTextView);

        fileDownloadButton.setOnClickListener(v -> fetchFileList());

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    private void fetchFileList() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, FILE_LIST_URL, null,
                response -> {
                    fileList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            fileList.add(response.getString(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!fileList.isEmpty()) {
                        currentFileIndex = 0;
                        downloadFile(fileList.get(currentFileIndex));
                    } else {
                        Toast.makeText(this, "다운로드할 파일이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "파일 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        );
        queue.add(jsonArrayRequest);
    }

    private void downloadFile(String fileName) {
        String fileUrl = BASE_URL + UPLOAD_DIR + fileName;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle("파일 다운로드");
        request.setDescription("다운로드 중: " + fileName);

        File destinationDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Memory");
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        request.setDestinationUri(Uri.fromFile(new File(destinationDir, fileName)));

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = downloadManager.enqueue(request);

        fileDownloadButton.setEnabled(false);
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadStatusTextView.setVisibility(View.VISIBLE);
        updateDownloadProgress();
    }

    private void updateDownloadProgress() {
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadId);
                Cursor cursor = ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).query(q);
                cursor.moveToFirst();
                int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false;
                }
                final int dlProgress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                runOnUiThread(() -> {
                    downloadProgressBar.setProgress(dlProgress);
                    downloadStatusTextView.setText(String.format("다운로드 중: %s (%d%%)", fileList.get(currentFileIndex), dlProgress));
                });
                cursor.close();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
    }
}