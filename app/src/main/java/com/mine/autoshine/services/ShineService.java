package com.mine.autoshine.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;

import com.mine.autoshine.Constants;
import com.mine.autoshine.MySettings;
import com.mine.autoshine.R;
import com.mine.autoshine.ShineControl;

public class ShineService extends Service {

    public static boolean isRunning;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AutoShineServiceChannel";

    private MySettings settings;
    public static ShineControl shineControl;

    @Override
    public void sendBroadcast(Intent intent) {
        super.sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        settings = new MySettings(this);
        shineControl = new ShineControl(this);

        IntentFilter sys = new IntentFilter();
        sys.addAction(Intent.ACTION_SCREEN_ON);
        sys.addAction(Intent.ACTION_SCREEN_OFF);
        sys.addAction(Intent.ACTION_USER_PRESENT);
        sys.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        sys.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        IntentFilter cmd = new IntentFilter(Constants.SERVICE_INTENT_ACTION);
        registerReceiver(systemReceiver, sys, Context.RECEIVER_EXPORTED);
        registerReceiver(commandReceiver, cmd, Context.RECEIVER_NOT_EXPORTED);

        sendServiceStatus(Constants.SERVICE_STATUS_RUNNING);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification.Builder builder =
                new Notification.Builder(this, CHANNEL_ID);

        Notification notification = builder
                .setContentTitle(getString(R.string.service_state))
                .setContentText(getString(R.string.service_message))
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }

        boolean isLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        shineControl.setLandscape(isLandscape);

        if (settings.mode == Constants.WORK_MODE.ALWAYS) {
            shineControl.startListening();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;

        if (shineControl != null) {
            shineControl.stopListening();
        }

        try { unregisterReceiver(systemReceiver); } catch (Exception ignored) { }
        try { unregisterReceiver(commandReceiver); } catch (Exception ignored) { }

        sendServiceStatus(Constants.SERVICE_STATUS_STOPPED);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // This is called if the user swipes the app away from the recent tasks list.
        // We attempt to restart the service to keep the backlight control running.
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Receives system broadcasts only
    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            boolean isLandscape =
                    getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            shineControl.setLandscape(isLandscape);

            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                    shineControl.prepareForScreenOn();
                    break;
                case Intent.ACTION_USER_PRESENT:
                case Intent.ACTION_SCREEN_ON:
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    if (settings.mode == Constants.WORK_MODE.UNLOCK) {
                        shineControl.onScreenUnlock();
                    } else {
                        shineControl.startListening();
                    }
                break;
                default:
                    break;
            }
        }
    };

    // Receives app-internal commands only
    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Constants.SERVICE_INTENT_ACTION.equals(intent.getAction())) return;

            String payload = intent.getStringExtra(Constants.SERVICE_INTENT_EXTRA);

            if (Constants.SERVICE_INTENT_PAYLOAD_SET.equals(payload)) {
                shineControl.reconfigure();
            }
        }
    };

    private void createNotificationChannel() {
        // Importance LOW keeps the notification silent but helps prevent the service
        // from being killed for battery saving.
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.service_name), NotificationManager.IMPORTANCE_LOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);

            Intent tapIntent = new Intent();
            tapIntent.putExtra(Constants.SERVICE_INTENT_EXTRA_TAP, 0);
            tapIntent.setAction(Constants.SERVICE_INTENT_ACTION);
            PendingIntent tapPendingIntent = PendingIntent.getBroadcast(
                    this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

            Notification.Builder notificationBuilder = new Notification.Builder(this, CHANNEL_ID);
            Notification mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(tapPendingIntent)
                    .build();

            startForeground(123, mNotification);
        }
    }

    private void sendServiceStatus(final int status) {
        Intent i = new Intent(Constants.SERVICE_INTENT_STATUS);
        i.setPackage(getPackageName());
        i.putExtra(Constants.SERVICE_INTENT_STATUS, status);
        sendBroadcast(i);
    }
}
