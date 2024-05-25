package dev.caceresenzo.rotationcontrol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum RotationMode {

    AUTO(R.id.mode_auto),
    PORTRAIT(R.id.mode_portrait),
    PORTRAIT_REVERSE(R.id.mode_portrait_reverse),
    PORTRAIT_SENSOR(R.id.mode_portrait_sensor),
    LANDSCAPE(R.id.mode_landscape),
    LANDSCAPE_REVERSE(R.id.mode_landscape_reverse),
    LANDSCAPE_SENSOR(R.id.mode_landscape_sensor);

    private final int viewId;

}