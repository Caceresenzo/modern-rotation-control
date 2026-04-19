package dev.caceresenzo.rotationcontrol.settings.preset;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum PresetRotationMode {

    DEFAULT(R.string.mode_default),
    KEEP_CURRENT(R.string.mode_keep_current),
    AUTO(RotationMode.AUTO),
    PORTRAIT(RotationMode.PORTRAIT),
    PORTRAIT_REVERSE(RotationMode.PORTRAIT_REVERSE),
    PORTRAIT_SENSOR(RotationMode.PORTRAIT_SENSOR),
    LANDSCAPE(RotationMode.LANDSCAPE),
    LANDSCAPE_REVERSE(RotationMode.LANDSCAPE_REVERSE),
    LANDSCAPE_SENSOR(RotationMode.LANDSCAPE_SENSOR);

    private final @StringRes int stringId;
    private final @Nullable RotationMode rotationMode;

    PresetRotationMode(int stringId) {
        this.stringId = stringId;
        this.rotationMode = null;
    }

    PresetRotationMode(RotationMode rotationMode) {
        this.stringId = rotationMode.stringId();
        this.rotationMode = rotationMode;
    }

    public @DrawableRes int drawableId() {
        if (rotationMode != null) {
            return rotationMode.drawableId();
        }

        return 0;
    }

    public static PresetRotationMode valueOf(String name, PresetRotationMode defaultValue) {
        if (name == null) {
            return defaultValue;
        }

        try {
            return valueOf(name);
        } catch (IllegalArgumentException __) {
            return defaultValue;
        }
    }

}