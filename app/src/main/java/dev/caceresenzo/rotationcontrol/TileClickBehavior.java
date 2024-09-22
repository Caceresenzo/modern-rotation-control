package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public enum TileClickBehavior {

    TOGGLE_CONTROL,
    SHOW_MODES_IF_CONTROLLING,
    ALWAYS_SHOW_MODES;

    public static TileClickBehavior fromPreferences(Context context) {
        return fromPreferences(context, TOGGLE_CONTROL);
    }

    public static TileClickBehavior fromPreferences(Context context, TileClickBehavior defaultValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String name = preferences.getString(context.getString(R.string.tile_click_behavior_key), null);

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