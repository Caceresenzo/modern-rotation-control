package dev.caceresenzo.rotationcontrol.settings;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum ActionButton {

    GUARD(
            R.id.guard,
            R.string.button_guard,
            R.drawable.guard
    ),

    AUTO(RotationMode.AUTO),
    PORTRAIT(RotationMode.PORTRAIT),
    PORTRAIT_REVERSE(RotationMode.PORTRAIT_REVERSE),
    PORTRAIT_SENSOR(RotationMode.PORTRAIT_SENSOR),
    LANDSCAPE(RotationMode.LANDSCAPE),
    LANDSCAPE_REVERSE(RotationMode.LANDSCAPE_REVERSE),
    LANDSCAPE_SENSOR(RotationMode.LANDSCAPE_SENSOR),

    REFRESH(
            R.id.refresh,
            R.string.button_refresh,
            R.drawable.refresh
    );

    private final RotationMode rotationMode;
    private final int viewId;
    private final @StringRes int stringId;
    private final @DrawableRes int drawableId;

    ActionButton(RotationMode mode) {
        this.rotationMode = mode;
        this.viewId = mode.viewId();
        this.stringId = mode.stringId();
        this.drawableId = mode.drawableId();
    }

    ActionButton(int viewId, @StringRes int stringId, @DrawableRes int drawableId) {
        this.rotationMode = null;
        this.viewId = viewId;
        this.stringId = stringId;
        this.drawableId = drawableId;
    }

    public boolean isActive(RotationMode currentMode, boolean guard) {
        if (currentMode != null && currentMode.equals(this.rotationMode)) {
            return true;
        }

        if (this == GUARD) {
            return guard;
        }

        return false;
    }

    @Nullable
    public static ActionButton fromViewId(int viewId) {
        for (ActionButton button : values()) {
            if (button.viewId == viewId) {
                return button;
            }
        }

        return null;
    }

}