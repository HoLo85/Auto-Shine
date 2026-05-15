package com.mine.autoshine;

public final class Constants {

    private Constants() {}

    public final static int SERVICE_STATUS_UNKNOWN = -1;
    public final static int SERVICE_STATUS_STOPPED = 0;
    public final static int SERVICE_STATUS_RUNNING = 1;

    public enum WORK_MODE {
        UNLOCK,
        PORTRAIT,
        LANDSCAPE,
        ALWAYS,
    }

    // Best practice: fully-qualified action & extra names (avoid collisions)
    public static final String SERVICE_INTENT_ACTION = "com.mine.autolight.ACTION_LIGHT_COMMAND";
    public static final String SERVICE_INTENT_EXTRA = "com.mine.autolight.EXTRA_COMMAND";
    public static final String SERVICE_INTENT_SENSOR = "com.mine.autolight.SENSOR_COMMAND";
    public static final String SERVICE_INTENT_STATUS = "com.mine.autolight.SERVICE_STATUS";
    public static final String SERVICE_INTENT_PAYLOAD_SET = "update_settings";
    public static final String SERVICE_INTENT_DEBUG_SET = "switch_debug";

    // Keep the user-enabled preference centralized
    public static final String PREFS_NAME = "AutoLightPrefs";
    public static final String PREF_ENABLED_KEY = "service_enabled_by_user";

    // Settings storage file for curve/mode
    public static final String SETTINGS_PREFS_NAME = "mine.autolight";
}