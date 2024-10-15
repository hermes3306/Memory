package com.jason.memory.lab;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.annotations.SerializedName;
import com.jason.memory.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;


public class StravaActivity extends AppCompatActivity {
    private static final String TAG = StravaActivity.class.getSimpleName();
    private static final String BASE_URL = "https://www.strava.com/";
    private static final String AUTHORIZATION_URL = BASE_URL + "oauth/mobile/authorize";
    private static final String TOKEN_URL = BASE_URL + "oauth/token";
    private static final String API_URL = BASE_URL + "api/v3/";

    // TODO: Move these to a secure storage or use Android's BuildConfig
    private static final String CLIENT_ID = "67174";
    private static final String CLIENT_SECRET = "11deb64d5fc70d28aed865992a6792f28edce3c6";
    public static final String REDIRECT_URI = "http://localhost";


    private Button loginButton;
    private RecyclerView activitiesRecyclerView;
    private ActivityAdapter activityAdapter;
    private StravaApiService stravaApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "--m-- onCreate: Starting StravaActivity");
        setContentView(R.layout.activity_strava);

        loginButton = findViewById(R.id.loginButton);
        activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);

        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Login button clicked");
            startAuthorization();
        });

        setupRecyclerView();
        setupRetrofit();
    }

    private void setupRecyclerView() {
        Log.d(TAG, "--m-- Setting up RecyclerView");
        if (activitiesRecyclerView == null) {
            Log.e(TAG, "--m-- RecyclerView is null!");
            return;
        }
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new ActivityAdapter();
        activitiesRecyclerView.setAdapter(activityAdapter);
        Log.d(TAG, "--m-- RecyclerView setup complete");
    }

    private void setupRetrofit() {
        Log.d(TAG, "--m-- Setting up Retrofit");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        stravaApiService = retrofit.create(StravaApiService.class);
        Log.d(TAG, "--m-- Retrofit setup complete");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "--m-- onNewIntent called");
        Uri uri = intent.getData();
        if (uri != null) {
            Log.d(TAG, "--m-- Received URI: " + uri.toString());
            if ("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost())) {
                String code = uri.getQueryParameter("code");
                if (code != null) {
                    Log.d(TAG, "--m-- Authorization code received: " + code);
                    exchangeCodeForToken(code);
                } else {
                    Log.e(TAG, "--m-- No authorization code found in the callback URI");
                    Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "--m-- Received URI does not match expected scheme and host");
            }
        } else {
            Log.e(TAG, "--m-- Received null URI in onNewIntent");
        }
    }

    private void handleAuthorization() {
        Log.d(TAG, "--m-- Handling authorization");
        String authUrl = "https://www.strava.com/oauth/authorize" +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&response_type=code" +
                "&approval_prompt=auto" +
                "&scope=activity:read_all";

        Log.d(TAG, "--m-- Full Authorization URL: " + authUrl);

        WebView webView = new WebView(this);
        setContentView(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "--m-- Redirected URL: " + url);
                if (url.startsWith(REDIRECT_URI)) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    if (code != null) {
                        Log.d(TAG, "--m-- Authorization code received: " + code);
                        exchangeCodeForToken(code);
                        return true;
                    } else {
                        Log.e(TAG, "--m-- No authorization code found in the callback URI");
                        Toast.makeText(StravaActivity.this, "Authorization failed", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "--m-- Page finished loading: " + url);
            }
        });

        webView.loadUrl(authUrl);
    }

    private void startAuthorization() {
        Log.d(TAG, "--m-- Starting authorization process");
        handleAuthorization();
    }

    private void exchangeCodeForToken(String code) {
        Log.d(TAG, "--m-- Exchanging code for token: " + code);
        Call<TokenResponse> call = stravaApiService.getAccessToken(
                CLIENT_ID, CLIENT_SECRET, code, "authorization_code");

        call.enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccessToken();
                    Log.d(TAG, "--m-- Received access token: " + accessToken);
                    runOnUiThread(() -> {
                        setContentView(R.layout.activity_strava);
                        setupRecyclerView();
                        fetchActivities(accessToken);
                    });
                } else {
                    Log.e(TAG, "--m-- Failed to get access token. Response: " + response.toString());
                    runOnUiThread(() -> Toast.makeText(StravaActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Log.e(TAG, "--m-- Network error while getting access token", t);
                runOnUiThread(() -> Toast.makeText(StravaActivity.this, "Network error", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void fetchActivities(String accessToken) {
        Log.d(TAG, "--m-- Fetching activities with token: " + accessToken);
        Call<List<ActivityResponse>> call = stravaApiService.getActivities("Bearer " + accessToken);
        call.enqueue(new Callback<List<ActivityResponse>>() {
            @Override
            public void onResponse(Call<List<ActivityResponse>> call, Response<List<ActivityResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ActivityResponse> activities = response.body();
                    Log.d(TAG, "--m-- Received activities: " + activities.size());
                    for (int i = 0; i < Math.min(5, activities.size()); i++) {
                        ActivityResponse activity = activities.get(i);
                        Log.d(TAG, "--m-- Activity " + i + ": " + activity.getName() + ", Type: " + activity.getType() + ", Distance: " + activity.getDistance());
                    }
                    runOnUiThread(() -> {
                        Log.d(TAG, "--m-- Updating RecyclerView with fetched activities");
                        loginButton.setVisibility(View.GONE);
                        activitiesRecyclerView.setVisibility(View.VISIBLE);
                        activityAdapter.setActivities(activities);
                        activityAdapter.notifyDataSetChanged();
                        Log.d(TAG, "--m-- RecyclerView update complete");
                        Log.d(TAG, "--m-- RecyclerView item count: " + activityAdapter.getItemCount());
                    });
                } else {
                    Log.e(TAG, "--m-- Failed to fetch activities. Response: " + response.toString());
                    runOnUiThread(() -> Toast.makeText(StravaActivity.this, "Failed to fetch activities", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<List<ActivityResponse>> call, Throwable t) {
                Log.e(TAG, "--m-- Network error while fetching activities", t);
                runOnUiThread(() -> Toast.makeText(StravaActivity.this, "Network error", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private interface StravaApiService {
        @POST("oauth/token")
        Call<TokenResponse> getAccessToken(
                @Query("client_id") String clientId,
                @Query("client_secret") String clientSecret,
                @Query("code") String code,
                @Query("grant_type") String grantType
        );

        @GET("athlete/activities")
        Call<List<ActivityResponse>> getActivities(@Header("Authorization") String token);
    }

    private static class TokenResponse {
        @SerializedName("access_token")
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }
    }

    private static class ActivityResponse {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        @SerializedName("distance")
        private float distance;

        @SerializedName("moving_time")
        private int movingTime;

        @SerializedName("type")
        private String type;

        @SerializedName("start_date")
        private String startDate;

        // Getters
        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public float getDistance() {
            return distance;
        }

        public int getMovingTime() {
            return movingTime;
        }

        public String getType() {
            return type;
        }

        public String getStartDate() {
            return startDate;
        }
    }


    private static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private List<ActivityResponse> activities = new ArrayList<>();

        public void setActivities(List<ActivityResponse> activities) {
            Log.d(TAG, "--m-- Setting activities in adapter: " + activities.size());
            this.activities = activities;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "--m-- onCreateViewHolder called");
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_strava_activity, parent, false);
            Log.d(TAG, "--m-- View created: " + (view != null));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.d(TAG, "--m-- onBindViewHolder called for position: " + position);
            ActivityResponse activity = activities.get(position);
            holder.nameTextView.setText(activity.getName());
            holder.distanceTextView.setText(String.format("%.2f km", activity.getDistance() / 1000));
            holder.typeTextView.setText(activity.getType());
            holder.dateTextView.setText(activity.getStartDate());
            Log.d(TAG, "--m-- Item bound: " + activity.getName());
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "--m-- getItemCount called, returning: " + activities.size());
            return activities.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView, distanceTextView, typeTextView, dateTextView;

            ViewHolder(View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                distanceTextView = itemView.findViewById(R.id.distanceTextView);
                typeTextView = itemView.findViewById(R.id.typeTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
                Log.d(TAG, "--m-- ViewHolder created: " +
                        (nameTextView != null) + ", " +
                        (distanceTextView != null) + ", " +
                        (typeTextView != null) + ", " +
                        (dateTextView != null));
            }
        }



    }


}