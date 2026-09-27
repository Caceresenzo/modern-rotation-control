package dev.caceresenzo.rotationcontrol.receiver;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.BuildConfig;
import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.util.Notifications;

public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootBroadcastReceiver.class.getSimpleName();

    private static final String DEBUG_BOOT_COMPLETED_ACTION = BuildConfig.APPLICATION_ID + ".DEBUG_BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isValidAction(intent.getAction())) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean startOnBoot = sharedPreferences.getBoolean(context.getString(R.string.start_on_boot_key), false);

        Log.i(TAG, String.format("Received Boot, start on boot? %s", startOnBoot));

        if (!startOnBoot) {
            return;
        }

        tryToStartOrShowNotification(context);
    }

    public boolean isValidAction(String action) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return true;
        }

        if (BuildConfig.DEBUG && DEBUG_BOOT_COMPLETED_ACTION.equals(action)) {
            return true;
        }

        return false;
    }

    public static void tryToStartOrShowNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                RotationService.start(context);
            } catch (ForegroundServiceStartNotAllowedException exception) {
                showTapToStartNotification(context);
            }
        } else {
            RotationService.start(context);
        }
    }

    public static void showTapToStartNotification(Context context) {
        Notifications.createChannels(context);

        Notification notification = new NotificationCompat.Builder(context, Notifications.WARNING_CHANNEL_ID)
                .setSmallIcon(R.drawable.mode_auto)
                .setAutoCancel(true)
                .setContentTitle(context.getString(R.string.tap_to_start_notification_title))
                .setContentText(context.getString(R.string.tap_to_start_notification_text))
                .setContentIntent(RotationService.newStartPendingIntent(context))
                .build();

        NotificationManager notificationManager = Notifications.getNotificationManager(context);
        notificationManager.notify(Notifications.TAP_TO_START_NOTIFICATION_ID, notification);
    }

}