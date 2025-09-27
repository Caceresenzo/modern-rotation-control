package dev.caceresenzo.rotationcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public class PresetsActivity extends AppCompatActivity {

    public static final List<String> IGNORED_PREFIXES = Arrays.asList(
            "com.android.systemui"
    );

    private ApplicationListAdapter adapter;
    private List<ApplicationInfo> allApplications;
    private List<ApplicationInfo> filteredApplications;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presets_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        loadInstalledApplications();

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ApplicationListAdapter(filteredApplications, new ApplicationListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ApplicationInfo applicationInfo) {
                showModeDialog(applicationInfo);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.presets_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Search applications...");
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
        allApplications = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0);
        
        for (ResolveInfo resolveInfo : resolveInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            String displayName = resolveInfo.loadLabel(packageManager).toString();
            Drawable icon = resolveInfo.loadIcon(packageManager);
            RotationMode currentMode = getCurrentMode(packageName);

            ApplicationInfo application = new ApplicationInfo(packageName, displayName, icon, currentMode);

            allApplications.add(application);
        }

        Collections.sort(allApplications);
        filteredApplications = new ArrayList<>(allApplications);
    }

    private RotationMode getCurrentMode(String packageName) {
        String key = getApplicationKey(packageName);
        String value = sharedPreferences.getString(key, null);

        return RotationMode.valueOf(value, null);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyFilter(String query) {
        filteredApplications.clear();

        if (query.isEmpty()) {
            filteredApplications.addAll(allApplications);
        } else {
            query = query.toLowerCase();

            for (ApplicationInfo application : allApplications) {
                if (application.getDisplayName().toLowerCase().contains(query)) {
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Mode for " + application.getDisplayName());
        builder.setSingleChoiceItems(items, selectedIndex, null);
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            if (selected >= 0) {
                RotationMode selectedMode = values[selected];
                updateAppMode(application, selectedMode);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateAppMode(ApplicationInfo application, RotationMode newMode) {
        String packageName = application.getPackageName();

        String key = getApplicationKey(packageName);
        if (newMode != null) {
            sharedPreferences.edit().putString(key, newMode.toString()).apply();
        } else {
            sharedPreferences.edit().remove(key).apply();
        }

        application.setCurrentMode(newMode);

        for (int index = 0; index < filteredApplications.size(); index++) {
            if (filteredApplications.get(index).getPackageName().equals(packageName)) {
                adapter.notifyItemChanged(index);
                break;
            }
        }
    }

    @NonNull
    public static String getApplicationKey(String packageName) {
        return "presets/" + packageName + "/mode";
    }

}

@Data
@AllArgsConstructor
class ApplicationInfo implements Comparable<ApplicationInfo> {

    private String packageName;
    private String displayName;
    private Drawable icon;
    private @Nullable RotationMode currentMode;

    @Override
    public int compareTo(ApplicationInfo other) {
        return displayName.compareToIgnoreCase(other.displayName);
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

        public ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            displayName = itemView.findViewById(R.id.display_name);
            currentModeText = itemView.findViewById(R.id.current_mode_text);
            currentModeIcon = itemView.findViewById(R.id.current_mode_icon);
        }

        public void bind(ApplicationInfo application, OnItemClickListener listener) {
            icon.setImageDrawable(application.getIcon());
            displayName.setText(application.getDisplayName());

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