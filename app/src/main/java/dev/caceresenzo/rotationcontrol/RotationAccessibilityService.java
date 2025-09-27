package dev.caceresenzo.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.preference.PreferenceManager;

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

    private void onPackageChanged(String packageName) {
        Log.d(TAG, String.format("package changed - packageName=%s", packageName));

        String key = PresetsActivity.getApplicationKey(packageName);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        RotationMode mode = RotationMode.valueOf(preferences.getString(key, null), null);

        if (mode != null) {
            RotationService.notifyPresetsUpdate(this, mode);
        } else {
            RotationService.notifyPresetsRestore(this);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupted");
    }

}