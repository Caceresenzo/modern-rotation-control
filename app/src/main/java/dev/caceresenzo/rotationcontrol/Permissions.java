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

}