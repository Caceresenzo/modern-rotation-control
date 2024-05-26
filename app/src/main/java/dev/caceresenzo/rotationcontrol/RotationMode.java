package dev.caceresenzo.rotationcontrol;

import android.view.Surface;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum RotationMode {

    AUTO(R.id.mode_auto, -1),
    PORTRAIT(R.id.mode_portrait, Surface.ROTATION_0),
    PORTRAIT_REVERSE(R.id.mode_portrait_reverse, Surface.ROTATION_180),
    PORTRAIT_SENSOR(R.id.mode_portrait_sensor, -1),
    LANDSCAPE(R.id.mode_landscape, Surface.ROTATION_90),
    LANDSCAPE_REVERSE(R.id.mode_landscape_reverse, Surface.ROTATION_270),
    LANDSCAPE_SENSOR(R.id.mode_landscape_sensor, -1);

    private final int viewId;
    private final int rotationValue;

    public boolean shouldUseAccelerometerRotation() {
        return this == AUTO;
    }

}