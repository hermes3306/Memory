package com.jason.memory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StravaAuthActivity extends AppCompatActivity {
    private static final String TAG = "StravaAuthActivity";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new StravaWebViewClient());
        setContentView(webView);

        String authUrl = getIntent().getStringExtra("AUTH_URL");
        if (authUrl != null) {
            Log.d(TAG, "Loading auth URL: " + authUrl);
            webView.loadUrl(authUrl);
        } else {
            Log.e(TAG, "No auth URL provided");
            Toast.makeText(this, "Error: No authentication URL provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private class StravaWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Log.d(TAG, "URL loading: " + url);

            if (url.startsWith(StravaUploader.REDIRECT_URI)) {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                if (code != null) {
                    Log.d(TAG, "Authorization code received: " + code);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("AUTH_CODE", code);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    return true;
                } else {
                    Log.e(TAG, "No authorization code in redirect URI");
                    Toast.makeText(StravaAuthActivity.this, "Authentication failed: No code received", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "Page finished loading: " + url);
        }
    }
}