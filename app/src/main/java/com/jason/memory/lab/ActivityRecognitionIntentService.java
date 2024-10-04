package com.jason.memory.lab;

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getMostProbableActivity());
        }
    }

    private void handleDetectedActivities(DetectedActivity detectedActivity) {
        int activityType = detectedActivity.getType();
        String activityName;

        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                activityName = "In Vehicle";
                break;
            case DetectedActivity.ON_BICYCLE:
                activityName = "On Bicycle";
                break;
            case DetectedActivity.ON_FOOT:
                activityName = "On Foot";
                break;
            case DetectedActivity.RUNNING:
                activityName = "Running";
                break;
            case DetectedActivity.STILL:
                activityName = "Still";
                break;
            case DetectedActivity.TILTING:
                activityName = "Tilting";
                break;
            case DetectedActivity.WALKING:
                activityName = "Walking";
                break;
            default:
                activityName = "Unknown";
        }

        // You can send a broadcast or use any other method to communicate this information
        // to your LabRunActivity
        Intent broadcastIntent = new Intent("ACTION_DETECTED_ACTIVITY");
        broadcastIntent.putExtra("type", activityName);
        sendBroadcast(broadcastIntent);
    }
}