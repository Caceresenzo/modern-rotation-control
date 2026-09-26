package dev.caceresenzo.rotationcontrol.util;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.annotation.StyleRes;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

import dev.caceresenzo.rotationcontrol.R;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Colors {

    private static int cachedActiveColor = -1;
    private static int cachedSaturatedActiveColor = -1;

    /* Default Material3's primary color isn't great, so forcing saturation increase. */
    public static int getActive(Context context) {
        Integer color = MaterialColors.getColorOrNull(context, androidx.appcompat.R.attr.colorPrimary);
        if (color != null) {
            if (cachedActiveColor != color) {
                cachedActiveColor = color;
                cachedSaturatedActiveColor = increaseSaturation(color, 4f /* magic number */);
            }

            return cachedSaturatedActiveColor;
        }

        return context.getColor(R.color.active);
    }

    public static int getInactive(Context context) {
        Integer color = MaterialColors.getColorOrNull(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
        if (color != null) {
            return color;
        }

        return context.getColor(R.color.inactive);
    }

    public static Context createThemedContext(Context baseContext, @StyleRes int themeResId) {
        Context wrappedContext = new ContextThemeWrapper(baseContext.getApplicationContext(), themeResId);
        return DynamicColors.wrapContextIfAvailable(wrappedContext);
    }

    private static int increaseSaturation(int color, float multiplier) {
        float[] hueSaturationLightness = new float[3];
        ColorUtils.colorToHSL(color, hueSaturationLightness);

        hueSaturationLightness[1] = Math.min(1f, hueSaturationLightness[1] * multiplier);

        return ColorUtils.HSLToColor(hueSaturationLightness);
    }

}