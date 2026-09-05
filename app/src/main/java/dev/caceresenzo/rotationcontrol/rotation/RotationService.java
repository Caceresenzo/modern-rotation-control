package dev.caceresenzo.rotationcontrol.rotation;

import android.app.ActivityManager;
import android.app.Notification;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.subsystem.AutoLockSystem;
import dev.caceresenzo.rotationcontrol.rotation.subsystem.ExternalEventSystem;
import dev.caceresenzo.rotationcontrol.rotation.subsystem.RefreshSystem;
import dev.caceresenzo.rotationcontrol.rotation.subsystem.SuggestionSystem;
import dev.caceresenzo.rotationcontrol.settings.ActionButton;
import dev.caceresenzo.rotationcontrol.settings.RotationSharedPreferences;
import dev.caceresenzo.rotationcontrol.util.Permissions;
import lombok.Getter;

public class RotationService extends Service {

    public static final String TAG = RotationService.class.getSimpleName();

    public static final String CONTROLS_CHANNEL_ID = "Controls";
    public static final String SERVICE_CHANNEL_ID = "Service";
    public static final String WARNING_CHANNEL_ID = "Warning";
    public static final int NOTIFICATION_ID = 1;
    public static final int PRESETS_NOTIFICATION_ID = 2;

    public static final String ACTION_START = "START";
    public static final String ACTION_CONFIGURATION_CHANGED = "CONFIGURATION_CHANGED";
    public static final String ACTION_ORIENTATION_CHANGED = "ORIENTATION_CHANGED";
    public static final String ACTION_PRESETS_UPDATE = "PRESETS_UPDATE";
    public static final String ACTION_PRESETS_RESTORE = "PRESETS_RESTORE";
    public static final String ACTION_KEYBOARD_APPEARED = "KEYBOARD_APPEARED";

    public static final String ACTION_REFRESH_NOTIFICATION = "REFRESH_NOTIFICATION";
    public static final int ACTION_REFRESH_NOTIFICATION_REQUEST_CODE = 10;

    public static final String ACTION_CHANGE_GUARD = "CHANGE_GUARD";
    public static final int ACTION_CHANGE_GUARD_REQUEST_CODE = 20;

    public static final String ACTION_CHANGE_MODE = "CHANGE_MODE";
    public static final int ACTION_CHANGE_MODE_REQUEST_CODE_BASE = 30;
    public static final String INTENT_NEW_MODE = "NEW_MODE";

    public static final String ACTION_REFRESH = "REFRESH";
    public static final int ACTION_REFRESH_REQUEST_CODE = 40;

    public static final String ACTION_STOP_IF_STARTED_OR_START_IF_STOPPED = "STOP_IF_STARTED_OR_START_IF_STOPPED";
    public static final int ACTION_STOP_IF_STARTED_OR_START_IF_STOPPED_REQUEST_CODE = 50;

    public static final String TINT_METHOD = "setColorFilter";

    public static final String ACTION_NOTIFY_CREATED = "dev.caceresenzo.rotationcontrol.SERVICE_CREATED";
    public static final String ACTION_NOTIFY_DESTROYED = "dev.caceresenzo.rotationcontrol.SERVICE_DESTROYED";
    public static final String ACTION_NOTIFY_UPDATED = "dev.caceresenzo.rotationcontrol.SERVICE_UPDATED";

    private final IBinder binder = new LocalBinder();

    private boolean started;
    @Getter
    private boolean running;
    private @Getter boolean guard = true;
    private @Getter RotationMode activeMode = RotationMode.AUTO;
    private @Getter RotationMode previousActiveMode = null;

    private View mView;

    private @Getter Handler handler;

    private final ExternalEventSystem mExternalEventSystem = new ExternalEventSystem(this);
    private final RefreshSystem mRefreshSystem = new RefreshSystem(this);
    private final AutoLockSystem mAutoLockSystem = new AutoLockSystem(this);
    private final SuggestionSystem mSuggestionSystem = new SuggestionSystem(this);

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
        createNotificationChannel(WARNING_CHANNEL_ID, R.string.warning_notification_channel_name);
        loadFromPreferences();

        handler = new Handler(Looper.getMainLooper());

        mExternalEventSystem.onCreate();
        mAutoLockSystem.onCreate();
        mSuggestionSystem.onCreate();

        sendBroadcast(new Intent(ACTION_NOTIFY_CREATED));
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        if (mView != null) {
            getWindowManager().removeView(mView);
            mView = null;
        }

        mExternalEventSystem.onDestroy();
        mAutoLockSystem.onDestroy();
        mSuggestionSystem.onDestroy();

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

        if (!started) {
            Notification notification = createNotification(isNotificationShown());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

            started = true;
        }

