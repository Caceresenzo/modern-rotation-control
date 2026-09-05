package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.util.function.IntConsumer;

import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;

public class SuggestionSystem extends System implements View.OnClickListener {

    public static final boolean IS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

    public static final String TAG = RotationService.TAG + "." + SuggestionSystem.class.getSimpleName();

    private WindowManager mUiWindowManager;

    private View mSuggestionView;
    private RotationMode mSuggestedMode;
    private Object mSuggestionToken;

    private final Runnable mHideSuggestion = this::hideSuggestion;

    private final IntConsumer mOnProposedRotation = (value) -> {
        Log.i(TAG, String.format("onProposedRotation - value=%d", value));

        // NOTE: `ORIENTATION_UNKNOWN` or (-1) is when the device is on a flat surface
        RotationMode newMode = OrientationEventListener.ORIENTATION_UNKNOWN == value
                ? RotationMode.AUTO
                : RotationMode.fromRotationValue(value);

        if (newMode == mService.getActiveMode()) {
            return;
        }

        showSuggestion(newMode);
    };

    public SuggestionSystem(RotationService mService) {
        super(mService);
    }

    public void onCreate() {
        if (IS_SUPPORTED) {
            mUiWindowManager = createUiWindowManager();
            mUiWindowManager.addProposedRotationListener(mService.getMainExecutor(), mOnProposedRotation);
        }
    }

    public void onDestroy() {
        hideSuggestion();
        mSuggestionView = null;

        if (IS_SUPPORTED) {
            mUiWindowManager.removeProposedRotationListener(mOnProposedRotation);
            mUiWindowManager = null;
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = RotationService.newChangeModeIntent(view.getContext(), mSuggestedMode);
        view.getContext().startService(intent);

        hideSuggestion();
    }

    public void hideSuggestion() {
        if (mSuggestionToken != null) {
            mService.getWindowManager().removeView(mSuggestionView);
            getHandler().removeCallbacks(mHideSuggestion, mSuggestionToken);

            mSuggestionToken = null;
        }
    }

    public void showSuggestion(RotationMode suggestedMode) {
        if (mSuggestionView == null) {
            mSuggestionView = new ImageButton(mService.getApplicationContext());

            mSuggestionView.setOnClickListener(this);
        }

        if (mSuggestionToken != null) {
            getHandler().removeCallbacks(mHideSuggestion, mSuggestionToken);
        } else {
            WindowManager.LayoutParams mSuggestionParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );

            mSuggestionParams.gravity = Gravity.BOTTOM | Gravity.END;
            mSuggestionParams.x = 24;
            mSuggestionParams.y = 96;

            mService.getWindowManager().addView(mSuggestionView, mSuggestionParams);
        }

        mSuggestedMode = suggestedMode;
        ((ImageButton) mSuggestionView).setImageResource(suggestedMode.drawableId());
        Log.d(TAG, "suggestion: changed to: " + suggestedMode + " (res: " + suggestedMode.drawableId() + ")");

        mSuggestionToken = new Object();
        getHandler().postDelayed(mHideSuggestion, mSuggestionToken, 5000);
    }

    private WindowManager createUiWindowManager() {
        Display display = mService.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);

        Context uiContext = mService.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);

        return uiContext.getSystemService(WindowManager.class);
    }

}