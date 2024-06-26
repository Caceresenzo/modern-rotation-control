package dev.caceresenzo.rotationcontrol;

import android.content.pm.ActivityInfo;
import android.view.Surface;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum RotationMode {

    AUTO(R.id.mode_auto, -1, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR),
    PORTRAIT(R.id.mode_portrait, Surface.ROTATION_0, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    PORTRAIT_REVERSE(R.id.mode_portrait_reverse, Surface.ROTATION_180, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT),
    PORTRAIT_SENSOR(R.id.mode_portrait_sensor, -1, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT),
    LANDSCAPE(R.id.mode_landscape, Surface.ROTATION_90, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    LANDSCAPE_REVERSE(R.id.mode_landscape_reverse, Surface.ROTATION_270, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE),
    LANDSCAPE_SENSOR(R.id.mode_landscape_sensor, -1, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

    private final int viewId;
    private final int rotationValue;
    private final int orientationValue;

    public boolean shouldUseAccelerometerRotation() {
        return this == AUTO;
    }

    public boolean doesRequireGuard() {
        return this != AUTO && rotationValue == -1;
    }

}