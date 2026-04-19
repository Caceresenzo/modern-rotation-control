package dev.caceresenzo.rotationcontrol.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import dev.caceresenzo.rotationcontrol.accessibility.RotationAccessibilityService;
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

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    public static Intent newOpenAccessibilityServiceSettingsIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

}