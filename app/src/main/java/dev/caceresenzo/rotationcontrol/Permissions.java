package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.provider.Settings;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Permissions {

    public static boolean canWriteSettings(Context context) {
        return Settings.System.canWrite(context);
    }

    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        String enabledServicesSetting = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServicesSetting == null) {
            return false;
        }

        String expectedServiceName = context.getPackageName() + "/" + RotationAccessibilityService.class.getName();
        return enabledServicesSetting.contains(expectedServiceName);
    }

}