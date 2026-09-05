package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.Intent;
import android.content.IntentFilter;

import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.receiver.OrientationBroadcastReceiver;
import dev.caceresenzo.rotationcontrol.rotation.receiver.UnlockBroadcastReceiver;

public class ExternalEventSystem extends System {

    public static final String TAG = RotationService.TAG + "." + ExternalEventSystem.class.getSimpleName();

    private UnlockBroadcastReceiver mUnlockBroadcastReceiver;
    private OrientationBroadcastReceiver mOrientationReceiver;

    public ExternalEventSystem(RotationService service) {
        super(service);
    }

    public void onCreate() {
        mUnlockBroadcastReceiver = new UnlockBroadcastReceiver();
        mService.registerReceiver(mUnlockBroadcastReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));

        mOrientationReceiver = new OrientationBroadcastReceiver();
        mService.registerReceiver(mOrientationReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
    }

    public void onDestroy() {
        if (mUnlockBroadcastReceiver != null) {
            mService.unregisterReceiver(mUnlockBroadcastReceiver);
            mUnlockBroadcastReceiver = null;
        }

        if (mOrientationReceiver != null) {
            mService.unregisterReceiver(mOrientationReceiver);
            mOrientationReceiver = null;
        }
    }

}