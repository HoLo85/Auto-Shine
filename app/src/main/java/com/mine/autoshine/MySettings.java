package com.mine.autoshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MySettings {

    private static final String TAG = "MySettings";

    private final Context context;
    private SharedPreferences sharedPref;

    public int l1, l2, l3, l4, b1, b2, b3, b4;
    public Constants.WORK_MODE mode;

    MySettings(Context context) {
        this.context = context;
        load();
    }

    public void load() {
        sharedPref = context.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);

        l1 = sharedPref.getInt("l1", 1);
        l2 = sharedPref.getInt("l2", 100);
        l3 = sharedPref.getInt("l3", 1000);
        l4 = sharedPref.getInt("l4", 10000);

        b1 = sharedPref.getInt("b1", 15);
        b2 = sharedPref.getInt("b2", 40);
        b3 = sharedPref.getInt("b3", 65);
        b4 = sharedPref.getInt("b4", 90);

        mode = Constants.WORK_MODE.values()[sharedPref.getInt("mode", Constants.WORK_MODE.UNLOCK.ordinal())];

        if (MainActivity.debugEnabled) {
            Log.d(TAG, "Reloaded settings:");
            Log.d(TAG, String.format("Mode: %s", mode));
            Log.d(TAG, String.format("Lux: %s,%s,%s,%s", l1, l2, l3, l4));
            Log.d(TAG, String.format("Display: %s,%s,%s,%s", b1, b2, b3, b4));
        }
    }

    public void save() {
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("l1", l1);
        editor.putInt("l2", l2);
        editor.putInt("l3", l3);
        editor.putInt("l4", l4);

        editor.putInt("b1", b1);
        editor.putInt("b2", b2);
        editor.putInt("b3", b3);
        editor.putInt("b4", b4);

        editor.putInt("mode", mode.ordinal());

        editor.apply();

        if (MainActivity.debugEnabled) {
            Log.d(TAG, "Settings saved:");
//            Log.d(TAG, String.format("Mode: %s", mode));
//            Log.d(TAG, String.format("Lux: %s,%s,%s,%s", l1, l2, l3, l4));
//            Log.d(TAG, String.format("Display: %s,%s,%s,%s", b1, b2, b3, b4));
        }
    }
}