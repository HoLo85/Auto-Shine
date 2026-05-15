package com.mine.autoshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {

    private static final String TAG = "AutoStartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(Constants.PREF_ENABLED_KEY, true);

        if (!isEnabled) {
            Log.d(TAG, "Boot detected, but service is disabled by user. Doing nothing.");
            return;
        }

        Log.d(TAG, "Boot completed detected and service is enabled. Starting LightService...");
        Intent serviceIntent = new Intent(context, ShineService.class);

        try {
            context.startForegroundService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service on boot: " + e.getMessage());
        }
    }
}