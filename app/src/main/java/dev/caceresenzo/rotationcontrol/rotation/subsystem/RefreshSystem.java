package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import lombok.Getter;

public class RefreshSystem extends System {

    public static final String TAG = RotationService.TAG + "." + RefreshSystem.class.getSimpleName();

    private final Runnable mBroadcastToggleGuardIntent = new Runnable() {
        @Override
        public void run() {
            currentlyRefreshing = false;
            mService.restorePreviousMode();
        }
    };

    private long mDelay;

    private @Getter boolean currentlyRefreshing;

    public RefreshSystem(RotationService service) {
        super(service);
    }

    @Override
    public void onConfiguration(SharedPreferences preferences, boolean isFirstTime) {
        String rawDelay = PreferenceManager.getDefaultSharedPreferences(mService).getString(getString(R.string.refresh_mode_delay_key), "600");
        mDelay = Long.parseLong(rawDelay);
    }

    public void onDestroy() {
        cancel();
    }

    public void schedule() {
        currentlyRefreshing = true;

        getHandler().postDelayed(mBroadcastToggleGuardIntent, mDelay);
    }

    public void cancel() {
        currentlyRefreshing = false;

        getHandler().removeCallbacks(mBroadcastToggleGuardIntent);
    }

}