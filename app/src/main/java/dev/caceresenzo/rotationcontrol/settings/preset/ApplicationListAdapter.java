package dev.caceresenzo.rotationcontrol.settings.preset;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.caceresenzo.rotationcontrol.R;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApplicationListAdapter extends RecyclerView.Adapter<ApplicationListAdapter.ViewHolder> {

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

            PresetRotationMode currentMode = application.getCurrentMode();
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

    public interface OnItemClickListener {
        void onItemClick(ApplicationInfo applicationInfo);
    }

}