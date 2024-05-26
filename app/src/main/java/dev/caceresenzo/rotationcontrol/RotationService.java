package dev.caceresenzo.rotationcontrol;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class RotationService extends Service {

    public static final String TAG = RotationService.class.getSimpleName();

    public static final String CHANNEL_ID = "Controls";
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_START = "START";

    public static final String ACTION_REFRESH_NOTIFICATION = "REFRESH";
    public static final int ACTION_REFRESH_NOTIFICATION_REQUEST_CODE = 10;

    public static final String ACTION_CHANGE_GUARD = "CHANGE_GUARD";
    public static final int ACTION_CHANGE_GUARD_REQUEST_CODE = 20;

    public static final String ACTION_CHANGE_MODE = "CHANGE_MODE";
    public static final int ACTION_CHANGE_MODE_REQUEST_CODE_BASE = 30;
    public static final String INTENT_NEW_MODE = "NEW_MODE";

    public static final String TINT_METHOD = "setColorFilter";

    private final IBinder binder = new LocalBinder();

    private NotificationCompat.Builder notificationBuilder;
    private boolean guard = true;
    private RotationMode mode = RotationMode.AUTO;
    private View view;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");

        createNotificationChannel();

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
    }

    @Override
    public void onDestroy() {
        if (view != null) {
            getWindowManager().removeView(view);
            view = null;
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, String.format("OnStartCommand flags=%d startId=%d", flags, startId));

        String action = intent.getAction();
        Log.i(TAG, String.format("handle action - action=%s extras=%s", action, intent.getExtras()));

        if (action == null) {
            return START_NOT_STICKY;
        }

        RemoteViews layout = new RemoteViews(getPackageName(), R.layout.notification);
        layout.setOnClickPendingIntent(R.id.guard, newGuardPendingIntent());

        for (RotationMode mode : RotationMode.values()) {
            // Log.i(TAG, String.format("attach intent - mode=%s viewId=%d", mode, mode.viewId()));
            layout.setOnClickPendingIntent(mode.viewId(), newModePendingIntent(mode));
        }

        notificationBuilder
                .setSmallIcon(R.drawable.mode_auto)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCustomContentView(layout)
                .setCustomBigContentView(layout)
                .setDeleteIntent(newRefreshPendingIntent());

        switch (action) {
            case ACTION_START: {
                updateViews(layout);
                startForeground(NOTIFICATION_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

                return START_STICKY;
            }

            case ACTION_REFRESH_NOTIFICATION: {
                break;
            }

            case ACTION_CHANGE_GUARD: {
                guard = !guard;
                Log.i(TAG, String.format("new guard=%s", guard));

                break;
            }

            case ACTION_CHANGE_MODE: {
                RotationMode newMode = RotationMode.valueOf(intent.getStringExtra(INTENT_NEW_MODE));
                Log.i(TAG, String.format("new mode=%s", newMode));

                mode = newMode;
                break;
            }

            default: {
                Log.i(TAG, String.format("unknown action - action=%s", action));
                return START_NOT_STICKY;
            }
        }

        applyMode();
        updateViews(layout);
        getNotificationManager().notify(NOTIFICATION_ID, notificationBuilder.build());

        return START_STICKY;
    }

    public boolean isGuardEnabledOrForced() {
        return guard || mode.doesRequireGuard();
    }

    private void updateViews(RemoteViews layout) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        int guardColor = isGuardEnabledOrForced() ? R.color.aqua : R.color.red;
        layout.setColor(R.id.guard, TINT_METHOD, guardColor);
        layout.setColor(mode.viewId(), TINT_METHOD, R.color.aqua);

        Set<String> enabledButtons = preferences.getStringSet(getString(R.string.buttons_key), null);
        if (enabledButtons != null) {
            for (RotationMode mode : RotationMode.values()) {
                if (enabledButtons.contains(mode.name())) {
                    continue;
                }

                layout.setViewVisibility(mode.viewId(), View.GONE);
            }
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

            layoutParams.screenOrientation = mode.orientationValue();

            if (view == null) {
                view = new View(getApplicationContext());
                getWindowManager().addView(view, layoutParams);
            } else {
                getWindowManager().updateViewLayout(view, layoutParams);
            }

            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1);
        } else if (view != null) {
            getWindowManager().removeView(view);
            view = null;

            if (mode.shouldUseAccelerometerRotation()) {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1);
            } else {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0);

                if (mode.rotationValue() != -1) {
                    Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, mode.rotationValue());
                }
            }
        }
    }

    private void createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
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
        Intent intent = new Intent(getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_CHANGE_GUARD);

        return PendingIntent.getService(
                this,
                ACTION_CHANGE_GUARD_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent newModePendingIntent(RotationMode mode) {
        Intent intent = new Intent(getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_CHANGE_MODE);
        intent.putExtra(INTENT_NEW_MODE, mode.name());

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

    public static void start(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_START);

        context.startForegroundService(intent);
    }

    public static void notifyConfigurationChanged(Context context) {
        if (!isRunning(context)) {
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_REFRESH_NOTIFICATION);

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

    static class LocalBinder extends Binder {
    }

}