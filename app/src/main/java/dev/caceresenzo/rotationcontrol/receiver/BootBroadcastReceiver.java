package dev.caceresenzo.rotationcontrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.BuildConfig;
import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;

public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootBroadcastReceiver.class.getSimpleName();

    private static final String DEBUG_BOOT_COMPLETED_ACTION = BuildConfig.APPLICATION_ID + ".DEBUG_BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isValidAction(intent.getAction())) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean startOnBoot = sharedPreferences.getBoolean(context.getString(R.string.start_on_boot_key), false);

        Log.i(TAG, String.format("received boot - startOnBoot=%s", startOnBoot));

        if (!startOnBoot) {
            return;
        }

        RotationService.tryActionInBackground(context, () -> RotationService.start(context));
    }

    public boolean isValidAction(String action) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return true;
        }

        if (BuildConfig.DEBUG && DEBUG_BOOT_COMPLETED_ACTION.equals(action)) {
            return true;
        }

        return false;
    }

}