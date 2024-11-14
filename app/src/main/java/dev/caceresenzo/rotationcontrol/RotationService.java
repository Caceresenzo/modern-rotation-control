package dev.caceresenzo.rotationcontrol;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

import lombok.Getter;

public class RotationService extends Service {

    public static final String TAG = RotationService.class.getSimpleName();

    public static final String CONTROLS_CHANNEL_ID = "Controls";
    public static final String SERVICE_CHANNEL_ID = "Service";
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_START = "START";
    public static final String ACTION_CONFIGURATION_CHANGED = "CONFIGURATION_CHANGED";

    public static final String ACTION_REFRESH_NOTIFICATION = "REFRESH";
    public static final int ACTION_REFRESH_NOTIFICATION_REQUEST_CODE = 10;

    public static final String ACTION_CHANGE_GUARD = "CHANGE_GUARD";
    public static final int ACTION_CHANGE_GUARD_REQUEST_CODE = 20;

    public static final String ACTION_CHANGE_MODE = "CHANGE_MODE";
    public static final int ACTION_CHANGE_MODE_REQUEST_CODE_BASE = 30;
    public static final String INTENT_NEW_MODE = "NEW_MODE";

    public static final String TINT_METHOD = "setColorFilter";

    public static final String ACTION_NOTIFY_CREATED = "dev.caceresenzo.rotationcontrol.SERVICE_CREATED";
    public static final String ACTION_NOTIFY_DESTROYED = "dev.caceresenzo.rotationcontrol.SERVICE_DESTROYED";
    public static final String ACTION_NOTIFY_UPDATED = "dev.caceresenzo.rotationcontrol.SERVICE_UPDATED";

    private final IBinder binder = new LocalBinder();

    private @Getter boolean guard = true;
    private @Getter RotationMode activeMode = RotationMode.AUTO;

    private View view;

    private UnlockBroadcastReceiver unlockBroadcastReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");

        createNotificationChannel(CONTROLS_CHANNEL_ID, R.string.controls_notification_channel_name);
        createNotificationChannel(SERVICE_CHANNEL_ID, R.string.service_notification_channel_name);
        loadFromPreferences();

