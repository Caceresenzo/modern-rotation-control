package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class UnlockBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = UnlockBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, String.format("Received %s", intent.getAction()));
        if (!Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        boolean startControl = sharedPreferences.getBoolean(context.getString(R.string.start_control_key), false);
        boolean refreshOnUnlock = sharedPreferences.getBoolean(context.getString(R.string.refresh_on_unlock_key), false);

        Log.i(TAG, String.format("Received Unlock, start control? %s, refresh on unlock? %s", startControl, refreshOnUnlock));

        if (!startControl || !refreshOnUnlock) {
            return;
        }

        RotationService.notifyConfigurationChanged(context);
    }

}