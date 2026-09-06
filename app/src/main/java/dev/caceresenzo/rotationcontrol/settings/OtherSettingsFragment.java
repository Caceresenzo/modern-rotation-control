package dev.caceresenzo.rotationcontrol.settings;

import android.os.Bundle;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.settings.preference.CustomPreferenceFragmentCompat;

public class OtherSettingsFragment extends CustomPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.other_preferences, rootKey);
    }

}