package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import dev.caceresenzo.rotationcontrol.R;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntConsumer;

import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;

public class SuggestionsSystem extends System implements View.OnClickListener {

    public static final boolean IS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

    public static final String TAG = RotationService.TAG + "." + SuggestionsSystem.class.getSimpleName();

    private static final int DEFAULT_PADDING = 24;
    private static final int BIGGER_PADDING = 48;

    private boolean mEnabled;
    private Set<RotationMode> mSuggestForModes = EnumSet.noneOf(RotationMode.class);
    private int mExpiration;
    private boolean mBiggerButton;

    private WindowManager mUiWindowManager;

    private View mSuggestionView;
    private RotationMode mSuggestedMode;
    private Object mSuggestionToken;

    private final Runnable mHideSuggestion = this::hideSuggestion;

    private final IntConsumer mOnProposedRotation = (value) -> {
        Log.i(TAG, String.format("onProposedRotation - value=%d", value));

        if (!mEnabled) {
            return;
        }

        // NOTE: `ORIENTATION_UNKNOWN` or (-1) is when the device is on a flat surface
        RotationMode newMode = OrientationEventListener.ORIENTATION_UNKNOWN == value
                ? RotationMode.AUTO
                : RotationMode.fromRotationValue(value);

        Log.d(TAG, "onProposedRotation: " + newMode + " (enabled: " + mSuggestForModes + ")");

        if (!mSuggestForModes.contains(newMode)) {
            return;
        }

        if (newMode == mService.getActiveMode()) {
            return;
        }

        showSuggestion(newMode);
    };

    public SuggestionsSystem(RotationService mService) {
        super(mService);
    }

    public void onCreate() {
        if (IS_SUPPORTED) {
            mUiWindowManager = createUiWindowManager();
            mUiWindowManager.addProposedRotationListener(mService.getMainExecutor(), mOnProposedRotation);
        }
    }

    @Override
    public void onConfiguration(SharedPreferences preferences, boolean isFirstTime) {
        mEnabled = preferences.getBoolean(getString(R.string.suggestions_enabled_key), true);
        mExpiration = preferences.getInt(getString(R.string.suggestion_expiration_key), 3000);

        {
            mSuggestForModes.clear();
            Iterable<String> suggestedModes = preferences.getStringSet(getString(R.string.suggest_for_modes_key), null);
            if (suggestedModes == null) {
                suggestedModes = Arrays.asList(mService.getResources().getStringArray(R.array.suggestions_values));
            }

            for (String mode : suggestedModes) {
                mSuggestForModes.add(RotationMode.valueOf(mode));
            }
        }

        {
            boolean newBiggerButton = preferences.getBoolean(getString(R.string.suggestion_button_bigger_key), false);
            boolean biggerButtonChanged = !isFirstTime && mBiggerButton != newBiggerButton;
            mBiggerButton = newBiggerButton;

            if (biggerButtonChanged && mSuggestionView != null) {
                applyPadding();
            }
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
            applyPadding();

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

        mSuggestionToken = new Object();
        getHandler().postDelayed(mHideSuggestion, mSuggestionToken, mExpiration);
    }

    private void applyPadding() {
        if (mBiggerButton) {
            mSuggestionView.setPadding(BIGGER_PADDING, BIGGER_PADDING, BIGGER_PADDING, BIGGER_PADDING);
        } else {
            mSuggestionView.setPadding(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        }
    }

    private WindowManager createUiWindowManager() {
        Display display = mService.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);

        Context uiContext = mService.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);

        return uiContext.getSystemService(WindowManager.class);
    }

}