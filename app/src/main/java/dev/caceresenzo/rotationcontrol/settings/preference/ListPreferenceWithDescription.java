package dev.caceresenzo.rotationcontrol.settings.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import dev.caceresenzo.rotationcontrol.R;

public class ListPreferenceWithDescription extends ListPreference {

    private CharSequence[] mEntryDescriptions;
    private int[] mEntryIcons;

    public ListPreferenceWithDescription(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceWithDescription);
        this.mEntryDescriptions = array.getTextArray(dev.caceresenzo.rotationcontrol.R.styleable.ListPreferenceWithDescription_entryDescriptions);

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

        private static final String SAVE_STATE_INDEX = "ListPreferenceWithDescription.DialogFragment.index";
        private static final String SAVE_STATE_ENTRIES = "ListPreferenceWithDescription.DialogFragment.entries";
        private static final String SAVE_STATE_ENTRY_DESCRIPTIONS = "ListPreferenceWithDescription.DialogFragment.entryDescriptions";
        private static final String SAVE_STATE_ENTRY_ICONS = "ListPreferenceWithDescription.entryIcons";
        private static final String SAVE_STATE_ENTRY_VALUES = "ListPreferenceWithDescription.DialogFragment.entryValues";

        int mClickedDialogEntryIndex;
        private CharSequence[] mEntries;
        private CharSequence[] mEntryDescriptions;
        private int[] mEntryIcons;
        private CharSequence[] mEntryValues;

        @androidx.annotation.NonNull
        public static DialogFragment newInstance(String key) {
            final DialogFragment fragment = new DialogFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                final ListPreferenceWithDescription preference = getListPreference();

                if (preference.getEntries() == null || preference.getEntryDescriptions() == null || preference.getEntryValues() == null) {
                    throw new IllegalStateException("ListPreferenceWithDescription requires an entries array, an entryDescriptions array and an entryValues array.");
                }

                mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
                mEntries = preference.getEntries();
                mEntryDescriptions = preference.getEntryDescriptions();
                mEntryIcons = preference.getEntryIcons();
                mEntryValues = preference.getEntryValues();
            } else {
                mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0);
                mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
                mEntryDescriptions = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_DESCRIPTIONS);
                mEntryIcons = savedInstanceState.getIntArray(SAVE_STATE_ENTRY_ICONS);
                mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
            }
        }

        @Override
        public void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex);
            outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
            outState.putCharSequenceArray(SAVE_STATE_ENTRY_DESCRIPTIONS, mEntryDescriptions);
            outState.putIntArray(SAVE_STATE_ENTRY_ICONS, mEntryIcons);
            outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
        }

        private ListPreferenceWithDescription getListPreference() {
            return (ListPreferenceWithDescription) getPreference();
        }

        @Override
        protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);

            // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
            // dialog instead of the user having to press 'Ok'.
            builder.setPositiveButton(null, null);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult && mClickedDialogEntryIndex >= 0) {
                String value = mEntryValues[mClickedDialogEntryIndex].toString();
                final ListPreference preference = getListPreference();
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
            }
        }

        @Override
        protected View onCreateDialogView(@NonNull Context context) {
            ScrollView scroll = new ScrollView(context);
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(0, 40, 0, 20);

            List<RadioButton> radioButtons = new ArrayList<>(mEntries.length);

            LayoutInflater inflater = LayoutInflater.from(context);
            for (int index = 0; index < mEntries.length; index++) {
                View item = inflater.inflate(R.layout.preference_list_item_with_description, layout, false);

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
                int iconResourceId = mEntryIcons == null ? 0 : mEntryIcons[index];
                if (iconResourceId != 0) {
                    iconImageView.setVisibility(View.VISIBLE);
                    iconImageView.setImageDrawable(AppCompatResources.getDrawable(context, iconResourceId));
                } else {
                    iconImageView.setVisibility(View.GONE);
                }

                RadioButton radio = item.findViewById(R.id.radio);
                radioButtons.add(radio);

                final int theIndex = index;
                item.setOnClickListener(view -> {
                    mClickedDialogEntryIndex = theIndex;
                    updateChecked(radioButtons);

                    Dialog dialog = getDialog();
                    onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    dialog.dismiss();
                });

                layout.addView(item);
            }

            updateChecked(radioButtons);

            scroll.addView(layout);
            return scroll;
        }

        private void updateChecked(List<RadioButton> radioButtons) {
            for (int index = 0; index < radioButtons.size(); index++) {
                radioButtons.get(index).setChecked(index == mClickedDialogEntryIndex);
            }
        }

        @Override
        protected void onBindDialogView(@NonNull View view) {
        }

    }

}