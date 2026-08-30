package dev.caceresenzo.rotationcontrol.tile;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.rotation.RotationMode;
import dev.caceresenzo.rotationcontrol.rotation.RotationService;
import dev.caceresenzo.rotationcontrol.settings.ActionButton;

public class QuickActionsDialog extends Dialog implements View.OnClickListener {

    private final Connection mConnection = new Connection();
    private final Listener mListener = new Listener();

    private boolean mShouldUnbind = false;
    private RotationService mService;

    public QuickActionsDialog(@NonNull Context context) {
        super(new ContextThemeWrapper(context, R.style.AppTheme_QuickActionsDialog));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.quick_actions_dialog);

        for (ActionButton button : ActionButton.values()) {
            ImageView view = findViewById(button.viewId());
            view.setOnClickListener(this);
        }

        boolean isServiceRunning = RotationService.isRunning(getApplicationContext());

        TextView infoView = findViewById(R.id.info);
        if (isServiceRunning) {
            infoView.setVisibility(View.GONE);
        }

        updateViews(false, null, isServiceRunning);
    }

    @Override
    public void onClick(View view) {
        final Context context = getApplicationContext();

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

        final Context context = getApplicationContext();

        Intent intent = new Intent(context, RotationService.class);
        mShouldUnbind = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(RotationService.ACTION_NOTIFY_UPDATED);
        ContextCompat.registerReceiver(context, mListener, filter, ContextCompat.RECEIVER_EXPORTED);
    }

    private void onServiceConnected() {
        RotationMode activeMode = mService.getActiveMode();
        boolean guard = mService.isGuardEnabledOrForced();
        boolean isServiceRunning = mService.isRunning();

        updateViews(guard, activeMode, isServiceRunning);
    }

    @Override
    protected void onStop() {
        super.onStop();

        final Context context = getApplicationContext();

        if (mShouldUnbind) {
            mShouldUnbind = false;

            context.unbindService(mConnection);
            mService = null;
        }

        context.unregisterReceiver(mListener);
    }

    public void updateViews(boolean guard, RotationMode activeMode, boolean isServiceRunning) {
        final Context context = getApplicationContext();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isDifferentAsNotification = preferences.getBoolean(context.getString(R.string.tile_different_buttons_as_notification_key), false);
        String buttonsKey = context.getString(isDifferentAsNotification ? R.string.tile_buttons_key : R.string.notification_buttons_key);

        Set<String> enabledButtons = preferences.getStringSet(buttonsKey, null);
        Set<Integer> enabledLineIds = new HashSet<>();

        for (ActionButton button : ActionButton.values()) {
            ImageView view = findViewById(button.viewId());

            if (enabledButtons != null && !enabledButtons.contains(button.name())) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
                enabledLineIds.add(button.lineId());
            }

            setActiveColor(context, view, button.isActive(activeMode, guard, isServiceRunning));
        }

        boolean swapButtonDirection = preferences.getBoolean(context.getString(R.string.tile_swap_buttons_direction_key), false);
        setButtonDirection(enabledLineIds, R.id.line_1, swapButtonDirection);
        setButtonDirection(enabledLineIds, R.id.line_2, swapButtonDirection);
        setButtonDirection(enabledLineIds, R.id.line_3, swapButtonDirection);
        setButtonDirection(enabledLineIds, R.id.line_4, swapButtonDirection);
    }

    private void setButtonDirection(Set<Integer> enabledLineIds, @IdRes int lineId, boolean swap) {
        LinearLayout line = findViewById(lineId);

        if (enabledLineIds.contains(lineId)) {
            line.setVisibility(View.VISIBLE);
            line.setLayoutDirection(swap ? LinearLayout.LAYOUT_DIRECTION_RTL : LinearLayout.LAYOUT_DIRECTION_LTR);
        } else {
            line.setVisibility(View.GONE);
        }
    }

    private void setActiveColor(Context context, ImageView view, boolean active) {
        if (active) {
            view.setColorFilter(context.getColor(R.color.active));
        } else {
            view.setColorFilter(context.getColor(R.color.inactive));
        }
    }

    public boolean shouldCloseOnClick() {
        final Context context = getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return preferences.getBoolean(context.getString(R.string.close_dialog_on_click_key), true);
    }

    public Context getApplicationContext() {
        return getContext().getApplicationContext();
    }

    /* https://developer.android.com/reference/android/app/Service#:~:text=With%20that%20done%2C%20one%20can%20now%20write%20client%20code%20that%20directly%20accesses%20the%20running%20service%2C%20such%20as%3A */
    public class Connection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            RotationService.LocalBinder binder = (RotationService.LocalBinder) service;
            mService = binder.getService();

            QuickActionsDialog.this.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }

    }

    public class Listener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            if (!RotationService.ACTION_NOTIFY_UPDATED.equals(action)) {
                return;
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean guard = preferences.getBoolean(context.getString(R.string.guard_key), false);
            RotationMode activeMode = RotationMode.fromPreferences(context);
            boolean isServiceRunning = RotationService.isRunning(context);

            updateViews(guard, activeMode, isServiceRunning);
        }
    }

}