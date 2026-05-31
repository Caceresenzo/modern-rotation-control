package dev.caceresenzo.rotationcontrol.settings.preference;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public abstract class CustomPreferenceFragmentCompat extends PreferenceFragmentCompat {

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ListPreferenceWithDescription) {
            ListPreferenceWithDescription.DialogFragment dialog =
                    ListPreferenceWithDescription.DialogFragment.newInstance(preference.getKey());
            dialog.setTargetFragment(this, 0);
            dialog.show(getParentFragmentManager(), "ListPreferenceWithDescription");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

}