package com.mine.autoshine;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;

import com.mine.autoshine.services.ShineService;

public class ShineControl implements SensorEventListener {

    private static final String TAG = "ShineControl";

    private final ShineService shineService;
    private final SensorManager sMgr;
    private final DisplayManager dMgr;
    private final Sensor lightSensor;
    private final MySettings sett;
    private final ContentResolver cResolver;

    // Used only to schedule stopListening after "pause" in non-always modes
    private final Handler delayer = new Handler(Looper.getMainLooper());

    private boolean onListen = false;
    private boolean landscape = false;
    private boolean needsImmediateUpdate = false;
    private float rawLux = 0;
    private static final long PAUSE = 2500;

    private final Context mContext;
    private final TelephonyManager telephonyManager;

    public ShineControl(ShineService service) {
        shineService = service;
        sett = new MySettings(service.getApplicationContext());
        cResolver = service.getApplicationContext().getContentResolver();
        sMgr = (SensorManager) service.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        dMgr = (DisplayManager) service.getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
        mContext = service.getApplicationContext();
        telephonyManager = (TelephonyManager) service.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (dMgr.getDisplay(0).getState() != Display.STATE_ON ||
            event.sensor.getType() != Sensor.TYPE_LIGHT) {
            return;
        }

        rawLux = event.values[0];
        setBrightness((int) rawLux);
    }

    public void prepareForScreenOn() {
        needsImmediateUpdate = true;
        startListening();
    }

    private class Runner implements Runnable {
        public void run() {
            boolean ringing = false;
            if (mContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                int state = telephonyManager.getCallStateForSubscription();
                ringing = (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK);
            }

            if (ringing) {
                delayer.postDelayed(new Runner(), PAUSE);
            } else {
                stopListening();
            }
        }
    }

    public void startListening() {
        boolean shouldActivate = false;
        switch (sett.mode) {
            case ALWAYS:
            case UNLOCK:
                shouldActivate = true;
                break;
            case LANDSCAPE:
                shouldActivate = landscape || needsImmediateUpdate;
                break;
            case PORTRAIT:
                shouldActivate = !landscape || needsImmediateUpdate;
                break;
            default:
                break;
        }

        if (!shouldActivate) {
            stopListening();
        } else {
            delayer.removeCallbacksAndMessages(null);

            if (!onListen && lightSensor != null) {
                sMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                onListen = true;
            }

            scheduleSuspend();
        }
    }

    public void stopListening() {
        if (onListen) {
            sMgr.unregisterListener(this);
            onListen = false;
        }
        delayer.removeCallbacksAndMessages(null);
    }

    public void reconfigure() {
        stopListening();
        sett.load();
        startListening();
    }

    public void setLandscape(boolean land) {
        this.landscape = land;
    }

    public void onScreenUnlock() {
        try {
            Settings.System.putInt(
                    cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
        } catch (Exception ignored) { }

        needsImmediateUpdate = true;
        startListening();
    }

    /**
     * Get current lux value read from sensor
     * @return raw lux level
     */
    public int getRawSensorValue() { return (int) rawLux; }

    /**
     * Read current display brightnes set in system.
     * Can return wrong values if auto brightness is active!
     * @return current display brightness in percent
     */
    public int getDisplayBrightness() {
        int displayBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, -1);
        return convertFromPWM(displayBrightness);
    }

    //Post token to observers
    public String[] getLiveBrightnessData() {
        return new String[]{
                String.valueOf(getRawSensorValue()),
                String.valueOf(getDisplayBrightness())
        };
    }

    public boolean getOnListen() {
        return onListen;
    }

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE.ALWAYS) return;
        if (sett.mode == Constants.WORK_MODE.PORTRAIT && !landscape) return;
        if (sett.mode == Constants.WORK_MODE.LANDSCAPE && landscape) return;

        delayer.removeCallbacksAndMessages(null);
        delayer.postDelayed(new Runner(), PAUSE);
    }

    private void setBrightness(int lux) {
        float x1, y1, x2, y2;
        int brightness;
        if (lux <= sett.l1)
            brightness = sett.b1;
        else if (lux > sett.l4)
            brightness = sett.b4;
        else {
            x1 = sett.l3;
            x2 = sett.l4;
            y1 = sett.b3;
            y2 = sett.b4;

            if (lux <= sett.l2) {
                x1 = sett.l1;
                x2 = sett.l2;
                y1 = sett.b1;
                y2 = sett.b2;
            } else if (lux <= sett.l3) {
                x1 = sett.l2;
                x2 = sett.l3;
                y1 = sett.b2;
                y2 = sett.b3;
            }

            float div = lux - x1;
            float xlen = x2 - x1;
            float ylen = y2 - y1;
            float coef = div / xlen;
            brightness = (int) (y1 + ylen * coef);
        }

        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, convertToPWM(brightness));
        sendBroadcastToService(getLiveBrightnessData());
        if (MainActivity.debugEnabled) {
            Log.d(TAG, String.format("Updating: %s:%s", lux, brightness));
        }
    }

    /**
     * Convert brightness from percent to pwm values
     * @param userSetBrightness display brightness 0-100
     * @return brightness converted to pwm values
     */
    private int convertToPWM(int userSetBrightness) {
        if (userSetBrightness < 1) {
            userSetBrightness = 1;
        }
        if (userSetBrightness > 100) {
            userSetBrightness = 100;
        }
        return Math.round((float)255/100*userSetBrightness);
    }

    /**
     * Convert display pwm values for brightness to percent
     * @param displayBrightness pwm value 0-255
     * @return brightness in percent
     */
    private int convertFromPWM(int displayBrightness) {
        if (displayBrightness < 1) {
            displayBrightness = 1;
        }
        if (displayBrightness > 255) {
            displayBrightness = 255;
        }
        return Math.round((float)100/255*displayBrightness);
    }

    private void sendBroadcastToService(String[] sensorData) {
        Intent i = new Intent(Constants.SERVICE_INTENT_SENSOR);
        i.setPackage(shineService.getPackageName());
        i.putExtra(Constants.SERVICE_INTENT_SENSOR, sensorData);
        shineService.sendBroadcast(i);
    }
}