        unlockBroadcastReceiver = new UnlockBroadcastReceiver();
        registerReceiver(unlockBroadcastReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));

        sendBroadcast(new Intent(ACTION_NOTIFY_CREATED));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.i(TAG, String.format("onConfigurationChanged - newConfig=%s", newConfig));

        notifyConfigurationChanged(this);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        if (view != null) {
            getWindowManager().removeView(view);
            view = null;
        }

        if (unlockBroadcastReceiver != null) {
            unregisterReceiver(unlockBroadcastReceiver);
            unlockBroadcastReceiver = null;
        }

        sendBroadcast(new Intent(ACTION_NOTIFY_DESTROYED));

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(getString(R.string.start_control_key), false)
                .apply();

        getNotificationManager().cancel(NOTIFICATION_ID);

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.i(TAG, String.format("onStartCommand - intent=null flags=%d startId=%d", flags, startId));
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        Log.i(TAG, String.format("onStartCommand - action=%s extras=%s flags=%d startId=%d", action, intent.getExtras(), flags, startId));

        if (action == null) {
            return START_NOT_STICKY;
        }

        boolean showNotification = isNotificationShown();

        switch (action) {
            case ACTION_START: {
                Notification notification = createNotification(showNotification);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }

                break;
            }

            case ACTION_CONFIGURATION_CHANGED: {
                loadFromPreferences();
                break;
            }

            case ACTION_REFRESH_NOTIFICATION: {
                break;
            }

            case ACTION_CHANGE_GUARD: {
                guard = !guard;
                Log.i(TAG, String.format("new guard=%s", guard));

                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean(getString(R.string.guard_key), guard)
                        .apply();

                break;
            }

            case ACTION_CHANGE_MODE: {
                RotationMode newMode = RotationMode.valueOf(intent.getStringExtra(INTENT_NEW_MODE));
                Log.i(TAG, String.format("new mode=%s", newMode));

                activeMode = newMode;

                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString(getString(R.string.mode_key), activeMode.name())
                        .apply();

                break;
            }

            default: {
                Log.i(TAG, String.format("unknown action - action=%s", action));
                return START_NOT_STICKY;
            }
        }

        applyMode();

        if (showNotification) {
            getNotificationManager().notify(NOTIFICATION_ID, createNotification(true));
        } else {
            getNotificationManager().cancel(NOTIFICATION_ID);
        }

        sendBroadcast(new Intent(ACTION_NOTIFY_UPDATED));

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(getString(R.string.start_control_key), true)
                .apply();

        return START_STICKY;
    }

    private Notification createNotification(boolean showNotification) {
        String channelId = showNotification
                ? CONTROLS_CHANNEL_ID
                : SERVICE_CHANNEL_ID;

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.mode_auto)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET);

        if (showNotification) {
            RemoteViews layout = new RemoteViews(getPackageName(), R.layout.notification);
            layout.setOnClickPendingIntent(R.id.guard, newGuardPendingIntent());

            for (RotationMode mode : RotationMode.values()) {
                // Log.i(TAG, String.format("attach intent - mode=%s viewId=%d", mode, mode.viewId()));
                layout.setOnClickPendingIntent(mode.viewId(), newModePendingIntent(mode));
            }

            notificationBuilder
                    .setCustomContentView(layout)
                    .setCustomBigContentView(layout)
                    .setDeleteIntent(newRefreshPendingIntent());

            notificationBuilder
                    .setSubText(null);

            updateViews(layout);
        } else {
            notificationBuilder
                    .setSubText(getString(R.string.notification_discard_me_title));

            notificationBuilder
                    .setCustomContentView(null)
                    .setCustomBigContentView(null)
                    .setDeleteIntent(null);
        }

        Log.i(TAG, String.format("prepared notification - showNotification=%s", showNotification));

        return notificationBuilder.build();
    }

    private boolean isNotificationShown() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.show_notification_key), true);
    }

    private void loadFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        guard = preferences.getBoolean(getString(R.string.guard_key), true);
        activeMode = RotationMode.fromPreferences(this);
    }

    public boolean isGuardEnabledOrForced() {
        return guard || activeMode.doesRequireGuard();
    }

    private void updateViews(RemoteViews layout) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> enabledButtons = preferences.getStringSet(getString(R.string.buttons_key), null);
        for (RotationMode mode : RotationMode.values()) {
            if (enabledButtons != null && !enabledButtons.contains(mode.name())) {
                layout.setViewVisibility(mode.viewId(), View.GONE);
            }

            layout.setInt(mode.viewId(), TINT_METHOD, getColor(R.color.inactive));
        }

        layout.setInt(activeMode.viewId(), TINT_METHOD, getColor(R.color.active));

        if (isGuardEnabledOrForced()) {
            layout.setInt(R.id.guard, TINT_METHOD, getColor(R.color.active));
        } else {
            layout.setInt(R.id.guard, TINT_METHOD, getColor(R.color.inactive));
        }
    }

    private void applyMode() {
        ContentResolver contentResolver = getContentResolver();

        if (isGuardEnabledOrForced()) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    0,
                    0,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    Gravity.TOP
            );
            layoutParams.screenOrientation = activeMode.orientationValue();

            if (view == null) {
                view = new View(getApplicationContext());
                getWindowManager().addView(view, layoutParams);
            } else {
                getWindowManager().updateViewLayout(view, layoutParams);
            }

            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1);
        } else {
            if (view != null) {
                getWindowManager().removeView(view);
                view = null;
            }

            if (activeMode.shouldUseAccelerometerRotation()) {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1);
            } else {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0);
                Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, activeMode.rotationValue());
            }
        }
    }

    private void createNotificationChannel(String id, @StringRes int name) {
        NotificationChannel notificationChannel = new NotificationChannel(id, getString(name), NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setSound(null, null);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableVibration(false);
        notificationChannel.enableLights(false);
        notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_SECRET);

        getNotificationManager().createNotificationChannel(notificationChannel);
    }

    private PendingIntent newRefreshPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_REFRESH_NOTIFICATION);

        return PendingIntent.getService(
                this,
                ACTION_REFRESH_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent newGuardPendingIntent() {
        Intent intent = newToggleGuardIntent(this);

        return PendingIntent.getService(
                this,
                ACTION_CHANGE_GUARD_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent newModePendingIntent(RotationMode mode) {
        Intent intent = newChangeModeIntent(this, mode);

        return PendingIntent.getService(
                this,
                ACTION_CHANGE_MODE_REQUEST_CODE_BASE + mode.ordinal(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }

    private WindowManager getWindowManager() {
        return (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
    }

    public static Intent newToggleGuardIntent(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_CHANGE_GUARD);

        return intent;
    }

    public static Intent newChangeModeIntent(Context context, RotationMode mode) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_CHANGE_MODE);
        intent.putExtra(INTENT_NEW_MODE, mode.name());

        return intent;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_START);

        context.startForegroundService(intent);
    }

    public static void notifyConfigurationChanged(Context context) {
        notifyConfigurationChanged(context, false);
    }

    public static void notifyConfigurationChanged(Context context, boolean forceStart) {
        if (!forceStart && !isRunning(context)) {
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_CONFIGURATION_CHANGED);

        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, RotationService.class);

        context.stopService(intent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RotationService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public class LocalBinder extends Binder {

        public RotationService getService() {
            return RotationService.this;
        }

    }

}