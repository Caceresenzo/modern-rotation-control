package dev.caceresenzo.rotationcontrol;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = SettingsFragment.class.getSimpleName();

    private final ActivityResultLauncher<String> notificationPermissionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            (accepted) -> {
                if (accepted) {
                    RotationService.start(getActivity());
                    return;
                }

                getPreferenceManager()
                        .getSharedPreferences()
                        .edit()
                        .putBoolean(getString(R.string.start_control_key), false)
                        .apply();

                SwitchPreferenceCompat switchPreference = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(getString(R.string.start_control_key));
                switchPreference.setChecked(false);
            }
    );

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.i(TAG, String.format("key changed - key=%s", key));

        Activity context = getActivity();
        if (context == null) {
            return;
        }

        if (getString(R.string.start_control_key).equals(key)) {
            boolean value = sharedPreferences.getBoolean(key, false);
            boolean hasNotificationPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            if (!value) {
                RotationService.stop(context);
            } else if (hasNotificationPermission) {
                RotationService.start(context);
            } else {
                Toast.makeText(context, R.string.require_notification_permission, Toast.LENGTH_LONG).show();
                notificationPermissionActivityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else if (getString(R.string.buttons_key).equals(key) || getString(R.string.guard_key).equals(key) || getString(R.string.mode_key).equals(key)) {
            // TODO should not be called if edit comes from service itself
            RotationService.notifyConfigurationChanged(context);

            refreshGuardAndMode(sharedPreferences);
        }
    }

    private void refreshGuardAndMode(SharedPreferences sharedPreferences) {
        {
            String key = getString(R.string.guard_key);
            boolean value = sharedPreferences.getBoolean(key, false);
            ((SwitchPreferenceCompat) findPreference(key)).setChecked(value);
        }

        {
            String key = getString(R.string.mode_key);
            String value = sharedPreferences.getString(key, RotationMode.AUTO.name());
            ((ListPreference) findPreference(key)).setValue(value);
        }
    }

}