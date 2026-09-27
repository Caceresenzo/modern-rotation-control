package dev.caceresenzo.rotationcontrol.oss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.caceresenzo.rotationcontrol.R;

public final class OssLicenseListAdapter extends RecyclerView.Adapter<OssLicenseListAdapter.ViewHolder> {

    private final List<OssLicense> licenses;
    private final OnItemClickListener onItemClickListener;

    public OssLicenseListAdapter(List<OssLicense> licenses, OnItemClickListener onItemClickListener) {
        this.licenses = licenses;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.oss_license_item, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OssLicense license = licenses.get(position);

        holder.bind(license, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return licenses.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;

        ViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
        }

        public void bind(OssLicense license, OnItemClickListener onLicenseClickListener) {
            name.setText(license.getLibraryName());
            itemView.setOnClickListener((view) -> onLicenseClickListener.onItemClick(license));
        }

    }

    public interface OnItemClickListener {
        void onItemClick(OssLicense license);
    }

}