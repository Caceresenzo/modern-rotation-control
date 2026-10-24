package dev.caceresenzo.rotationcontrol.tile;

import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.WindowManager;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.RotationServiceStateProvider;
import dev.caceresenzo.rotationcontrol.settings.RotationSharedPreferences;

public class RotationTileService extends TileService implements RotationServiceStateProvider.Callback {

    public static final String TAG = RotationTileService.class.getSimpleName();

    private RotationServiceStateProvider mServiceStateProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        mServiceStateProvider = new RotationServiceStateProvider(this, this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        Log.i(TAG, "onStartListening");

        if (!RotationService.isRunning(this) && RotationSharedPreferences.from(getApplicationContext()).shouldStartControl()) {
            RotationService.start(this);
        }

        mServiceStateProvider.bind();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        Log.i(TAG, "onStopListening");

        mServiceStateProvider.unbind();
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
                    showDialogSafe(QuickActionsDialog.newInstance(this));
                } else {
                    setTileUnavailable();
                    RotationService.start(this);
                }

                break;
            }

            case ALWAYS_SHOW_MODES: {
                showDialogSafe(QuickActionsDialog.newInstance(this));

                break;
            }

            case SWITCH_TO_PORTRAIT_OR_LANDSCAPE: {
                RotationSharedPreferences preferences = RotationSharedPreferences.from(this);

                RotationMode mode = preferences.getMode();

                if (RotationMode.AUTO.equals(mode)) {
                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    int rotation = windowManager.getDefaultDisplay().getRotation();

                    mode = RotationMode.fromRotationValue(rotation);
                }

                if (mode.isLandscape()) {
                    mode = RotationMode.PORTRAIT_SENSOR;
                } else if (mode.isPortrait()) {
                    mode = RotationMode.LANDSCAPE_SENSOR;
                }

                preferences.setMode(mode);
                RotationService.notifyConfigurationChanged(this, true);

                break;
            }
        }
    }

    private void showDialogSafe(QuickActionsDialog dialog) {
        try {
            showDialog(dialog);
        } catch (WindowManager.BadTokenException exception) {
            Log.e(TAG, "showDialogSafe", exception);
        }
    }

    public void setTileUnavailable() {
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    @Override
    public void onRotationData(RotationServiceStateProvider.State state) {
        updateTile(state.isRunning(), state.getActiveMode(), state.isGuardOrForced(), state.isPresets());
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

    public Icon getIconWith(RotationMode mode, boolean guard, boolean presets) {
        if (!guard && !presets) {
            return Icon.createWithResource(this, mode.drawableId());
        }

        Bitmap mainBitmap = getBitmapFromDrawable(getDrawable(mode.drawableId()));
        Canvas canvas = new Canvas(mainBitmap);

        if (guard) {
            Bitmap bitmap = getBitmapFromDrawable(getDrawable(R.drawable.guard));
            Bitmap scaledBitmap = scaleBitmap(bitmap, 0.4f);

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
            Bitmap scaledBitmap = scaleBitmap(bitmap, 0.4f);

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

    private static Bitmap scaleBitmap(Bitmap original, float scale) {
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