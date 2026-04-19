package dev.caceresenzo.rotationcontrol.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.settings.RotationSharedPreferences;
import dev.caceresenzo.rotationcontrol.settings.preset.PresetRotationMode;
import dev.caceresenzo.rotationcontrol.settings.preset.PresetsActivity;
import dev.caceresenzo.rotationcontrol.tile.QuickActionsDialog;

public class RotationAccessibilityService extends AccessibilityService {

    public static final String APPLICATION_PACKAGE = RotationAccessibilityService.class.getPackage().getName();
    public static final String QUICK_ACTIONS_DIALOG = QuickActionsDialog.class.getName();

    private static final String TAG = RotationAccessibilityService.class.getSimpleName();

    private String previousPackageName;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log.d(TAG, String.format("event received - event=%s", event));
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        cancelNotification();

        CharSequence packageName = event.getPackageName();
        if (packageName == null) {
            return;
        }

        String currentPackageName = packageName.toString();
        if (currentPackageName.equals(previousPackageName)) {
            return;
        }

        if (currentPackageName.equals(APPLICATION_PACKAGE) && QUICK_ACTIONS_DIALOG.contentEquals(event.getClassName())) {
            return;
        }

        for (String prefix : PresetsActivity.IGNORED_PREFIXES) {
            if (currentPackageName.startsWith(prefix)) {
                return;
            }
        }

        /* ignore overlays */
        if (event.getText().isEmpty()) {
            return;
        }

        previousPackageName = currentPackageName;
        onPackageChanged(currentPackageName);
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(RotationService.PRESETS_NOTIFICATION_ID);
    }

    private void onPackageChanged(String packageName) {
        RotationSharedPreferences preferences = RotationSharedPreferences.from(this);
        PresetRotationMode mode = preferences.getApplicationMode(packageName);

        Log.d(TAG, String.format("package changed - packageName=%s mode=%s", packageName, mode));

        if (PresetRotationMode.DEFAULT.equals(mode)) {
            RotationService.notifyPresetsRestore(this);
        } else if (PresetRotationMode.KEEP_CURRENT.equals(mode)) {
            /* do nothing */
        } else {
            RotationService.notifyPresetsUpdate(this, mode.rotationMode());
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupted");
    }

}