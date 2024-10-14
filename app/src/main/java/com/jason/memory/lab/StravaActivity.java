package com.jason.memory.lab;


import com.jason.memory.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StravaActivity extends AppCompatActivity {
    private static final String TAG = StravaActivity.class.getSimpleName();
    private static final String BASE_URL = "https://www.strava.com/";
    private static final String AUTHORIZATION_URL = BASE_URL + "oauth/mobile/authorize";
    private static final String TOKEN_URL = BASE_URL + "oauth/token";
    private static final String API_URL = BASE_URL + "api/v3/";

    // TODO: Move these to a secure storage or use Android's BuildConfig
    private static final String CLIENT_ID = "67174";
    private static final String CLIENT_SECRET = "11deb64d5fc70d28aed865992a6792f28edce3c6";
    public static final String REDIRECT_URI = "memorylab://strava-callback";

    private Button loginButton;
    private RecyclerView activitiesRecyclerView;
    private ActivityAdapter activityAdapter;
    private StravaApiService stravaApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strava);

        loginButton = findViewById(R.id.loginButton);
        activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);

        loginButton.setOnClickListener(v -> startAuthorization());

        setupRecyclerView();
        setupRetrofit();
    }

    private void setupRecyclerView() {
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new ActivityAdapter();
        activitiesRecyclerView.setAdapter(activityAdapter);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        stravaApiService = retrofit.create(StravaApiService.class);
    }

    private void startAuthorization() {
        String authUrl = AUTHORIZATION_URL + "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) + // Ensure this is properly encoded
                "&response_type=code" +
                "&approval_prompt=auto" +
                "&scope=activity:read_all";

        Log.d(TAG, "--m-- Authorization URL: " + authUrl);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null && "memorylab".equals(uri.getScheme()) && "strava-callback".equals(uri.getHost())) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                Log.d(TAG, "Authorization code received: " + code);
                exchangeCodeForToken(code);
            } else {
                Log.e(TAG, "No authorization code found in the callback URI");
                Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exchangeCodeForToken(String code) {
        Call<TokenResponse> call = stravaApiService.getAccessToken(
                CLIENT_ID, CLIENT_SECRET, code, "authorization_code");

        call.enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccessToken();
                    fetchActivities(accessToken);
                } else {
                    Toast.makeText(StravaActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Toast.makeText(StravaActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchActivities(String accessToken) {
        Call<List<ActivityResponse>> call = stravaApiService.getActivities("Bearer " + accessToken);
        call.enqueue(new Callback<List<ActivityResponse>>() {
            @Override
            public void onResponse(Call<List<ActivityResponse>> call, Response<List<ActivityResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activityAdapter.setActivities(response.body());
                } else {
                    Toast.makeText(StravaActivity.this, "Failed to fetch activities", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ActivityResponse>> call, Throwable t) {
                Toast.makeText(StravaActivity.this, "Network error", Toast.LENGTH_SHORT).show();
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
        public long getId() { return id; }
        public String getName() { return name; }
        public float getDistance() { return distance; }
        public int getMovingTime() { return movingTime; }
        public String getType() { return type; }
        public String getStartDate() { return startDate; }
    }

    private static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private List<ActivityResponse> activities;

        public void setActivities(List<ActivityResponse> activities) {
            this.activities = activities;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ActivityResponse activity = activities.get(position);
            holder.nameTextView.setText(activity.getName());
            holder.distanceTextView.setText(String.format("%.2f km", activity.getDistance() / 1000));
            holder.typeTextView.setText(activity.getType());
            holder.dateTextView.setText(activity.getStartDate());
        }

        @Override
        public int getItemCount() {
            return activities != null ? activities.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView, distanceTextView, typeTextView, dateTextView;

            ViewHolder(View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                distanceTextView = itemView.findViewById(R.id.distanceTextView);
                typeTextView = itemView.findViewById(R.id.typeTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
            }
        }
    }
}