package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class RotationTileService extends TileService {

    public static final String TAG = RotationTileService.class.getSimpleName();

    private Listener mListener;

    @Override
    public void onStartListening() {
        super.onStartListening();

        Log.i(TAG, "onStartListening");

        if (mListener == null) {
            mListener = new Listener();

            IntentFilter filter = new IntentFilter();
            filter.addAction(RotationService.ACTION_NOTIFY_CREATED);
            filter.addAction(RotationService.ACTION_NOTIFY_DESTROYED);

            ContextCompat.registerReceiver(this, mListener, filter, ContextCompat.RECEIVER_EXPORTED);
        }

        updateTile(RotationService.isRunning(this));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        Log.i(TAG, "onStopListening");

        if (mListener != null) {
            unregisterReceiver(mListener);

            mListener = null;
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        Log.i(TAG, "onClick");

        if (RotationService.isRunning(this)) {
            RotationService.stop(this);
        } else {
            RotationService.start(this);
        }
    }

    public void updateTile(boolean running) {
        Tile tile = getQsTile();

        if (running) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }

    public class Listener extends BroadcastReceiver {

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
                    updateTile(true);
                    break;
                }

                case RotationService.ACTION_NOTIFY_DESTROYED: {
                    updateTile(false);
                    break;
                }
            }
        }
    }

}