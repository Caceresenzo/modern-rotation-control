package dev.caceresenzo.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.preference.PreferenceManager;

public class RotationAccessibilityService extends AccessibilityService {

    private static final String TAG = RotationAccessibilityService.class.getSimpleName();

    private String previousPackageName;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
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

        previousPackageName = currentPackageName;
        onPackageChanged(currentPackageName);
    }

    private void onPackageChanged(String packageName) {
        Log.d(TAG, String.format("package changed - packageName=%s", packageName));

        String key = PresetsActivity.getApplicationKey(packageName);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        RotationMode mode = RotationMode.valueOf(preferences.getString(key, null), null);

        if (mode != null) {
            preferences.edit()
                    .putString(getString(R.string.mode_key), mode.name())
                    .apply();

            RotationService.notifyConfigurationChanged(this);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupted");
    }

}