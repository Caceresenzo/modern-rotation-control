package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.view.Surface;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum RotationMode {

    AUTO(
            R.id.mode_auto,
            R.string.mode_auto,
            R.drawable.mode_auto,
            -1,
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    ),

    PORTRAIT(
            R.id.mode_portrait,
            R.string.mode_portrait,
            R.drawable.mode_portrait,
            Surface.ROTATION_0,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    ),

    PORTRAIT_REVERSE(
            R.id.mode_portrait_reverse,
            R.string.mode_portrait_reverse,
            R.drawable.mode_portrait_reverse,
            Surface.ROTATION_180,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
    ),

    PORTRAIT_SENSOR(
            R.id.mode_portrait_sensor,
            R.string.mode_portrait_sensor,
            R.drawable.mode_portrait_sensor,
            -1,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    ),

    LANDSCAPE(
            R.id.mode_landscape,
            R.string.mode_landscape,
            R.drawable.mode_landscape,
            Surface.ROTATION_90,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    ),

    LANDSCAPE_REVERSE(
            R.id.mode_landscape_reverse,
            R.string.mode_landscape_reverse,
            R.drawable.mode_landscape_reverse,
            Surface.ROTATION_270,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    ),

    LANDSCAPE_SENSOR(
            R.id.mode_landscape_sensor,
            R.string.mode_landscape_sensor,
            R.drawable.mode_landscape_sensor,
            -1,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    );

    private final int viewId;
    private final @StringRes int stringId;
    private final @DrawableRes int drawableId;
    private final int rotationValue;
    private final int orientationValue;

    public boolean shouldUseAccelerometerRotation() {
        return this == AUTO;
    }

    public boolean doesRequireGuard() {
        return this != AUTO && rotationValue == -1;
    }

    @Nullable
    public static RotationMode fromViewId(int viewId) {
        for (RotationMode mode : values()) {
            if (mode.viewId == viewId) {
                return mode;
            }
        }

        return null;
    }

    public static RotationMode fromPreferences(Context context) {
        return fromPreferences(context, AUTO);
    }

    public static RotationMode fromPreferences(Context context, RotationMode defaultValue) {
        return fromPreferences(context, R.string.mode_key, defaultValue);
    }

    public static RotationMode fromPreferences(Context context, @StringRes int preferenceKeyId, RotationMode defaultValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String name = preferences.getString(context.getString(preferenceKeyId), null);

        if (name == null) {
            return defaultValue;
        }

        try {
            return valueOf(name);
        } catch (IllegalArgumentException __) {
            return defaultValue;
        }
    }

    public static RotationMode fromRotationValue(int rotationValue) {
        for (RotationMode mode : values()) {
            if (mode.rotationValue == rotationValue) {
                return mode;
            }
        }

        return PORTRAIT;
    }

}