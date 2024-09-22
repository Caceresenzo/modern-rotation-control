package dev.caceresenzo.rotationcontrol;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class QuickActionsDialog extends Dialog implements View.OnClickListener, ServiceConnection {

    private final Listener mListener = new Listener();

    private RotationService mService;

    public QuickActionsDialog(@NonNull Context context) {
        super(new ContextThemeWrapper(context, R.style.AppTheme_QuickActionsDialog));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.quick_actions_dialog);

        for (RotationMode mode : RotationMode.values()) {
            ImageView view = findViewById(mode.viewId());
            view.setOnClickListener(this);
        }

        ImageView guardView = findViewById(R.id.guard);
        guardView.setOnClickListener(this);

        TextView infoView = findViewById(R.id.info);
        if (RotationService.isRunning(getContext())) {
            infoView.setVisibility(View.GONE);
        }

        updateViews(false, null);
    }

    @Override
    public void onClick(View view) {
        final Context context = getContext();

        Intent intent = null;

        int viewId = view.getId();
        if (viewId == R.id.guard) {
            intent = RotationService.newToggleGuardIntent(context);
        } else {
            RotationMode newMode = RotationMode.fromViewId(viewId);
            if (newMode != null) {
                intent = RotationService.newChangeModeIntent(context, newMode);
            }
        }

        if (intent == null) {
            return;
        }

        if (!RotationService.isRunning(context)) {
            RotationService.start(context);
        }

        context.startService(intent);

        if (shouldCloseOnClick()) {
            cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        final Context context = getContext();

        Intent intent = new Intent(context, RotationService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(RotationService.ACTION_NOTIFY_UPDATED);
        ContextCompat.registerReceiver(context, mListener, filter, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();

        final Context context = getContext();

        context.unbindService(this);
        context.unregisterReceiver(mListener);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        RotationService.LocalBinder binder = (RotationService.LocalBinder) service;
        mService = binder.getService();

        RotationMode activeMode = mService.getActiveMode();
        boolean guard = mService.isGuardEnabledOrForced();

        updateViews(guard, activeMode);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    public void updateViews(boolean guard, RotationMode activeMode) {
        final Context context = getContext();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> enabledButtons = preferences.getStringSet(context.getString(R.string.buttons_key), null);
        for (RotationMode mode : RotationMode.values()) {
            ImageView view = findViewById(mode.viewId());

            if (enabledButtons != null && !enabledButtons.contains(mode.name())) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
            }

            setActiveColor(context, view, mode == activeMode);
        }

        ImageView guardView = findViewById(R.id.guard);
        setActiveColor(context, guardView, guard);
    }

    private void setActiveColor(Context context, ImageView view, boolean active) {
        if (active) {
            view.setColorFilter(context.getColor(R.color.active));
        } else {
            view.setColorFilter(context.getColor(R.color.inactive));
        }
    }

    public boolean shouldCloseOnClick() {
        final Context context = getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return preferences.getBoolean(context.getString(R.string.close_dialog_on_click_key), true);
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
            RotationMode activeMode = RotationMode.valueOf(preferences.getString(context.getString(R.string.mode_key), RotationMode.AUTO.name()));

            updateViews(guard, activeMode);
        }
    }

}