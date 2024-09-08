package dev.caceresenzo.rotationcontrol;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    public static final String TAG = SettingsFragment.class.getSimpleName();

    private ActivityResultLauncher<String> mNotificationPermissionActivityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        mNotificationPermissionActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                (accepted) -> {
                    if (accepted) {
                        RotationService.start(getActivity());
                    }

                    setChecked(getString(R.string.start_control_key), accepted);
                }
        );
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        findPreference(getString(R.string.start_control_key)).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    public void setChecked(String key, boolean value) {
        getPreferenceManager()
                .getSharedPreferences()
                .edit()
                .putBoolean(key, value)
                .apply();

        SwitchPreferenceCompat switchPreference = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(key);
        switchPreference.setChecked(value);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        String key = preference.getKey();
        Log.i(TAG, String.format("onPreferenceChange - key=%s newValue=%s", key, newValue));

        Context context = getContext();
        if (context == null) {
            return false;
        }

        if (getString(R.string.start_control_key).equals(key)) {
            boolean value = (boolean) newValue;

            if (!value) {
                RotationService.stop(context);
                return true;
            }

            if (hasNotificationPermission(context)) {
                RotationService.start(context);
                return true;
            }

            Toast.makeText(context, R.string.require_notification_permission, Toast.LENGTH_LONG).show();
            mNotificationPermissionActivityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return false;
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.i(TAG, String.format("onSharedPreferenceChanged - key=%s", key));

        Context context = getContext();
        if (context == null) {
            return;
        }

        if (getString(R.string.buttons_key).equals(key) || getString(R.string.guard_key).equals(key) || getString(R.string.mode_key).equals(key) || getString(R.string.show_notification_key).equals(key)) {
            // TODO should not be called if edit comes from service itself
            RotationService.notifyConfigurationChanged(context);
        }

        refresh(sharedPreferences);
    }

    private void refresh(SharedPreferences sharedPreferences) {
        {
            String key = getString(R.string.start_control_key);
            boolean value = sharedPreferences.getBoolean(key, false);
            ((SwitchPreferenceCompat) findPreference(key)).setChecked(value);
        }

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

    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

}