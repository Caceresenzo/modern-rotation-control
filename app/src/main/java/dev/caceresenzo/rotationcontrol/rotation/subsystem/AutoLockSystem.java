package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;

public class AutoLockSystem extends System {

    public static final String TAG = RotationService.TAG + "." + AutoLockSystem.class.getSimpleName();

    private boolean enabled;
    private int waitSeconds;
    private boolean force;

    private int lastDisplayRotationValue = -1;

    private final Runnable mTriggerAutoLock = () -> {
        Log.i(TAG, "triggering auto lock");

        triggerAutoLock();
    };

    public AutoLockSystem(RotationService service) {
        super(service);
    }

    public void onCreate() {
        reset();
    }

    public void onDestroy() {
        getHandler().removeCallbacks(mTriggerAutoLock);
    }

    public void load(SharedPreferences preferences) {
        this.waitSeconds = Integer.parseInt(preferences.getString(mService.getString(R.string.auto_lock_key), "0"));
        this.enabled = this.waitSeconds != 0;
        this.force = preferences.getBoolean(mService.getString(R.string.auto_lock_force_key), false);
    }

    public void reset() {
        getHandler().removeCallbacks(mTriggerAutoLock);

        boolean isCurrentlyRefreshing = mService.isCurrentlyRefreshing();
        if (!enabled || isCurrentlyRefreshing) {
            Log.d(TAG, String.format("setupAutoLock not enabled - autoLockWait=%s currentlyRefreshing=%s", enabled, isCurrentlyRefreshing));
            return;
        }

        RotationMode activeMode = mService.getActiveMode();
        if (!RotationMode.AUTO.equals(activeMode)) {
            Log.d(TAG, String.format("setupAutoLock cancelled - activeMode=%s", activeMode));
            return;
        }

        lastDisplayRotationValue = getCurrentDisplayRotation();
        Log.d(TAG, String.format("setupAutoLock - lastDisplayRotationValue=%s", lastDisplayRotationValue));

        getHandler().postDelayed(mTriggerAutoLock, waitSeconds * 1000L);
    }


    private void triggerAutoLock() {
        int currentDisplayRotation = getCurrentDisplayRotation();
        if (lastDisplayRotationValue == -1) {
            lastDisplayRotationValue = currentDisplayRotation;
        }

        RotationMode newMode = RotationMode.fromPreferences(mService, R.string.auto_lock_mode_key, RotationMode.AUTO);
        if (newMode == RotationMode.AUTO) {
            newMode = RotationMode.fromRotationValue(lastDisplayRotationValue);
        } else if (!force) {
            RotationMode currentMode = RotationMode.fromRotationValue(currentDisplayRotation);

            if (!isCompatible(newMode, currentMode)) {
                return;
            }
        }

        Toast.makeText(mService, mService.getString(R.string.auto_lock_trigger, mService.getString(newMode.stringId())), Toast.LENGTH_SHORT).show();

        PreferenceManager.getDefaultSharedPreferences(mService)
                .edit()
                .putString(mService.getString(R.string.mode_key), newMode.name())
                .apply();

        RotationService.notifyConfigurationChanged(mService);
    }

    private boolean isCompatible(RotationMode toMode, RotationMode currentMode) {
        if (toMode.equals(currentMode)) {
            return true;
        }

        if (RotationMode.LANDSCAPE_SENSOR.equals(toMode)) {
            return RotationMode.LANDSCAPE.equals(currentMode) || RotationMode.LANDSCAPE_REVERSE.equals(currentMode);
        }

        if (RotationMode.PORTRAIT_SENSOR.equals(toMode)) {
            return RotationMode.PORTRAIT.equals(currentMode) || RotationMode.PORTRAIT_REVERSE.equals(currentMode);
        }

        return false;
    }

    public boolean hasRotationValueChanged() {
        int rotationValue = getCurrentDisplayRotation();

        return lastDisplayRotationValue != rotationValue;
    }

    public void resetIfRotationChanged() {
        if (hasRotationValueChanged()) {
            reset();
        }
    }

}