        switch (action) {
            case ACTION_START: {
                break;
            }

            case ACTION_CONFIGURATION_CHANGED: {
                loadFromPreferences();
                mAutoLockSystem.reset();

                break;
            }

            case ACTION_ORIENTATION_CHANGED: {
                mAutoLockSystem.resetIfRotationChanged();

                break;
            }

            case ACTION_PRESETS_UPDATE: {
                RotationMode newMode = RotationMode.valueOf(intent.getStringExtra(INTENT_NEW_MODE));

                previousActiveMode = activeMode;
                activeMode = newMode;

                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString(getString(R.string.mode_key), newMode.toString())
                        .apply();

                break;
            }

            case ACTION_PRESETS_RESTORE: {
                if (previousActiveMode != null) {
                    activeMode = previousActiveMode;
                    previousActiveMode = null;

                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putString(getString(R.string.mode_key), activeMode.name())
                            .apply();
                }

                break;
            }

            case ACTION_KEYBOARD_APPEARED: {
                mSuggestionSystem.hideSuggestion();
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
                previousActiveMode = null;

                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString(getString(R.string.mode_key), activeMode.name())
                        .apply();

                mRefreshSystem.cancel();

                break;
            }

            case ACTION_REFRESH: {
                previousActiveMode = activeMode;
                activeMode = getNextRotation();

                applyMode();

                mRefreshSystem.schedule();

                break;
            }

            case ACTION_STOP_IF_STARTED_OR_START_IF_STOPPED: {
                if (running) {
                    // TODO Service is not killed right because of the TileService
                    Toast.makeText(this, R.string.stopping_service_soon, Toast.LENGTH_LONG).show();
                    stopSelf();
                }

                break;
            }

            default: {
                Log.i(TAG, String.format("unknown action - action=%s", action));
                return START_NOT_STICKY;
            }
        }

        running = true;
        afterStartCommand();

