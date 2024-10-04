package com.jason.memory.lab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActivityRecognitionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("ACTION_DETECTED_ACTIVITY")) {
            String type = intent.getStringExtra("type");
            // Handle the detected activity type
            if (context instanceof LabRunActivity) {
                ((LabRunActivity) context).handleDetectedActivity(type);
            }
        }
    }
}