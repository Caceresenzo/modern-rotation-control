package dev.caceresenzo.rotationcontrol;

import android.app.Application;

import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;

public class RotationControlApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);

        setDefaultValues();
    }

    private void setDefaultValues() {
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);

        PreferenceManager.setDefaultValues(this, R.xml.tile_preferences, true);
        PreferenceManager.setDefaultValues(this, R.xml.notification_preferences, true);
        PreferenceManager.setDefaultValues(this, R.xml.other_preferences, true);

        PreferenceManager.setDefaultValues(this, R.xml.auto_lock_preferences, true);
        PreferenceManager.setDefaultValues(this, R.xml.power_preferences, true);
    }

}