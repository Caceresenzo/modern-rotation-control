package dev.caceresenzo.rotationcontrol.rotation.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.BuildConfig;
import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;

public class PowerBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = PowerBroadcastReceiver.class.getSimpleName();

    public static final String DEBUG_ACTION_POWER_CONNECTED = BuildConfig.APPLICATION_ID + ".DEBUG_ACTION_POWER_CONNECTED";
    public static final String DEBUG_ACTION_POWER_DISCONNECTED = BuildConfig.APPLICATION_ID + ".DEBUG_ACTION_POWER_DISCONNECTED";

    public static final String DISABLED_MODE = "DISABLED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isValidAction(intent.getAction(), Intent.ACTION_POWER_CONNECTED, DEBUG_ACTION_POWER_CONNECTED)) {
            changeMode(context, R.string.power_connected_mode_key, R.string.power_connected_toast);
        } else if (isValidAction(intent.getAction(), Intent.ACTION_POWER_DISCONNECTED, DEBUG_ACTION_POWER_DISCONNECTED)) {
            changeMode(context, R.string.power_disconnected_mode_key, R.string.power_disconnected_toast);
        }
    }

    public void changeMode(Context context, @StringRes int keyId, @StringRes int toastId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        String key = context.getString(keyId);
        String mode = sharedPreferences.getString(key, DISABLED_MODE);

        Log.i(TAG, String.format("received power - key=%s mode=%s", key, mode));

        if (DISABLED_MODE.equals(mode)) {
            return;
        }

        Toast.makeText(context, toastId, Toast.LENGTH_SHORT).show();

        RotationMode newMode = RotationMode.valueOf(mode);
        RotationService.tryActionInBackground(context, () -> RotationService.notifyChangeMode(context, newMode, true));
    }

    public boolean isValidAction(String action, String intentAction, String debugAction) {
        if (intentAction.equals(action)) {
            return true;
        }

        if (BuildConfig.DEBUG && debugAction.equals(action)) {
            return true;
        }

        return false;
    }

}