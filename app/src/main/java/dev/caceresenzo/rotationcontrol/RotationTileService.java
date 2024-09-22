package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

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

        Tile tile = getQsTile();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();

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

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            RotationMode activeMode = RotationMode.valueOf(preferences.getString(getString(R.string.mode_key), RotationMode.AUTO.name()));
            boolean guard = preferences.getBoolean(getString(R.string.guard_key), true);

            if (guard) {
                Log.i(TAG, String.format("set icon with guard - activeMode=%s", activeMode));
                tile.setIcon(getIconWithGuard(activeMode));
                tile.setSubtitle(getString(R.string.tile_active_with_guard, getString(activeMode.nameId())));
            } else {
                Log.i(TAG, String.format("set icon - activeMode=%s", activeMode));
                tile.setIcon(Icon.createWithResource(this, activeMode.drawableId()));
                tile.setSubtitle(getString(R.string.tile_active, getString(activeMode.nameId())));
            }
        } else {
            Log.i(TAG, "set inactive");
            tile.setState(Tile.STATE_INACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.mode_auto));
            tile.setSubtitle(getString(R.string.tile_inactive));
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

    public Icon getIconWithGuard(RotationMode mode) {
        Bitmap mainBitmap = getBitmapFromDrawable(getDrawable(mode.drawableId()));
        Canvas canvas = new Canvas(mainBitmap);

        Bitmap guardBitmap = getBitmapFromDrawable(getDrawable(R.drawable.guard));
        Bitmap scaledGuardBitmap = scaledBitmap(guardBitmap, 0.4f);

        int left = mainBitmap.getWidth() - scaledGuardBitmap.getWidth();
        int top = mainBitmap.getHeight() - scaledGuardBitmap.getHeight();

        {
            float centerX = left + (scaledGuardBitmap.getWidth() / 2f);
            float centerY = top + (scaledGuardBitmap.getHeight() / 2f);
            float radius = (scaledGuardBitmap.getWidth() / 2f) * 1.05f;

            Paint paint = new Paint();
            paint.setBlendMode(BlendMode.CLEAR);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        canvas.drawBitmap(scaledGuardBitmap, left, top, null);

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