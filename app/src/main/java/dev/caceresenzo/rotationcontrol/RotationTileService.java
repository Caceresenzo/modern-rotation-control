package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class RotationTileService extends TileService implements ServiceConnection {

    public static final String TAG = RotationTileService.class.getSimpleName();

    private Listener mListener;
    private RotationService mService;

    @Override
    public void onStartListening() {
        super.onStartListening();

        Log.i(TAG, "onStartListening");

        if (mListener == null) {
            mListener = new Listener();

            IntentFilter filter = new IntentFilter();
            filter.addAction(RotationService.ACTION_NOTIFY_CREATED);
            filter.addAction(RotationService.ACTION_NOTIFY_UPDATED);
            filter.addAction(RotationService.ACTION_NOTIFY_DESTROYED);

            ContextCompat.registerReceiver(this, mListener, filter, ContextCompat.RECEIVER_EXPORTED);
        }

        if (mService == null) {
            Intent intent = new Intent(this, RotationService.class);
            bindService(intent, this, Context.BIND_AUTO_CREATE);
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

        if (mService != null) {
            unbindService(this);
            mService = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        RotationService.LocalBinder binder = (RotationService.LocalBinder) service;
        mService = binder.getService();

        updateTileUsingService();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    @Override
    public void onClick() {
        super.onClick();

        Log.i(TAG, "onClick");

        TileClickBehavior tileClickBehavior = TileClickBehavior.fromPreferences(this);

        switch (tileClickBehavior) {
            case TOGGLE_CONTROL: {
                setTileUnavailable();

                if (RotationService.isRunning(this)) {
                    RotationService.stop(this);
                } else {
                    RotationService.start(this);
                }

                break;
            }

            case SHOW_MODES_IF_CONTROLLING: {
                if (RotationService.isRunning(this)) {
                    showDialog(new QuickActionsDialog(this));
                } else {
                    setTileUnavailable();
                    RotationService.start(this);
                }

                break;
            }

            case ALWAYS_SHOW_MODES: {
                showDialog(new QuickActionsDialog(this));

                break;
            }
        }
    }

    public void setTileUnavailable() {
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    public void updateTile(boolean running) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        RotationMode activeMode = RotationMode.fromPreferences(this);
        boolean guard = preferences.getBoolean(getString(R.string.guard_key), true);
        boolean presets = false;

        updateTile(running, activeMode, guard, presets);
    }

    public void updateTileUsingService() {
        RotationMode activeMode = mService.getActiveMode();
        boolean guard = mService.isGuardEnabledOrForced();
        boolean presets = mService.isUsingPresets();

        updateTile(true, activeMode, guard, presets);
    }

    public void updateTile(boolean running, RotationMode activeMode, boolean guard, boolean presets) {
        Tile tile = getQsTile();

        String suffix = "";

        if (guard) {
            suffix = " " + getString(R.string.tile_with_guard);
        }

        if (presets) {
            suffix = " " + getString(R.string.tile_with_presets);
        }

        tile.setIcon(getIconWith(activeMode, guard, presets));

        String prefix;
        if (running) {
            tile.setState(Tile.STATE_ACTIVE);
            prefix = getString(R.string.tile_active);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            prefix = getString(R.string.tile_inactive);
        }

        tile.setSubtitle(String.format("%s: %s%s", prefix, getString(activeMode.stringId()), suffix));
        tile.updateTile();

        Log.d(TAG, String.format("updated tile - running=%s activeMode=%s guard=%s presets=%s", running, activeMode, guard, presets));
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

            Log.d(TAG, String.format("received intent - action=%s", action));

            switch (action) {
                case RotationService.ACTION_NOTIFY_CREATED: {
                    updateTile(true);
                    break;
                }

                case RotationService.ACTION_NOTIFY_UPDATED: {
                    if (mService != null) {
                        updateTileUsingService();
                    }

                    break;
                }

                case RotationService.ACTION_NOTIFY_DESTROYED: {
                    updateTile(false);
                    break;
                }
            }
        }
    }

    public Icon getIconWith(RotationMode mode, boolean guard, boolean presets) {
        if (!guard && !presets) {
            return Icon.createWithResource(this, mode.drawableId());
        }

        Bitmap mainBitmap = getBitmapFromDrawable(getDrawable(mode.drawableId()));
        Canvas canvas = new Canvas(mainBitmap);

        if (guard) {
            Bitmap bitmap = getBitmapFromDrawable(getDrawable(R.drawable.guard));
            Bitmap scaledBitmap = scaledBitmap(bitmap, 0.4f);

            int left = mainBitmap.getWidth() - scaledBitmap.getWidth();
            int top = mainBitmap.getHeight() - scaledBitmap.getHeight();

            {
                float centerX = left + (scaledBitmap.getWidth() / 2f);
                float centerY = top + (scaledBitmap.getHeight() / 2f);
                float radius = (scaledBitmap.getWidth() / 2f) * 1.05f;

                Paint paint = new Paint();
                paint.setBlendMode(BlendMode.CLEAR);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, radius, paint);
            }

            canvas.drawBitmap(scaledBitmap, left, top, null);
        }

        if (presets) {
            Bitmap bitmap = getBitmapFromDrawable(getDrawable(R.drawable.icon_smart_toy));
            Bitmap scaledBitmap = scaledBitmap(bitmap, 0.4f);

            int left = 0;
            int top = 0;

            {
                float centerX = left + (scaledBitmap.getWidth() / 2f);
                float centerY = top + (scaledBitmap.getHeight() / 2f);
                float radius = (scaledBitmap.getWidth() / 2f) * 1.05f;

                Paint paint = new Paint();
                paint.setBlendMode(BlendMode.CLEAR);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, radius, paint);
            }

            canvas.drawBitmap(scaledBitmap, left, top, null);
        }

        return Icon.createWithBitmap(mainBitmap);
    }

    private static Bitmap scaledBitmap(Bitmap original, float scale) {
        int width = (int) (original.getWidth() * scale);
        int height = (int) (original.getHeight() * scale);

        return Bitmap.createScaledBitmap(original, width, height, true);
    }

    private static Bitmap getBitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

}