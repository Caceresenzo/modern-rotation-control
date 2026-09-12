package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntConsumer;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.DisplayRotation;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.SuggestedRotationMode;
import lombok.AllArgsConstructor;

public class SuggestionsSystem extends System implements View.OnClickListener {

    public static final boolean IS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

    public static final String TAG = RotationService.TAG + "." + SuggestionsSystem.class.getSimpleName();

    private static final int DEFAULT_PADDING = 24;
    private static final int BIGGER_PADDING = 48;

    private boolean mEnabled;
    private Set<SuggestedRotationMode> mSuggestForModes = EnumSet.noneOf(SuggestedRotationMode.class);
    private int mExpiration;
    private boolean mBiggerButton;
    private boolean mLeftHanded;

    private WindowManager mUiWindowManager;

    private View mSuggestionView;
    private WindowManager.LayoutParams mSuggestionParams;
    private SuggestedRotationMode mSuggestedMode;
    private Object mSuggestionToken;
    private DisplayRotation mLastConcreteSuggestedMode;

    private final Runnable mHideSuggestion = this::hideSuggestion;

    // NOTE: `ORIENTATION_UNKNOWN` or (-1) is when the device is on a flat surface (more or less)
    private final IntConsumer mOnProposedRotation = (value) -> {
        Log.i(TAG, String.format("onProposedRotation - value=%d", value));

        if (!mEnabled) {
            return;
        }

        DisplayRotation proposedRotation = DisplayRotation.fromValue(value, null);
        if (proposedRotation != null) {
            // NOTE: Could be rejected by condition below, so naming is a bit off
            mLastConcreteSuggestedMode = proposedRotation;
        }

        SuggestedRotationMode suggestedRotationMode = SuggestedRotationMode.fromNullableAsAuto(proposedRotation);
        Log.d(TAG, "onProposedRotation: " + suggestedRotationMode + " (enabled: " + mSuggestForModes + ")");

        if (!mSuggestForModes.contains(suggestedRotationMode)) {
            return;
        }

        if (suggestedRotationMode.rotationMode() == mService.getActiveMode()) {
            hideSuggestion();
        } else {
            // TODO: Not great, need better algorithm to take into account the current rotation
            getHandler().postDelayed(() -> showSuggestion(suggestedRotationMode), 100);
        }
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
        mLeftHanded = preferences.getBoolean(getString(R.string.suggestion_swap_button_side_key), false);

        {
            mSuggestForModes.clear();
            Iterable<String> suggestedModes = preferences.getStringSet(getString(R.string.suggest_for_modes_key), null);
            if (suggestedModes == null) {
                suggestedModes = Arrays.asList(mService.getResources().getStringArray(R.array.suggestions_values));
            }

            for (String mode : suggestedModes) {
                mSuggestForModes.add(SuggestedRotationMode.valueOf(mode));
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
        Intent intent = RotationService.newChangeModeIntent(view.getContext(), mSuggestedMode.rotationMode());
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

    public void showSuggestion(SuggestedRotationMode suggestedMode) {
        if (mSuggestionView == null) {
            mSuggestionView = new ImageButton(mService.getApplicationContext());
            mSuggestionView.setOnClickListener(this);
            applyPadding();

            mSuggestionParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
            mSuggestionParams.x = 24;
            mSuggestionParams.y = 96;
        }

        IconLocation iconLocation = computeIconLocation(suggestedMode);
        mSuggestionParams.gravity = iconLocation.gravity;
        mSuggestionView.setRotation(iconLocation.rotation);

        if (mSuggestionToken != null) {
            getHandler().removeCallbacks(mHideSuggestion, mSuggestionToken);

            mService.getWindowManager().updateViewLayout(mSuggestionView, mSuggestionParams);
        } else {
            mService.getWindowManager().addView(mSuggestionView, mSuggestionParams);
        }

        mSuggestedMode = suggestedMode;

        ((ImageButton) mSuggestionView).setImageResource(suggestedMode.drawableId());
        // ((ImageButton) mSuggestionView).setImageResource(RotationMode.PORTRAIT_REVERSE.drawableId());

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

    @SuppressLint("RtlHardcoded")
    private IconLocation computeIconLocation(SuggestedRotationMode suggestedRotation) {
        RotationMode activeMode = mService.getActiveMode();
        if (activeMode == RotationMode.AUTO) {
            int side = mLeftHanded ? Gravity.LEFT : Gravity.RIGHT;
            return new IconLocation(Gravity.BOTTOM | side, 0);
        }

        Log.d(TAG, "computeIconLocation: " + suggestedRotation + " (active: " + activeMode + ", previous: " + mLastConcreteSuggestedMode + ")");
        if (suggestedRotation == SuggestedRotationMode.AUTO && mLastConcreteSuggestedMode != null) {
            suggestedRotation = SuggestedRotationMode.from(mLastConcreteSuggestedMode);
        }

        DisplayRotation currentRotation = getCurrentDisplayRotation();
        int relativeDirection = (currentRotation.rotationValue() - suggestedRotation.rotationValue() + 4) % 4;

        // NOTE: Rotation is relative to current rotation, not absolute!
        switch (relativeDirection) {
            default:
            case Surface.ROTATION_0: {
                int side = mLeftHanded ? Gravity.LEFT : Gravity.RIGHT;
                return new IconLocation(Gravity.BOTTOM | side, 0);
            }

            case Surface.ROTATION_90: {
                int side = mLeftHanded ? Gravity.BOTTOM : Gravity.TOP;
                return new IconLocation(side | Gravity.RIGHT, 270);
            }

            case Surface.ROTATION_180: {
                int side = mLeftHanded ? Gravity.RIGHT : Gravity.LEFT;
                return new IconLocation(Gravity.TOP | side, 180);
            }

            case Surface.ROTATION_270: {
                int side = mLeftHanded ? Gravity.TOP : Gravity.BOTTOM;
                return new IconLocation(side | Gravity.LEFT, 90);
            }
        }
    }

    @SuppressLint("NewApi")
    private WindowManager createUiWindowManager() {
        Display display = mService.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);

        Context uiContext = mService.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);

        return uiContext.getSystemService(WindowManager.class);
    }

    @AllArgsConstructor
    private static class IconLocation {

        int gravity;
        int rotation;

    }

}