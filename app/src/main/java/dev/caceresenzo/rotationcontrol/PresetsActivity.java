package dev.caceresenzo.rotationcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public class PresetsActivity extends AppCompatActivity {

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
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadInstalledApplications() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                PackageManager packageManager = getPackageManager();

                Intent mainIntent = new Intent();
                List<android.content.pm.ApplicationInfo> resolveInfoList = packageManager.getInstalledApplications(0);

                for (android.content.pm.ApplicationInfo resolveInfo : resolveInfoList) {
                    String packageName = resolveInfo.packageName;
                    String displayName = resolveInfo.loadLabel(packageManager).toString();
                    Drawable icon = resolveInfo.loadIcon(packageManager);
                    RotationMode currentMode = preferences.getApplicationMode(packageName);

                    if (displayName.equals(packageName)) {
                        displayName = null;
                    }

                    ApplicationInfo application = new ApplicationInfo(packageName, displayName, icon, currentMode);

                    allApplications.add(application);
                }

                Collections.sort(allApplications);

                runOnUiThread(() -> {
                    filteredApplications.addAll(allApplications);
                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                });

                executor.shutdown();
            }
        });
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
        String[] items = {
                getString(R.string.mode_default),
                getString(R.string.mode_auto),
                getString(R.string.mode_portrait),
                getString(R.string.mode_portrait_reverse),
                getString(R.string.mode_portrait_sensor),
                getString(R.string.mode_landscape),
                getString(R.string.mode_landscape_reverse),
                getString(R.string.mode_landscape_sensor),
        };

        RotationMode[] values = {
                null,
                RotationMode.AUTO,
                RotationMode.PORTRAIT,
                RotationMode.PORTRAIT_REVERSE,
                RotationMode.PORTRAIT_SENSOR,
                RotationMode.LANDSCAPE,
                RotationMode.LANDSCAPE_REVERSE,
                RotationMode.LANDSCAPE_SENSOR,
        };

        RotationMode currentMode = application.getCurrentMode();

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
                    RotationMode selectedMode = values[selected];
                    updateApplicationMode(application, selectedMode);
                })
                .setNegativeButton(R.string.presets_change_negative, null)
                .show();
    }

    private void updateApplicationMode(ApplicationInfo application, RotationMode newMode) {
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

}

@Data
@AllArgsConstructor
class ApplicationInfo implements Comparable<ApplicationInfo> {

    private String packageName;
    private @Nullable String displayName;
    private Drawable icon;
    private @Nullable RotationMode currentMode;

    public boolean hasName() {
        return displayName != null;
    }

    @Override
    public int compareTo(ApplicationInfo other) {
        if (displayName != null && other.displayName == null) {
            return -1;
        }

        if (displayName == null && other.displayName != null) {
            return 1;
        }

        if (displayName != null) {
            return displayName.compareToIgnoreCase(other.displayName);
        }

        return packageName.compareTo(other.packageName);
    }

}

@RequiredArgsConstructor
class ApplicationListAdapter extends RecyclerView.Adapter<ApplicationListAdapter.ViewHolder> {

    private final List<ApplicationInfo> list;
    private final OnItemClickListener onItemClickListener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.presets_application, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo item = list.get(position);

        holder.bind(item, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView displayName;
        private final TextView currentModeText;
        private final ImageView currentModeIcon;
        private final TextView packageName;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            displayName = itemView.findViewById(R.id.display_name);
            currentModeText = itemView.findViewById(R.id.current_mode_text);
            currentModeIcon = itemView.findViewById(R.id.current_mode_icon);
            packageName = itemView.findViewById(R.id.package_name);
        }

        public void bind(ApplicationInfo application, OnItemClickListener listener) {
            icon.setImageDrawable(application.getIcon());

            if (application.hasName()) {
                displayName.setText(application.getDisplayName());
                packageName.setText(application.getPackageName());
            } else {
                displayName.setText(R.string.presets_application_no_name);
                packageName.setText(application.getPackageName());
            }

            RotationMode currentMode = application.getCurrentMode();
            if (currentMode == null) {
                currentModeText.setText(R.string.presets_mode_default);

                currentModeIcon.setImageResource(0);
                currentModeIcon.setVisibility(View.GONE);
            } else {
                Context context = currentModeText.getContext();
                currentModeText.setText(context.getString(R.string.presets_mode, context.getString(currentMode.stringId())));

                currentModeIcon.setImageResource(application.getCurrentMode().drawableId());
                currentModeIcon.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(application);
                }
            });
        }
    }

    interface OnItemClickListener {
        void onItemClick(ApplicationInfo applicationInfo);
    }

}