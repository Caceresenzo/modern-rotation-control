package dev.caceresenzo.rotationcontrol.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import dev.caceresenzo.rotationcontrol.R;

public class Notifications {

    public static final String CONTROLS_CHANNEL_ID = "Controls";
    public static final String SERVICE_CHANNEL_ID = "Service";
    public static final String WARNING_CHANNEL_ID = "Warning";

    public static final int SERVICE_NOTIFICATION_ID = 1;
    public static final int PRESETS_NOTIFICATION_ID = 2;
    public static final int TAP_TO_START_NOTIFICATION_ID = 3;
    public static final int PERMISSION_SETTINGS_WRITE_NOTIFICATION_ID = 4;
    public static final int PERMISSION_DRAW_OVERLAYS_NOTIFICATION_ID = 5;

    private static boolean channelsCreated = false;

    public static void createChannels(Context context) {
        if (channelsCreated) {
            return;
        }

        createChannel(context, CONTROLS_CHANNEL_ID, R.string.controls_notification_channel_name);
        createChannel(context, SERVICE_CHANNEL_ID, R.string.service_notification_channel_name);
        createChannel(context, WARNING_CHANNEL_ID, R.string.warning_notification_channel_name);

        channelsCreated = true;
    }

    public static void createChannel(Context context, String id, @StringRes int name) {
        NotificationChannel notificationChannel = new NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setSound(null, null);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableVibration(false);
        notificationChannel.enableLights(false);
        notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_SECRET);

        getNotificationManager(context).createNotificationChannel(notificationChannel);
    }

    public static NotificationManager getNotificationManager(Context context) {
        return context.getApplicationContext().getSystemService(NotificationManager.class);
    }

}