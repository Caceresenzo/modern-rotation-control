package dev.caceresenzo.rotationcontrol.rotation.subsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntConsumer;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.DisplayRotation;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.SuggestedRotationMode;
import dev.caceresenzo.rotationcontrol.util.Colors;
import lombok.Value;

public class SuggestionsSystem extends System implements View.OnClickListener {

    public static final boolean IS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

    public static final String TAG = RotationService.TAG + "." + SuggestionsSystem.class.getSimpleName();

    private static final int DEFAULT_PADDING = 24;
    private static final int BIGGER_PADDING = 48;

    private boolean mEnabled;
    private boolean mDisableWhenUseless;
    private Set<SuggestedRotationMode> mSuggestForModes = EnumSet.noneOf(SuggestedRotationMode.class);
    private int mExpiration;
    private boolean mCoSuggestAuto;
    private boolean mBiggerButton;
    private boolean mLeftHanded;

    private PowerManager mPowerManager;

    private LinearLayout mSuggestionView;
    private ImageButton mSuggestedModeButtonView;
    private ImageButton mAutoModeButtonView;

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
        mPowerManager = mService.getSystemService(PowerManager.class);

        if (IS_SUPPORTED) {
            mService.getWindowManager().addProposedRotationListener(mService.getMainExecutor(), mOnProposedRotation);
        }
    }

    @Override
    public void onConfiguration(SharedPreferences preferences, boolean isFirstTime) {
        mEnabled = preferences.getBoolean(getString(R.string.suggestions_enabled_key), true);
        mDisableWhenUseless = preferences.getBoolean(getString(R.string.suggestion_disable_when_useless_key), true);
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
            boolean newCoSuggestAuto = preferences.getBoolean(getString(R.string.suggestion_co_suggest_auto_key), true);
            boolean coSuggestAutoChanged = !isFirstTime && mCoSuggestAuto != newCoSuggestAuto;
            mCoSuggestAuto = newCoSuggestAuto;

            if (coSuggestAutoChanged && mSuggestionView != null) {
                applyCoSuggestButtonVisibility();
            }
        }

        {
            boolean newBiggerButton = preferences.getBoolean(getString(R.string.suggestion_button_bigger_key), false);
            boolean biggerButtonChanged = !isFirstTime && mBiggerButton != newBiggerButton;
            mBiggerButton = newBiggerButton;

            if (biggerButtonChanged && mSuggestionView != null) {
                applyButtonChanges(mSuggestedModeButtonView);
                applyButtonChanges(mAutoModeButtonView);
            }
        }
    }

    public void onDestroy() {
        hideSuggestion();

        mSuggestionView = null;
        mSuggestedModeButtonView = null;
        mAutoModeButtonView = null;

        if (IS_SUPPORTED) {
            mService.getWindowManager().removeProposedRotationListener(mOnProposedRotation);
        }
    }

    @Override
    public void onClick(View view) {
        RotationMode newMode = RotationMode.AUTO;
        if (R.id.suggested_mode == view.getId()) {
            newMode = mSuggestedMode.rotationMode();
        }

        Intent intent = RotationService.newChangeModeIntent(view.getContext(), newMode);
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
        // NOTE: Adding then removing an overlay while the screen is off can make the system destroy the window context, which silently kills the proposed rotation listener
        if (!mPowerManager.isInteractive()) {
            return;
        }

        if (!mService.canDrawOverlaysOrStop()) {
            return;
        }

        if (mDisableWhenUseless && isUselessSuggestion(suggestedMode)) {
            hideSuggestion();
            return;
        }

        if (mSuggestionView == null) {
            mSuggestionView = (LinearLayout) LayoutInflater
                    .from(mService.getThemedContext())
                    .inflate(R.layout.suggestion_buttons, null);

            mSuggestedModeButtonView = mSuggestionView.findViewById(R.id.suggested_mode);
            mAutoModeButtonView = mSuggestionView.findViewById(R.id.mode_auto);

            mSuggestedModeButtonView.setOnClickListener(this);
            mAutoModeButtonView.setOnClickListener(this);

            applyButtonChanges(mSuggestedModeButtonView);
            applyButtonChanges(mAutoModeButtonView);

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
        mSuggestionParams.gravity = iconLocation.getRootGravity();
        mSuggestedModeButtonView.setRotation(iconLocation.getIconRotation());
        mAutoModeButtonView.setRotation(iconLocation.getIconRotation());
        mSuggestionView.setOrientation(iconLocation.getLayoutOrientation());

        if (mSuggestionToken != null) {
            getHandler().removeCallbacks(mHideSuggestion, mSuggestionToken);

            mService.getWindowManager().updateViewLayout(mSuggestionView, mSuggestionParams);
        } else {
            mService.getWindowManager().addView(mSuggestionView, mSuggestionParams);
        }

        mSuggestedMode = suggestedMode;

        mSuggestedModeButtonView.setImageResource(suggestedMode.drawableId());
        applyCoSuggestButtonVisibility();

        mSuggestionToken = new Object();
        getHandler().postDelayed(mHideSuggestion, mSuggestionToken, mExpiration);
    }

    public boolean isUselessSuggestion(SuggestedRotationMode suggestedMode) {
        RotationMode activeMode = mService.getActiveMode();

        switch (activeMode) {

            case AUTO: {
                return true;
            }

            case PORTRAIT_SENSOR: {
                return suggestedMode.isPortrait();
            }

            case LANDSCAPE_SENSOR: {
                return suggestedMode.isLandscape();
            }

            default: {
                return false;
            }

        }
    }

    private void applyButtonChanges(ImageButton button) {
        if (mBiggerButton) {
            button.setPadding(BIGGER_PADDING, BIGGER_PADDING, BIGGER_PADDING, BIGGER_PADDING);
        } else {
            button.setPadding(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
        }

        button.setColorFilter(Colors.getActive(mService.getThemedContext()));
    }

    private void applyCoSuggestButtonVisibility() {
        boolean hideCoSuggest = !mCoSuggestAuto;
        boolean isAutoSuggested = SuggestedRotationMode.AUTO.equals(mSuggestedMode);
        boolean isCurrentAuto = RotationMode.AUTO.equals(mService.getActiveMode());

        mAutoModeButtonView.setVisibility(
                hideCoSuggest || isAutoSuggested || isCurrentAuto
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    @SuppressLint("RtlHardcoded")
    private IconLocation computeIconLocation(SuggestedRotationMode suggestedRotation) {
        RotationMode activeMode = mService.getActiveMode();
        if (activeMode == RotationMode.AUTO) {
            int side = mLeftHanded ? Gravity.LEFT : Gravity.RIGHT;
            return new IconLocation(Gravity.BOTTOM | side, 0, LinearLayout.HORIZONTAL);
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
                return new IconLocation(Gravity.BOTTOM | side, 0, LinearLayout.HORIZONTAL);
            }

            case Surface.ROTATION_90: {
                int side = mLeftHanded ? Gravity.BOTTOM : Gravity.TOP;
                return new IconLocation(side | Gravity.RIGHT, 270, LinearLayout.VERTICAL);
            }

            case Surface.ROTATION_180: {
                int side = mLeftHanded ? Gravity.RIGHT : Gravity.LEFT;
                return new IconLocation(Gravity.TOP | side, 180, LinearLayout.HORIZONTAL);
            }

            case Surface.ROTATION_270: {
                int side = mLeftHanded ? Gravity.TOP : Gravity.BOTTOM;
                return new IconLocation(side | Gravity.LEFT, 90, LinearLayout.VERTICAL);
            }
        }
    }

    @Value
    private static class IconLocation {

        int rootGravity;
        int iconRotation;
        int layoutOrientation;

    }

}