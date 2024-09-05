package com.jason.memory;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class Utility {
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