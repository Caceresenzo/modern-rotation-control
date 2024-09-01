package dev.caceresenzo.rotationcontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class QuickActionsDialogActivity extends AppCompatActivity implements ServiceConnection {

    private RotationService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFinishOnTouchOutside(true);

        Window window = getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        window.requestFeature(Window.FEATURE_NO_TITLE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        setContentView(R.layout.quick_actions_dialog);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> enabledButtons = preferences.getStringSet(getString(R.string.buttons_key), null);
        for (RotationMode mode : RotationMode.values()) {
            ImageView view = findViewById(mode.viewId());

            if (enabledButtons != null && !enabledButtons.contains(mode.name())) {
                view.setVisibility(View.GONE);
            }

            view.setColorFilter(getColor(R.color.inactive));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    preferences.edit()
                            .putString(getString(R.string.mode_key), mode.name())
                            .apply();

                    RotationService.notifyConfigurationChanged(QuickActionsDialogActivity.this);
                    finishAndRemoveTask();
                }
            });
        }

        ImageView guardView = findViewById(R.id.guard);
        guardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean guard = preferences.getBoolean(getString(R.string.guard_key), false);

                preferences.edit()
                        .putBoolean(getString(R.string.guard_key), !guard)
                        .apply();

                RotationService.notifyConfigurationChanged(QuickActionsDialogActivity.this);
                finishAndRemoveTask();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, RotationService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        RotationService.LocalBinder binder = (RotationService.LocalBinder) service;
        mService = binder.getService();

        RotationMode activeMode = mService.getActiveMode();
        ImageView activeView = findViewById(activeMode.viewId());
        activeView.setColorFilter(getColor(R.color.active));

        ImageView guardView = findViewById(R.id.guard);
        if (mService.isGuardEnabledOrForced()) {
            guardView.setColorFilter(getColor(R.color.active));
        } else {
            guardView.setColorFilter(getColor(R.color.inactive));
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

}