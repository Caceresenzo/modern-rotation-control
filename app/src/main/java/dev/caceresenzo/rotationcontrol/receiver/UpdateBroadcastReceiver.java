package dev.caceresenzo.rotationcontrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.BuildConfig;
import dev.caceresenzo.rotationcontrol.R;

public class UpdateBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = UpdateBroadcastReceiver.class.getSimpleName();

    private static final String DEBUG_ACTION_MY_PACKAGE_REPLACED = BuildConfig.APPLICATION_ID + ".DEBUG_MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isValidAction(intent.getAction())) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean wasRunningBefore = sharedPreferences.getBoolean(context.getString(R.string.start_control_key), false);

        Log.i(TAG, String.format("Received Update, was running before? %s", wasRunningBefore));

        if (!wasRunningBefore) {
            return;
        }

        BootBroadcastReceiver.tryToStartOrShowNotification(context);
    }

    public boolean isValidAction(String action) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return true;
        }

        if (BuildConfig.DEBUG && DEBUG_ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return true;
        }

        return false;
    }

}