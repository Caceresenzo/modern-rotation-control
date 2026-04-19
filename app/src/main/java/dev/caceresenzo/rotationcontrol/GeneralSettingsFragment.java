package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class GeneralSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    public static final int RESTART_SERVICE_DELAY_MILLISECOND = 200;

    private Handler mHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler(Looper.getMainLooper());

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.general_preferences, rootKey);

        findPreference(getString(R.string.show_notification_key)).setOnPreferenceChangeListener(this);
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
        if (getString(R.string.show_notification_key).equals(key)
                && RotationService.isRunning(context)
                && Permissions.hasNotificationPermission(context)
        ) {
            restartService();
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

        if (getString(R.string.buttons_key).equals(key)
                || getString(R.string.show_notification_key).equals(key)) {
            RotationService.notifyConfigurationChanged(context);
        }
    }

    private void restartService() {
        Context context = getContext();
        RotationService.stop(context);

        mHandler.postDelayed(() -> {
            RotationService.start(context);
        }, RESTART_SERVICE_DELAY_MILLISECOND);
    }
}