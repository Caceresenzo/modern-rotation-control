package dev.caceresenzo.rotationcontrol.util;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Managers {

    public static ActivityManager getActivityManager(Context context) {
        return context.getApplicationContext().getSystemService(ActivityManager.class);
    }

    public static DisplayManager getDisplayManager(Context context) {
        return context.getApplicationContext().getSystemService(DisplayManager.class);
    }

    public static InputMethodManager getInputMethodManager(Context context) {
        return context.getApplicationContext().getSystemService(InputMethodManager.class);
    }

    public static NotificationManager getNotificationManager(Context context) {
        return context.getApplicationContext().getSystemService(NotificationManager.class);
    }

    public static PowerManager getPowerManager(Context context) {
        return context.getApplicationContext().getSystemService(PowerManager.class);
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static StatusBarManager getStatusBarManager(Context context) {
        return context.getApplicationContext().getSystemService(StatusBarManager.class);
    }

    public static WindowManager createUiWindowManager(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return context.getSystemService(WindowManager.class);
        }

        Display display = getDisplayManager(context)
                .getDisplay(Display.DEFAULT_DISPLAY);

        Context uiContext = context.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);

        return uiContext.getSystemService(WindowManager.class);
    }

}