package dev.caceresenzo.rotationcontrol.settings.preset;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationInfo implements Comparable<ApplicationInfo> {

    private String packageName;
    private @Nullable String displayName;
    private Drawable icon;
    private PresetRotationMode currentMode;

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