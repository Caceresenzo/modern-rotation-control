package dev.caceresenzo.rotationcontrol.rotation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum DisplayRotation {

    PORTRAIT(RotationMode.PORTRAIT),
    PORTRAIT_REVERSE(RotationMode.PORTRAIT_REVERSE),
    LANDSCAPE(RotationMode.LANDSCAPE),
    LANDSCAPE_REVERSE(RotationMode.LANDSCAPE_REVERSE);

    private final RotationMode rotationMode;

    public DisplayRotation next() {
        switch (this) {
            case PORTRAIT: {
                return DisplayRotation.LANDSCAPE;
            }

            case PORTRAIT_REVERSE: {
                return DisplayRotation.LANDSCAPE_REVERSE;
            }

            case LANDSCAPE: {
                return DisplayRotation.PORTRAIT;
            }

            case LANDSCAPE_REVERSE: {
                return DisplayRotation.PORTRAIT_REVERSE;
            }

            default: {
                // NOTE: Should never happen, but compiler is complaining...
                throw new IllegalStateException("unknown rotation: " + this);
            }
        }
    }

    public int rotationValue() {
        return rotationMode.rotationValue();
    }

    public static DisplayRotation fromValue(int value, DisplayRotation defaultRotation) {
        for (DisplayRotation rotation : values()) {
            if (rotation.rotationMode.rotationValue() == value) {
                return rotation;
            }
        }

        return defaultRotation;
    }

}