package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.SharedPreferences;
import android.os.Handler;

import androidx.annotation.StringRes;

import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.DisplayRotation;

public abstract class System {

    protected final RotationService mService;

    protected System(RotationService mService) {
        this.mService = mService;
    }

    public void onCreate() {
    }

    public void onConfiguration(SharedPreferences preferences, boolean isFirstTime) {
    }

    public void onDestroy() {
    }

    public Handler getHandler() {
        return mService.getHandler();
    }

    public String getString(@StringRes int id) {
        return mService.getString(id);
    }

    public DisplayRotation getCurrentDisplayRotation() {
        return mService.getCurrentDisplayRotation();
    }

}