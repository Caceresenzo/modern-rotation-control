package dev.caceresenzo.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class RotationAccessibilityService extends AccessibilityService {

    private static final String TAG = RotationAccessibilityService.class.getSimpleName();

    private String previousPackageName;
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent: " + event.toString());

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
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupted");
    }

}