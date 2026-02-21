package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RotationSharedPreferences {

    private static boolean initializedKeys = false;
    private static String KEY_START_CONTROL;
    private static String KEY_PRESETS_USED;
    private static String KEY_PRESETS_NOTIFIED_ACCESSIBILITY;

    private final SharedPreferences preferences;

    public void setStartControl(boolean enabled) {
        preferences.edit().putBoolean(KEY_START_CONTROL, enabled).apply();
    }

    public boolean hasPresetsBeenUsed() {
        return preferences.getBoolean(KEY_PRESETS_USED, false);
    }

    public void markPresetsAsUsed() {
        preferences.edit()
                .putBoolean(KEY_PRESETS_USED, true)
                .putBoolean(KEY_PRESETS_NOTIFIED_ACCESSIBILITY, false)
                .apply();
    }

    public boolean hasBeenNotifiedAboutAccessibilityNotEnabledForPresets() {
        return preferences.getBoolean(KEY_PRESETS_USED, false);
    }

    public void markAccessibilityNotEnabledForPresetsAsNotified() {
        preferences.edit().putBoolean(KEY_PRESETS_NOTIFIED_ACCESSIBILITY, true).apply();
    }

    @Nullable
    public PresetRotationMode getApplicationMode(String packageName) {
        String key = getApplicationKey(packageName);
        String value = preferences.getString(key, null);

        return PresetRotationMode.valueOf(value, PresetRotationMode.DEFAULT);
    }

    public void setApplicationMode(String packageName, PresetRotationMode newMode) {
        String key = getApplicationKey(packageName);

        preferences.edit().putString(key, newMode.toString()).apply();
    }

    @NonNull
    public static String getApplicationKey(String packageName) {
        return "presets/" + packageName + "/mode";
    }

    public static RotationSharedPreferences from(Context context) {
        if (!initializedKeys) {
            initializedKeys = true;

            KEY_START_CONTROL = context.getString(R.string.start_control_key);
            KEY_PRESETS_USED = context.getString(R.string.presets_used_key);
            KEY_PRESETS_NOTIFIED_ACCESSIBILITY = context.getString(R.string.presets_notified_accessibility_key);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new RotationSharedPreferences(preferences);
    }

}