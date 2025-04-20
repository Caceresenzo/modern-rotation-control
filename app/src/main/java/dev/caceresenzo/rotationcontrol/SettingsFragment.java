package dev.caceresenzo.rotationcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
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

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static final String TAG = SettingsFragment.class.getSimpleName();
    public static final int RESTART_SERVICE_DELAY_MILLISECOND = 200;

    private Handler mHandler;
    private ActivityResultLauncher<String> mNotificationPermissionActivityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        mHandler = new Handler(Looper.getMainLooper());
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
        findPreference(getString(R.string.show_notification_key)).setOnPreferenceChangeListener(this);

        {
            String key = getString(R.string.install_tile_key);

            Preference preference = findPreference(key);
            preference.setOnPreferenceClickListener(this);

            boolean isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
            boolean isInstalled = getPreferenceScreen().getSharedPreferences().getBoolean(key, false);
            preference.setVisible(isAvailable && !isInstalled);
        }

        {
            String key = getString(R.string.battery_optimization_key);

            Preference preference = findPreference(key);
            preference.setOnPreferenceClickListener(this);

            boolean isAlready = isIgnoringBatteryOptimizations(getContext());
            preference.setVisible(!isAlready);
        }
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

        if (getString(R.string.show_notification_key).equals(key)
                && RotationService.isRunning(context)
                && hasNotificationPermission(context)
        ) {
            restartService();
            return true;
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        String key = preference.getKey();

        Context context = getContext();
        if (context == null) {
            return false;
        }

        if (getString(R.string.install_tile_key).equals(key)) {
            requestAddTile(context, preference);
        } else if (getString(R.string.battery_optimization_key).equals(key)) {
            requestBatteryOptimization(context);
            preference.setVisible(false);
        }

        return true;
    }

    @SuppressLint("NewApi")
    private void requestAddTile(Context context, Preference preference) {
        StatusBarManager statusBarManager = context.getSystemService(StatusBarManager.class);

        statusBarManager.requestAddTileService(
                new ComponentName(context, RotationTileService.class),
                getString(R.string.tile_title),
                Icon.createWithResource(context, R.drawable.guard),
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        getActivity().runOnUiThread(command);
                    }
                },
                new Consumer<Integer>() {
                    @Override
                    public void accept(Integer status) {
                        switch (status) {
                            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED: {
                                getPreferenceManager().getSharedPreferences()
                                        .edit()
                                        .putBoolean(getString(R.string.install_tile_key), false)
                                        .apply();

                                break;
                            }

                            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED:
                            case StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED: {
                                preference.setVisible(false);

                                getPreferenceManager().getSharedPreferences()
                                        .edit()
                                        .putBoolean(getString(R.string.install_tile_key), true)
                                        .apply();

                                break;
                            }
                        }
                    }
                }
        );
    }

    private boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /**
     * @author <a href="https://stackoverflow.com/a/77118142/7292958">HSMKU from StackOverflow</a>
     */
    @SuppressLint("BatteryLife")
    private void requestBatteryOptimization(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.fromParts("package", context.getPackageName(), null))
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        context.startActivity(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Log.i(TAG, String.format("onSharedPreferenceChanged - key=%s", key));

        Context context = getContext();
        if (context == null) {
            return;
        }

        if (getString(R.string.buttons_key).equals(key) || getString(R.string.guard_key).equals(key) || getString(R.string.mode_key).equals(key) || getString(R.string.show_notification_key).equals(key) || getString(R.string.auto_lock_key).equals(key)) {
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

    private void setStartControlEnabled(boolean enabled) {
        findPreference(getString(R.string.start_control_key)).setEnabled(enabled);
    }

    private void restartService() {
        Context context = getContext();

        RotationService.stop(context);
        setStartControlEnabled(false);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RotationService.start(context);
                setStartControlEnabled(true);
            }
        }, RESTART_SERVICE_DELAY_MILLISECOND);
    }

    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

}