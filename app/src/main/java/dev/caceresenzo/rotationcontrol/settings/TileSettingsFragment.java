package dev.caceresenzo.rotationcontrol.settings;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;

import java.util.Set;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.settings.preference.CustomPreferenceFragmentCompat;

public class TileSettingsFragment extends CustomPreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.tile_preferences, rootKey);

        findPreference(getString(R.string.tile_buttons_key)).setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        String key = preference.getKey();

        if (getString(R.string.tile_buttons_key).equals(key)) {
            Set<String> selectedValues = (Set<String>) newValue;

            if (selectedValues.isEmpty()) {
                Toast.makeText(getContext(), R.string.buttons_at_least_one, Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }

        return true;
    }

}