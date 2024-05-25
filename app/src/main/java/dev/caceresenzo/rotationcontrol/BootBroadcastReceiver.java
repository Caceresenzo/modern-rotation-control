package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.i(TAG, "Received Boot");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (!sharedPreferences.getBoolean(context.getString(R.string.start_on_boot_key), false)) {
            return;
        }

        Intent serviceIntent = new Intent(context.getApplicationContext(), RotationService.class);
        serviceIntent.putExtra(RotationService.INTENT_ACTION, RotationService.ACTION_START);
        context.startForegroundService(serviceIntent);
    }

}