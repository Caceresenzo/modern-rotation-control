package dev.caceresenzo.rotationcontrol;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String GITHUB_URL = "https://github.com/Caceresenzo/modern-rotation-control";

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

            getSupportFragmentManager().addOnBackStackChangedListener(() -> {
                int count = getSupportFragmentManager().getBackStackEntryCount();
                actionBar.setDisplayHomeAsUpEnabled(count > 0);

                if (count == 0) {
                    setTitle(R.string.app_name);
                }
            });
        }

        if (checkPermissions(true)) {
            RotationSharedPreferences preferences = RotationSharedPreferences.from(this);

            if (preferences.shouldStartControl()) {
                RotationService.start(this);
            }
        }
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference preference) {
        Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(
                        getClassLoader(),
                        preference.getFragment()
                );

        fragment.setArguments(preference.getExtras());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();

        setTitle(preference.getTitle());

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        if (id == R.id.action_github) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(GITHUB_URL));

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, R.string.no_browser_found, Toast.LENGTH_SHORT).show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean checkPermissions(boolean request) {
        Uri uri = Uri.parse("package:" + getPackageName());

        if (!Permissions.canWriteSettings(this)) {
            if (request) {
                Log.i(TAG, "Cannot yet write settings");
                Toast.makeText(this, R.string.require_settings_write_permission, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, uri);
                permissionActivityResultLauncher.launch(intent);
            }

            return false;
        }

        if (!Permissions.canDrawOverlays(this)) {
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