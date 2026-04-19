package dev.caceresenzo.rotationcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private ActivityResultLauncher<String> mNotificationPermissionActivityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        findPreference(getString(R.string.configure_presets_key)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.install_tile_key)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.battery_optimization_key)).setOnPreferenceClickListener(this);

        updateSuggestionsVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSuggestionsVisibility();
    }

    private void updateSuggestionsVisibility() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        boolean tileVisible = false;
        {
            String key = getString(R.string.install_tile_key);
            Preference preference = Objects.requireNonNull(findPreference(key));

            boolean isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
            boolean isInstalled = getPreferenceScreen().getSharedPreferences().getBoolean(key, false);

            tileVisible = isAvailable && !isInstalled;

            preference.setVisible(tileVisible);
        }

        boolean batteryVisible = false;
        {
            String key = getString(R.string.battery_optimization_key);
            Preference preference = Objects.requireNonNull(findPreference(key));

            batteryVisible = !isIgnoringBatteryOptimizations(context);

            preference.setVisible(batteryVisible);
        }

        {
            PreferenceCategory category = findPreference(getString(R.string.settings_suggestions_key));
            category.setVisible(tileVisible || batteryVisible);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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

        Preference preference = findPreference(key);
        if (preference instanceof SwitchPreferenceCompat) {
            ((SwitchPreferenceCompat) preference).setChecked(value);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        String key = preference.getKey();
        if (getString(R.string.start_control_key).equals(key)) {
            boolean value = (boolean) newValue;

            if (!value) {
                RotationService.stop(context);
                return true;
            }

            if (Permissions.hasNotificationPermission(context)) {
                Toast.makeText(context, R.string.require_notification_permission, Toast.LENGTH_LONG).show();
                mNotificationPermissionActivityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }

            RotationService.start(context);
            return true;
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        String key = preference.getKey();
        if (getString(R.string.install_tile_key).equals(key)) {
            requestAddTile(context);
        } else if (getString(R.string.battery_optimization_key).equals(key)) {
            requestBatteryOptimization(context);
            updateSuggestionsVisibility();
        } else if (getString(R.string.configure_presets_key).equals(key)) {
            if (Permissions.isAccessibilityServiceEnabled(context)) {
                PresetsActivity.start(context);
            } else {
                requestAccessibilityService(context);
            }
        }

        return true;
    }

    @SuppressLint("NewApi")
    private void requestAddTile(Context context) {
        StatusBarManager statusBarManager = context.getSystemService(StatusBarManager.class);

        statusBarManager.requestAddTileService(
                new ComponentName(context, RotationTileService.class),
                getString(R.string.tile_title),
                Icon.createWithResource(context, R.drawable.guard),
                context.getMainExecutor(),
                (status) -> {
                    if (status == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED || status == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                        getPreferenceManager().getSharedPreferences()
                                .edit()
                                .putBoolean(getString(R.string.install_tile_key), true)
                                .apply();

                        updateSuggestionsVisibility();
                    }
                }
        );
    }

    private boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    @SuppressLint("BatteryLife")
    private static void requestBatteryOptimization(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.fromParts("package", context.getPackageName(), null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    private static void requestAccessibilityService(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.accessibility_permission_required_title)
                .setMessage(R.string.accessibility_permission_required_description)
                .setPositiveButton(R.string.accessibility_permission_required_positive, (dialog, which) -> {
                    context.startActivity(Permissions.newOpenAccessibilityServiceSettingsIntent());
                })
                .setNegativeButton(R.string.accessibility_permission_required_negative, null)
                .show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (getString(R.string.guard_key).equals(key) || getString(R.string.mode_key).equals(key)) {
            RotationService.notifyConfigurationChanged(context);
        }

        refresh(sharedPreferences);
    }

    private void refresh(SharedPreferences sharedPreferences) {
        {
            String key = getString(R.string.start_control_key);
            boolean value = sharedPreferences.getBoolean(key, false);

            SwitchPreferenceCompat preference = findPreference(key);
            preference.setChecked(value);
        }

        {
            String key = getString(R.string.guard_key);
            boolean value = sharedPreferences.getBoolean(key, false);

            SwitchPreferenceCompat preference = findPreference(key);
            preference.setChecked(value);
        }

        {
            String key = getString(R.string.mode_key);
            String value = sharedPreferences.getString(key, RotationMode.AUTO.name());

            ListPreference preference = findPreference(key);
            preference.setValue(value);
        }
    }

}