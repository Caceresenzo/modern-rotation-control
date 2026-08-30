package dev.caceresenzo.rotationcontrol.settings;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
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
            R.id.line_1,
            R.string.button_guard,
            R.drawable.guard
    ),

    AUTO(RotationMode.AUTO, R.id.line_1),
    PORTRAIT(RotationMode.PORTRAIT, R.id.line_2),
    PORTRAIT_REVERSE(RotationMode.PORTRAIT_REVERSE, R.id.line_2),
    PORTRAIT_SENSOR(RotationMode.PORTRAIT_SENSOR, R.id.line_2),
    LANDSCAPE(RotationMode.LANDSCAPE, R.id.line_3),
    LANDSCAPE_REVERSE(RotationMode.LANDSCAPE_REVERSE, R.id.line_3),
    LANDSCAPE_SENSOR(RotationMode.LANDSCAPE_SENSOR, R.id.line_3),

    REFRESH(
            R.id.refresh,
            R.id.line_4,
            R.string.button_refresh,
            R.drawable.refresh
    ),

    POWER(
            R.id.power,
            R.id.line_4,
            R.string.button_power,
            R.drawable.power
    );

    private final RotationMode rotationMode;
    private final @IdRes int viewId;
    private final @IdRes int lineId;
    private final @StringRes int stringId;
    private final @DrawableRes int drawableId;

    ActionButton(RotationMode mode, @IdRes int lineId) {
        this.rotationMode = mode;
        this.viewId = mode.viewId();
        this.lineId = lineId;
        this.stringId = mode.stringId();
        this.drawableId = mode.drawableId();
    }

    ActionButton(@IdRes int viewId, @IdRes int lineId, @StringRes int stringId, @DrawableRes int drawableId) {
        this.rotationMode = null;
        this.viewId = viewId;
        this.lineId = lineId;
        this.stringId = stringId;
        this.drawableId = drawableId;
    }

    public boolean isActive(RotationMode currentMode, boolean guard, boolean running) {
        if (currentMode != null && currentMode.equals(this.rotationMode)) {
            return true;
        }

        if (this == GUARD) {
            return guard;
        }

        if (this == POWER) {
            return running;
        }

        return false;
    }

    @Nullable
    public static ActionButton fromViewId(@IdRes int viewId) {
        for (ActionButton button : values()) {
            if (button.viewId == viewId) {
                return button;
            }
        }

        return null;
    }

}