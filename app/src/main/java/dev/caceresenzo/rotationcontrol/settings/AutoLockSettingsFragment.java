package dev.caceresenzo.rotationcontrol.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;

public class AutoLockSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

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
        setPreferencesFromResource(R.xml.auto_lock_preferences, rootKey);

        updateAutoLockModeEnabledState();
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        updateAutoLockModeEnabledState();

        if (getString(R.string.auto_lock_key).equals(key)
                || getString(R.string.auto_lock_mode_key).equals(key)
                || getString(R.string.auto_lock_force_key).equals(key)) {
            RotationService.notifyConfigurationChanged(context);
        }
    }

    private void updateAutoLockModeEnabledState() {
        boolean isEnabled = !"0".equals(getPreferenceScreen().getSharedPreferences().getString(getString(R.string.auto_lock_key), "0"));
        findPreference(getString(R.string.auto_lock_mode_key)).setEnabled(isEnabled);

        boolean isModeAuto = RotationMode.AUTO.equals(RotationMode.fromPreferences(getContext(), R.string.auto_lock_mode_key, RotationMode.AUTO));
        findPreference(getString(R.string.auto_lock_force_key)).setEnabled(isEnabled && !isModeAuto);
    }
}