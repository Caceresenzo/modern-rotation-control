package dev.caceresenzo.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class QuickActionsDialogActivity extends AppCompatActivity implements View.OnClickListener, ServiceConnection {

    private final Listener mListener = new Listener();

    private RotationService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFinishOnTouchOutside(true);
        getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);

        setContentView(R.layout.quick_actions_dialog);

        for (RotationMode mode : RotationMode.values()) {
            ImageView view = findViewById(mode.viewId());
            view.setOnClickListener(this);
        }

        ImageView guardView = findViewById(R.id.guard);
        guardView.setOnClickListener(this);

        updateViews(false, null);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.guard) {
            startService(RotationService.newToggleGuardIntent(this));
            finishAndRemoveTask();
            return;
        }

        RotationMode newMode = RotationMode.fromViewId(viewId);
        if (newMode != null) {
            startService(RotationService.newChangeModeIntent(this, newMode));
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, RotationService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(RotationService.ACTION_NOTIFY_UPDATED);
        ContextCompat.registerReceiver(this, mListener, filter, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(this);
        unregisterReceiver(mListener);
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> enabledButtons = preferences.getStringSet(getString(R.string.buttons_key), null);
        for (RotationMode mode : RotationMode.values()) {
            ImageView view = findViewById(mode.viewId());

            if (enabledButtons != null && !enabledButtons.contains(mode.name())) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
            }

            setActiveColor(view, mode == activeMode);
        }

        ImageView guardView = findViewById(R.id.guard);
        setActiveColor(guardView, guard);
    }

    public void setActiveColor(ImageView view, boolean active) {
        if (active) {
            view.setColorFilter(getColor(R.color.active));
        } else {
            view.setColorFilter(getColor(R.color.inactive));
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

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(QuickActionsDialogActivity.this);
            boolean guard = preferences.getBoolean(getString(R.string.guard_key), false);
            RotationMode activeMode = RotationMode.valueOf(preferences.getString(getString(R.string.mode_key), RotationMode.AUTO.name()));

            updateViews(guard, activeMode);
        }
    }

}