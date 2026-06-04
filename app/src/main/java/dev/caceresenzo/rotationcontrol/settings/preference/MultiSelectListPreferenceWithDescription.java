package dev.caceresenzo.rotationcontrol.settings.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import dev.caceresenzo.rotationcontrol.R;

public class MultiSelectListPreferenceWithDescription extends MultiSelectListPreference {

    private CharSequence[] mEntryDescriptions;
    private int[] mEntryIcons;

    public MultiSelectListPreferenceWithDescription(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceWithDescription);
        mEntryDescriptions = array.getTextArray(R.styleable.ListPreferenceWithDescription_entryDescriptions);

        int resourceId = array.getResourceId(R.styleable.ListPreferenceWithDescription_entryIcons, 0);
        if (resourceId != 0) {
            TypedArray icons = context.getResources().obtainTypedArray(resourceId);

            mEntryIcons = new int[icons.length()];
            for (int index = 0; index < icons.length(); index++) {
                mEntryIcons[index] = icons.getResourceId(index, 0);
            }

            icons.recycle();
        }

        array.recycle();
    }

    public CharSequence[] getEntryDescriptions() {
        return mEntryDescriptions;
    }

    public int[] getEntryIcons() {
        return mEntryIcons;
    }

    public static class DialogFragment extends PreferenceDialogFragmentCompat {

        private static final String SAVE_STATE_VALUES = "MultiSelectListPreferenceWithDescription.values";
        private static final String SAVE_STATE_CHANGED = "MultiSelectListPreferenceWithDescription.changed";
        private static final String SAVE_STATE_ENTRIES = "MultiSelectListPreferenceWithDescription.entries";
        private static final String SAVE_STATE_ENTRY_DESCRIPTIONS = "MultiSelectListPreferenceWithDescription.entryDescriptions";
        private static final String SAVE_STATE_ENTRY_ICONS = "MultiSelectListPreferenceWithDescription.entryIcons";
        private static final String SAVE_STATE_ENTRY_VALUES = "MultiSelectListPreferenceWithDescription.entryValues";

        private Set<String> mNewValues = new HashSet<>();
        private boolean mPreferenceChanged = false;
        private CharSequence[] mEntries;
        private CharSequence[] mEntryDescriptions;
        private int[] mEntryIcons;
        private CharSequence[] mEntryValues;

        @NonNull
        public static DialogFragment newInstance(String key) {
            DialogFragment fragment = new DialogFragment();
            Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                MultiSelectListPreferenceWithDescription preference = getMultiSelectListPreferencePreference();

                if (preference.getEntries() == null || preference.getEntryDescriptions() == null || preference.getEntryValues() == null) {
                    throw new IllegalStateException("MultiSelectListPreferenceWithDescription requires an entries array, an entryDescriptions array and an entryValues array.");
                }

                mNewValues.clear();
                mNewValues.addAll(preference.getValues());
                mPreferenceChanged = false;
                mEntries = preference.getEntries();
                mEntryDescriptions = preference.getEntryDescriptions();
                mEntryIcons = preference.getEntryIcons();
                mEntryValues = preference.getEntryValues();
            } else {
                mNewValues.clear();
                mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES));
                mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false);
                mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
                mEntryDescriptions = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_DESCRIPTIONS);
                mEntryIcons = savedInstanceState.getIntArray(SAVE_STATE_ENTRY_ICONS);
                mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
            }
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putStringArrayList(SAVE_STATE_VALUES, new ArrayList<>(mNewValues));
            outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged);
            outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
            outState.putCharSequenceArray(SAVE_STATE_ENTRY_DESCRIPTIONS, mEntryDescriptions);
            outState.putIntArray(SAVE_STATE_ENTRY_ICONS, mEntryIcons);
            outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
        }

        private MultiSelectListPreferenceWithDescription getMultiSelectListPreferencePreference() {
            return (MultiSelectListPreferenceWithDescription) super.getPreference();
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult && mPreferenceChanged) {
                MultiSelectListPreferenceWithDescription preference = getMultiSelectListPreferencePreference();
                if (preference.callChangeListener(mNewValues)) {
                    preference.setValues(mNewValues);
                }
            }
        }

        @Override
        protected View onCreateDialogView(@NonNull Context context) {
            ScrollView scroll = new ScrollView(context);
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(0, 40, 0, 20);

            LayoutInflater inflater = LayoutInflater.from(context);

            for (int index = 0; index < mEntries.length; index++) {
                View item = inflater.inflate(R.layout.preference_multi_select_list_item_with_description, layout, false);

                ((TextView) item.findViewById(R.id.title)).setText(mEntries[index]);

                TextView descriptionTextView = item.findViewById(R.id.description);
                CharSequence description = mEntryDescriptions[index];
                if (description != null && description.length() > 0) {
                    descriptionTextView.setVisibility(View.VISIBLE);
                    descriptionTextView.setText(description);
                } else {
                    descriptionTextView.setVisibility(View.GONE);
                }

                ImageView iconImageView = item.findViewById(R.id.image);
                int iconResourceId = mEntryIcons[index];
                if (iconResourceId != 0) {
                    iconImageView.setVisibility(View.VISIBLE);
                    iconImageView.setImageDrawable(AppCompatResources.getDrawable(context, iconResourceId));
                } else {
                    iconImageView.setVisibility(View.GONE);
                }

                CheckBox checkBox = item.findViewById(R.id.checkbox);
                checkBox.setChecked(mNewValues.contains(mEntryValues[index].toString()));

                final String value = mEntryValues[index].toString();
                item.setOnClickListener((view) -> {
                    mPreferenceChanged = true;
                    if (mNewValues.contains(value)) {
                        mNewValues.remove(value);
                        checkBox.setChecked(false);
                    } else {
                        mNewValues.add(value);
                        checkBox.setChecked(true);
                    }
                });

                layout.addView(item);
            }

            scroll.addView(layout);
            return scroll;
        }

        @Override
        protected void onBindDialogView(@NonNull View view) {
        }

    }

}