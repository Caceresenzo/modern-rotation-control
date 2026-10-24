package dev.caceresenzo.rotationcontrol.rotation;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import dev.caceresenzo.rotationcontrol.R;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class RotationServiceStateProvider implements ServiceConnection {

    private final Context mContext;
    private final Callback mCallback;

    private RotationService mService;
    private boolean mShouldUnbindService;

    private Listener mListener;
    private boolean mShouldUnregisterListener;

    public void bind() {
        if (mService == null) {
            Intent intent = new Intent(mContext, RotationService.class);
            mShouldUnbindService = mContext.bindService(intent, this, 0);
        }

        if (mListener == null) {
            mListener = new Listener();
        }

        if (!mShouldUnregisterListener) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(RotationService.ACTION_NOTIFY_CREATED);
            filter.addAction(RotationService.ACTION_NOTIFY_UPDATED);
            filter.addAction(RotationService.ACTION_NOTIFY_DESTROYED);

            ContextCompat.registerReceiver(mContext, mListener, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

            mShouldUnregisterListener = true;
        }

        mCallback.onRotationData(getState());
    }

    public void unbind() {
        if (mShouldUnbindService) {
            mContext.unbindService(this);

            mService = null;
            mShouldUnbindService = false;
        }

        if (mShouldUnregisterListener) {
            mContext.unregisterReceiver(mListener);

            mListener = null;
            mShouldUnregisterListener = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        RotationService.LocalBinder binder = (RotationService.LocalBinder) service;
        mService = binder.getService();

        mCallback.onRotationData(getStateFromService());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService = null;

        mCallback.onRotationData(getStateFromPreferences(false));
    }

    public State getState() {
        if (mService == null) {
            return getStateFromPreferences(false);
        } else {
            return getStateFromService();
        }
    }

    private State getStateFromPreferences(boolean running) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        RotationMode activeMode = RotationMode.fromPreferences(mContext);

        return new State(
                running,
                activeMode,
                preferences.getBoolean(mContext.getString(R.string.guard_key), true),
                false
        );
    }

    private State getStateFromService() {
        return new State(
                true,
                mService.getActiveMode(),
                mService.isGuard(),
                mService.isUsingPresets()
        );
    }

    @Value
    public static class State {

        boolean running;
        RotationMode activeMode;
        boolean guard;
        boolean presets;

        public boolean isGuardOrForced() {
            return guard || activeMode.doesRequireGuard();
        }

    }

    public interface Callback {

        default void onRotationData(State state) {
        }

    }

    private class Listener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case RotationService.ACTION_NOTIFY_CREATED: {
                    mCallback.onRotationData(getStateFromPreferences(true));
                    break;
                }

                case RotationService.ACTION_NOTIFY_UPDATED: {
                    State data = mService != null
                            ? getStateFromService()
                            : getStateFromPreferences(true);

                    mCallback.onRotationData(data);
                    break;
                }

                case RotationService.ACTION_NOTIFY_DESTROYED: {
                    mCallback.onRotationData(getStateFromPreferences(false));
                    break;
                }
            }
        }

    }

}