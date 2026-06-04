package dev.caceresenzo.rotationcontrol.settings.preference;

import android.content.Context;
import android.content.res.TypedArray;

import dev.caceresenzo.rotationcontrol.R;
import lombok.experimental.UtilityClass;

@UtilityClass
class Helper {

    public static int[] getEntryIcons(Context context, TypedArray array) {
        int resourceId = array.getResourceId(R.styleable.ListPreferenceWithDescription_entryIcons, 0);
        if (resourceId == 0) {
            return null;
        }

        try (TypedArray icons = context.getResources().obtainTypedArray(resourceId)) {
            int[] entryIcons = new int[icons.length()];
            for (int index = 0; index < icons.length(); index++) {
                entryIcons[index] = icons.getResourceId(index, 0);
            }

            return entryIcons;
        }
    }

}