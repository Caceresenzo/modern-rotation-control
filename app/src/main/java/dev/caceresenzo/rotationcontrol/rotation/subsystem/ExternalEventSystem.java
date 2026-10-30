package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.receiver.OrientationBroadcastReceiver;
import dev.caceresenzo.rotationcontrol.rotation.receiver.PowerBroadcastReceiver;
import dev.caceresenzo.rotationcontrol.rotation.receiver.UnlockBroadcastReceiver;

public class ExternalEventSystem extends System {

    public static final String TAG = RotationService.TAG + "." + ExternalEventSystem.class.getSimpleName();

    private UnlockBroadcastReceiver mUnlockReceiver;
    private OrientationBroadcastReceiver mOrientationReceiver;
    private PowerBroadcastReceiver mPowerReceiver;

    public ExternalEventSystem(RotationService service) {
        super(service);
    }

    public void onCreate() {
        mUnlockReceiver = new UnlockBroadcastReceiver();
        registerReceiver(mUnlockReceiver, Intent.ACTION_USER_PRESENT);

        mOrientationReceiver = new OrientationBroadcastReceiver();
        registerReceiver(mOrientationReceiver, Intent.ACTION_CONFIGURATION_CHANGED);

        mPowerReceiver = new PowerBroadcastReceiver();
        registerReceiver(mPowerReceiver, new String[]{
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED,
                PowerBroadcastReceiver.DEBUG_ACTION_POWER_CONNECTED,
                PowerBroadcastReceiver.DEBUG_ACTION_POWER_DISCONNECTED
        });
    }

    public void onDestroy() {
        if (mUnlockReceiver != null) {
            mService.unregisterReceiver(mUnlockReceiver);
            mUnlockReceiver = null;
        }

        if (mOrientationReceiver != null) {
            mService.unregisterReceiver(mOrientationReceiver);
            mOrientationReceiver = null;
        }

        if (mPowerReceiver != null) {
            mService.unregisterReceiver(mPowerReceiver);
            mPowerReceiver = null;
        }
    }

    private void registerReceiver(BroadcastReceiver receiver, String... actions) {
        IntentFilter filter = new IntentFilter();
        for (String action : actions) {
            filter.addAction(action);
        }

        ContextCompat.registerReceiver(mService, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
    }

}