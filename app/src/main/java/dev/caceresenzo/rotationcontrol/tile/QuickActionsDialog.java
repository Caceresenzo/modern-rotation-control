package dev.caceresenzo.rotationcontrol.tile;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.rotation.RotationServiceStateProvider;
import dev.caceresenzo.rotationcontrol.settings.ActionButton;
import dev.caceresenzo.rotationcontrol.util.Colors;

public class QuickActionsDialog extends Dialog implements View.OnClickListener, RotationServiceStateProvider.Callback {

    private RotationServiceStateProvider mServiceStateProvider;

    private SharedPreferences mPreferences;
    private boolean mShowButtonsAsLine;

    private QuickActionsDialog(@NonNull Context context, SharedPreferences preferences, boolean showButtonsAsLine) {
        super(Colors.createThemedContext(
                context,
                showButtonsAsLine
                        ? R.style.AppTheme_QuickActionsDialogLine
                        : R.style.AppTheme_QuickActionsDialogPad
        ));

        this.mPreferences = preferences;
        this.mShowButtonsAsLine = showButtonsAsLine;
    }

    public static QuickActionsDialog newInstance(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean showButtonsAsLine = preferences.getBoolean(context.getString(R.string.tile_dialog_line_design_key), false);

        return new QuickActionsDialog(context, preferences, showButtonsAsLine);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                mShowButtonsAsLine
                        ? R.layout.quick_actions_dialog_line
                        : R.layout.quick_actions_dialog_pad
        );

        mServiceStateProvider = new RotationServiceStateProvider(getContext(), this);

        for (ActionButton button : ActionButton.values()) {
            ImageView view = findViewById(button.viewId());
            view.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        final Context context = getContext();

        ActionButton button = ActionButton.fromViewId(view.getId());
        if (button == null) {
            return;
        }

        Intent intent = null;

        if (button == ActionButton.GUARD) {
            intent = RotationService.newToggleGuardIntent(context);
        } else if (button == ActionButton.REFRESH) {
            intent = RotationService.newRefreshModeIntent(context);
        } else if (button == ActionButton.POWER) {
            intent = RotationService.newStartIfStoppedOrStopIfStartedIntent(context);
        } else {
            RotationMode newMode = button.rotationMode();

            if (newMode != null) {
                intent = RotationService.newChangeModeIntent(context, newMode);
            }
        }

        if (intent == null) {
            return;
        }

        context.startForegroundService(intent);

        if (shouldCloseOnClick()) {
            cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mServiceStateProvider.bind();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mServiceStateProvider.unbind();
    }

    @Override
    public void onRotationData(RotationServiceStateProvider.State state) {
        updateViews(state.isRunning(), state.getActiveMode(), state.isGuardOrForced());
    }

    public void updateViews(boolean isServiceRunning, RotationMode activeMode, boolean guard) {
        final Context context = getContext();

        TextView infoView = findViewById(R.id.info);
        infoView.setTextColor(Colors.getActive(context));
        infoView.setVisibility(isServiceRunning ? View.GONE : View.VISIBLE);

        Set<String> enabledButtons = mPreferences.getStringSet(context.getString(R.string.tile_buttons_key), null);
        Set<Integer> enabledLineIds = new HashSet<>();

        int activeColor = Colors.getActive(context);
        int inactiveColor = Colors.getInactive(context);

        for (ActionButton button : ActionButton.values()) {
            ImageView view = findViewById(button.viewId());

            if (enabledButtons != null && !enabledButtons.contains(button.name())) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
                enabledLineIds.add(button.lineId());
            }

            int color = button.isActive(activeMode, guard, isServiceRunning)
                    ? activeColor
                    : inactiveColor;

            view.setColorFilter(color);
        }

        boolean swapButtonDirection = mPreferences.getBoolean(context.getString(R.string.tile_swap_buttons_direction_key), false);

        if (mShowButtonsAsLine) {
            LinearLayout root = findViewById(R.id.line);
            setButtonDirection(root, swapButtonDirection);
        } else {
            setButtonDirection(enabledLineIds, R.id.line_1, swapButtonDirection);
            setButtonDirection(enabledLineIds, R.id.line_2, swapButtonDirection);
            setButtonDirection(enabledLineIds, R.id.line_3, swapButtonDirection);
            setButtonDirection(enabledLineIds, R.id.line_4, swapButtonDirection);
        }
    }

    private void setButtonDirection(Set<Integer> enabledLineIds, @IdRes int lineId, boolean swap) {
        LinearLayout line = findViewById(lineId);

        if (enabledLineIds.contains(lineId)) {
            line.setVisibility(View.VISIBLE);
            setButtonDirection(line, swap);
        } else {
            line.setVisibility(View.GONE);
        }
    }

    private void setButtonDirection(LinearLayout line, boolean swap) {
        line.setLayoutDirection(swap ? LinearLayout.LAYOUT_DIRECTION_RTL : LinearLayout.LAYOUT_DIRECTION_LTR);
    }

    public boolean shouldCloseOnClick() {
        final Context context = getContext();

        return mPreferences.getBoolean(context.getString(R.string.close_dialog_on_click_key), true);
    }

}