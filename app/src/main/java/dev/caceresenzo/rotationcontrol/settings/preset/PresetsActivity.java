package dev.caceresenzo.rotationcontrol.settings.preset;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.caceresenzo.rotationcontrol.R;
import dev.caceresenzo.rotationcontrol.settings.RotationSharedPreferences;
import dev.caceresenzo.rotationcontrol.util.Queries;

public class PresetsActivity extends AppCompatActivity {

    public static final List<String> KEEP_CURRENT_BY_DEFAULT = Arrays.asList(
            "com.android.settings"
    );

    public static final List<String> IGNORED_PREFIXES = Arrays.asList(
            "com.android.systemui"
    );

    private ProgressBar progressBar;
    private ApplicationListAdapter adapter;
    private List<ApplicationInfo> allApplications;
    private List<ApplicationInfo> filteredApplications;
    private RotationSharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presets_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.list);
        progressBar = findViewById(R.id.progress_bar);

        preferences = RotationSharedPreferences.from(this);

        if (!preferences.hasPresetsBeenUsed()) {
            applyDefaultKeepCurrentModes(false);
        }

        preferences.markPresetsAsUsed();

        allApplications = new ArrayList<>();
        filteredApplications = new ArrayList<>();

        loadInstalledApplications();

        adapter = new ApplicationListAdapter(filteredApplications, new ApplicationListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ApplicationInfo applicationInfo) {
                showModeDialog(applicationInfo);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public Set<String> getKeepCurrentPackageNames() {
        Set<String> packageNames = new HashSet<>();

        packageNames.addAll(Queries.getAllLauncherPackageNames(this));
        packageNames.addAll(Queries.getAllInputMethodPackageNames(this));
        packageNames.addAll(KEEP_CURRENT_BY_DEFAULT);

        return packageNames;
    }

    public void applyDefaultKeepCurrentModes(boolean showToast) {
        for (String packageName : getKeepCurrentPackageNames()) {
            preferences.setApplicationMode(packageName, PresetRotationMode.KEEP_CURRENT);
        }

        if (showToast) {
            Toast.makeText(this, R.string.presets_defaults_applied, Toast.LENGTH_SHORT).show();
        }

        if (adapter != null) {
            loadInstalledApplications();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.presets_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint(getString(R.string.presets_search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilter(newText);
                return true;
            }

        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        if (id == R.id.action_apply_defaults) {
            applyDefaultKeepCurrentModes(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadInstalledApplications() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                allApplications.clear();
                filteredApplications.clear();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                });

                PackageManager packageManager = getPackageManager();

                List<android.content.pm.ApplicationInfo> resolveInfoList = packageManager.getInstalledApplications(0);
                for (android.content.pm.ApplicationInfo resolveInfo : resolveInfoList) {
                    String packageName = resolveInfo.packageName;
                    String displayName = resolveInfo.loadLabel(packageManager).toString();
                    Drawable icon = resolveInfo.loadIcon(packageManager);
                    PresetRotationMode currentMode = preferences.getApplicationMode(packageName);

                    if (displayName.equals(packageName)) {
                        displayName = null;
                    }

                    ApplicationInfo application = new ApplicationInfo(packageName, displayName, icon, currentMode);
                    allApplications.add(application);
                }

                Collections.sort(allApplications);
                filteredApplications.addAll(allApplications);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                });

                executor.shutdown();
            }
        });

        executor.shutdown();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyFilter(String query) {
        filteredApplications.clear();

        if (query.isEmpty()) {
            filteredApplications.addAll(allApplications);
        } else {
            query = query.toLowerCase();

            for (ApplicationInfo application : allApplications) {
                String displayName = application.getDisplayName();

                boolean matchDisplayName = displayName != null && displayName.toLowerCase().contains(query);
                boolean matchPackageName = application.getPackageName().contains(query);

                if (matchDisplayName || matchPackageName) {
                    filteredApplications.add(application);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showModeDialog(ApplicationInfo application) {
        PresetRotationMode[] values = PresetRotationMode.values();

        String[] items = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            items[index] = getString(values[index].stringId());
        }

        PresetRotationMode currentMode = application.getCurrentMode();

        int selectedIndex = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index] == currentMode) {
                selectedIndex = index;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.presets_change_title, application.getDisplayName()))
                .setSingleChoiceItems(items, selectedIndex, null)
                .setPositiveButton(R.string.presets_change_positive, (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    PresetRotationMode selectedMode = values[selected];
                    updateApplicationMode(application, selectedMode);
                })
                .setNegativeButton(R.string.presets_change_negative, null)
                .show();
    }

    private void updateApplicationMode(ApplicationInfo application, PresetRotationMode newMode) {
        String packageName = application.getPackageName();

        preferences.setApplicationMode(packageName, newMode);

        application.setCurrentMode(newMode);

        for (int index = 0; index < filteredApplications.size(); index++) {
            if (filteredApplications.get(index).getPackageName().equals(packageName)) {
                adapter.notifyItemChanged(index);
                break;
            }
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, PresetsActivity.class);
        context.startActivity(intent);
    }

}