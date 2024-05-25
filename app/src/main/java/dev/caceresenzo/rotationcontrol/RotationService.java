package dev.caceresenzo.rotationcontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RotationService extends Service {

    public static final String TAG = RotationService.class.getSimpleName();

    public static final String CHANNEL_ID = "Controls";
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_START = "START";
    public static final String ACTION_REFRESH_NOTIFICATION = "REFRESH";

    private final IBinder binder = new LocalBinder();
    private Notification.Builder notificationBuilder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");

        createNotificationChannel();

        notificationBuilder = new Notification.Builder(getApplicationContext(), CHANNEL_ID);
    }

    @Override
    public void onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, String.format("OnStartCommand flags=%d startId=%d", flags, startId));

        String action = intent.getAction();
        if (action == null) {
            Log.i(TAG, "null action");

            return START_NOT_STICKY;
        }

        notificationBuilder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setContentTitle("Hello World")
                .setDeleteIntent(newRefreshPendingIntent());

        switch (action) {
            case ACTION_START: {
                startForeground(NOTIFICATION_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                return START_STICKY;
            }

            case ACTION_REFRESH_NOTIFICATION: {
                getNotificationManager().notify(NOTIFICATION_ID, notificationBuilder.build());
                return START_STICKY;
            }

            default: {
                Log.i(TAG, String.format("unknown action - action=%s", action));

                return START_NOT_STICKY;
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
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_START);

        context.startForegroundService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, RotationService.class);

        context.stopService(intent);
    }

    static class LocalBinder extends Binder {
    }

}