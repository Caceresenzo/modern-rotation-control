package dev.caceresenzo.rotationcontrol.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import java.util.Set;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.settings.preference.CustomPreferenceFragmentCompat;
import dev.caceresenzo.rotationcontrol.util.Permissions;

public class SuggestionsSettingsFragment extends CustomPreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.suggestion_preferences, rootKey);

        findPreference(getString(R.string.suggest_for_modes_key)).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        String key = preference.getKey();

        if (getString(R.string.suggest_for_modes_key).equals(key)) {
            Set<String> selectedValues = (Set<String>) newValue;

            if (selectedValues.isEmpty()) {
                Toast.makeText(getContext(), R.string.buttons_at_least_one, Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        RotationService.notifyConfigurationChanged(context);
    }

}