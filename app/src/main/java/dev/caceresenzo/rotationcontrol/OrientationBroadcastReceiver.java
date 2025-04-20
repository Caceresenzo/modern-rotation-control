package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OrientationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = OrientationBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, String.format("Received %s", intent.getAction()));
        if (!Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
            return;
        }

        RotationService.notifyOrientationChanged(context);
    }

}