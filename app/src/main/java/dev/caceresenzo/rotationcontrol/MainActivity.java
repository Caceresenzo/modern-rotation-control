package dev.caceresenzo.rotationcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private final ActivityResultLauncher<Intent> permissionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> checkPermissions(true)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        if (checkPermissions(true)) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean shouldStart = sharedPreferences.getBoolean(getString(R.string.start_control_key), false);

            if (shouldStart) {
                RotationService.start(this);
            }
        }
    }

    public boolean checkPermissions(boolean request) {
        Uri uri = Uri.parse("package:" + getPackageName());

        if (!Settings.System.canWrite(this)) {
            if (request) {
                Log.i(TAG, "Cannot yet write settings");
                Toast.makeText(this, R.string.require_settings_write_permission, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, uri);
                permissionActivityResultLauncher.launch(intent);
            }

            return false;
        }

        if (!Settings.canDrawOverlays(this)) {
            if (request) {
                Log.i(TAG, "Cannot yet draw overlays");
                Toast.makeText(this, R.string.require_overlay_permission, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                permissionActivityResultLauncher.launch(intent);
            }

            return false;
        }

        return true;
    }

}