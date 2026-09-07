package dev.caceresenzo.rotationcontrol.rotation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum SuggestedRotationMode {

    AUTO(RotationMode.AUTO),
    PORTRAIT(RotationMode.PORTRAIT),
    PORTRAIT_REVERSE(RotationMode.PORTRAIT_REVERSE),
    LANDSCAPE(RotationMode.LANDSCAPE),
    LANDSCAPE_REVERSE(RotationMode.LANDSCAPE_REVERSE);

    private final RotationMode rotationMode;

    public int drawableId() {
        return rotationMode.drawableId();
    }

    public int rotationValue() {
        return rotationMode.rotationValue();
    }

    public static SuggestedRotationMode fromNullableAsAuto(DisplayRotation displayRotation) {
        if (displayRotation == null) {
            return AUTO;
        }

        return from(displayRotation);
    }

    public static SuggestedRotationMode from(DisplayRotation displayRotation) {
        switch (displayRotation) {
            case PORTRAIT: {
                return PORTRAIT;
            }

            case PORTRAIT_REVERSE: {
                return PORTRAIT_REVERSE;
            }

            case LANDSCAPE: {
                return LANDSCAPE;
            }

            case LANDSCAPE_REVERSE: {
                return LANDSCAPE_REVERSE;
            }

            default: {
                // NOTE: Should not happen
                throw new IllegalStateException("unknown rotation: " + displayRotation);
            }
        }
    }

}