        return START_STICKY;
    }

    private void afterStartCommand() {
        Log.i(TAG, String.format("afterStartCommand - guard=%s mode=%s", guard, activeMode));
        applyMode();

        NotificationManager notificationManager = getNotificationManager();
        if (isNotificationShown()) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(true));
        } else {
            notificationManager.cancel(NOTIFICATION_ID);
        }

        sendBroadcast(new Intent(ACTION_NOTIFY_UPDATED));

        RotationSharedPreferences preferences = RotationSharedPreferences.from(this);
        preferences.setStartControl(true);

        if (preferences.hasPresetsBeenUsed() && !preferences.hasBeenNotifiedAboutAccessibilityNotEnabledForPresets() && !Permissions.isAccessibilityServiceEnabled(this)) {
            notificationManager.notify(PRESETS_NOTIFICATION_ID, createPresetsNotification());
            preferences.markAccessibilityNotEnabledForPresetsAsNotified();
        }
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
            layout.setOnClickPendingIntent(R.id.refresh, newRefreshModePendingIntent());
            layout.setOnClickPendingIntent(R.id.power, newStartIfStoppedOrStopIfStartedPendingIntent());

            for (RotationMode mode : RotationMode.values()) {
                // Log.i(TAG, String.format("attach intent - mode=%s viewId=%d", mode, mode.viewId()));
                layout.setOnClickPendingIntent(mode.viewId(), newModePendingIntent(mode));
            }

            notificationBuilder
                    .setCustomContentView(layout)
                    .setCustomBigContentView(layout)
                    .setDeleteIntent(newRefreshNotificationPendingIntent());

            notificationBuilder
                    .setSubText(null);

            updateViews(layout);
        } else {
            notificationBuilder
                    .setSubText(getString(R.string.notification_discard_me_title))
                    .setContentText(getString(R.string.notification_discard_me_subtitle));

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

    private Notification createPresetsNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                PRESETS_NOTIFICATION_ID /* should use a dedicated code */,
                Permissions.newOpenAccessibilityServiceSettingsIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), WARNING_CHANNEL_ID)
                .setSmallIcon(R.drawable.mode_auto)
                .setSilent(true)
                .setSubText(getString(R.string.notification_accessibility_not_enabled_title))
                .setContentText(getString(R.string.notification_accessibility_not_enabled_subtitle))
                .setContentIntent(pendingIntent);

        Log.i(TAG, "prepared presets notification");

        return notificationBuilder.build();
    }

    private void loadFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        guard = preferences.getBoolean(getString(R.string.guard_key), true);
        activeMode = RotationMode.fromPreferences(this);

        mAutoLockSystem.load(preferences);
    }

    public boolean isGuardEnabledOrForced() {
        return (guard || activeMode.doesRequireGuard()) && !isCurrentlyRefreshing();
    }

    public boolean isUsingPresets() {
        return previousActiveMode != null;
    }

    public boolean isCurrentlyRefreshing() {
        return mRefreshSystem.isCurrentlyRefreshing();
    }

    private void updateViews(RemoteViews layout) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean guard = isGuardEnabledOrForced();

        Set<String> enabledButtons = preferences.getStringSet(getString(R.string.notification_buttons_key), null);
        for (ActionButton button : ActionButton.values()) {
            if (enabledButtons != null && !enabledButtons.contains(button.name())) {
                layout.setViewVisibility(button.viewId(), View.GONE);
            }

            setActiveColor(layout, button.viewId(), button.isActive(activeMode, guard, running));
        }
    }

    private void setActiveColor(RemoteViews layout, int viewId, boolean active) {
        if (active) {
            layout.setInt(viewId, TINT_METHOD, getColor(R.color.active));
        } else {
            layout.setInt(viewId, TINT_METHOD, getColor(R.color.inactive));
        }
    }

    public void restorePreviousMode() {
        if (previousActiveMode != null) {
            activeMode = previousActiveMode;
            previousActiveMode = null;

            applyMode();
        }
    }

    private void applyMode() {
        ContentResolver contentResolver = getContentResolver();

        if (isGuardEnabledOrForced()) {
            if (!Permissions.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.missing_overlay_permission, Toast.LENGTH_SHORT).show();
                return;
            }

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    0,
                    0,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    Gravity.TOP
            );
            layoutParams.screenOrientation = activeMode.orientationValue();

            if (mView == null) {
                mView = new View(getApplicationContext());
                getWindowManager().addView(mView, layoutParams);
            } else {
                getWindowManager().updateViewLayout(mView, layoutParams);
            }

            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1);
        } else {
            if (!Permissions.canWriteSettings(this)) {
                Toast.makeText(this, R.string.missing_settings_write_permission, Toast.LENGTH_SHORT).show();
                return;
            }

            if (mView != null) {
                getWindowManager().removeView(mView);
                mView = null;
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

    private PendingIntent newRefreshNotificationPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_REFRESH_NOTIFICATION);

        return PendingIntent.getService(
                this,
                ACTION_REFRESH_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent newRefreshModePendingIntent() {
        Intent intent = newRefreshModeIntent(this);

        return PendingIntent.getService(
                this,
                ACTION_REFRESH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent newStartIfStoppedOrStopIfStartedPendingIntent() {
        Intent intent = newStartIfStoppedOrStopIfStartedIntent(this);

        return PendingIntent.getService(
                this,
                ACTION_STOP_IF_STARTED_OR_START_IF_STOPPED_REQUEST_CODE,
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

    private RotationMode getNextRotation() {
        RotationMode rotationMode = RotationMode.fromRotationValue(getCurrentDisplayRotation());

        switch (rotationMode) {
            case PORTRAIT: {
                return RotationMode.LANDSCAPE;
            }

            case PORTRAIT_REVERSE: {
                return RotationMode.LANDSCAPE_REVERSE;
            }

            case LANDSCAPE: {
                return RotationMode.PORTRAIT;
            }

            case LANDSCAPE_REVERSE: {
                return RotationMode.PORTRAIT_REVERSE;
            }

            default: {
                throw new IllegalArgumentException("no concrete next rotation for mode: " + rotationMode);
            }
        }
    }

    public int getCurrentDisplayRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
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

    public static Intent newRefreshModeIntent(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_REFRESH);

        return intent;
    }

    public static Intent newStartIfStoppedOrStopIfStartedIntent(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_STOP_IF_STARTED_OR_START_IF_STOPPED);

        return intent;
    }

    public NotificationManager getNotificationManager() {
        return getApplicationContext().getSystemService(NotificationManager.class);
    }

    public WindowManager getWindowManager() {
        return getApplicationContext().getSystemService(WindowManager.class);
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

        context.startForegroundService(intent);
    }

    public static void notifyOrientationChanged(Context context) {
        if (!isRunning(context)) {
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_ORIENTATION_CHANGED);

        context.startForegroundService(intent);
    }

    public static void notifyPresetsUpdate(Context context, RotationMode newMode) {
        if (!isRunning(context)) {
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_PRESETS_UPDATE);
        intent.putExtra(INTENT_NEW_MODE, newMode.name());

        context.startForegroundService(intent);
    }

    public static void notifyPresetsRestore(Context context) {
        if (!isRunning(context)) {
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_PRESETS_RESTORE);

        context.startForegroundService(intent);
    }

    public static void notifyKeyboardAppeared(Context context) {
        if (!isRunning(context)) {
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), RotationService.class);
        intent.setAction(ACTION_KEYBOARD_APPEARED);

        context.startForegroundService(intent);
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