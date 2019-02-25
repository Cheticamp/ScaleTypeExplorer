package com.example.android.scaletypeexplorer;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getFragmentManager().findFragmentByTag(PREF_FRAGMENT_TAG) == null) {
            getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MainPreferenceFragment(), PREF_FRAGMENT_TAG)
                .commit();
        }
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        private ListPreference mPrefScaleType;
        private EditTextPreference mPrefWidthPercentage;
        private EditTextPreference mPrefHeightPercentage;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            mPrefScaleType = (ListPreference) findPreference(PREF_SCALE_TYPE);
            mPrefWidthPercentage =
                (EditTextPreference) findPreference(PREF_WIDTH_PERCENTAGE);
            mPrefHeightPercentage =
                (EditTextPreference) findPreference(PREF_HEIGHT_PERCENTAGE);
            bindPreferenceSummaryToValue(mPrefScaleType);
            bindPreferenceSummaryToValue(mPrefWidthPercentage);
            bindPreferenceSummaryToValue(mPrefHeightPercentage);

            // Percentages apply only to the matrix scale type. Ensure that percentage are between
            // 0.0 and 100.0 inclusive. Don't allow more than one decimal place.
            InputFilter minMaxFilter[] = {new InputFilter() {
                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dstart, int dend) {
                    String s = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.length());
                    s = s.substring(0, dstart) + source.toString() + s.substring(dstart);
                    int decimalPos = s.indexOf(".");
                    if (decimalPos != -1 && s.length() - decimalPos > 2) {
                        return "";
                    }
                    Float value;
                    try {
                        value = Float.valueOf(s);
                    } catch (NumberFormatException e) {
                        return "";
                    }
                    if (value > 100.0f) {
                        return "";
                    }
                    return null;
                }
            }};
            mPrefWidthPercentage.getEditText().setFilters(minMaxFilter);
            mPrefHeightPercentage.getEditText().setFilters(minMaxFilter);
        }

        EditTextPreference getPrefWidthPercentage() {
            return mPrefWidthPercentage;
        }

        EditTextPreference getPrefHeightPercentage() {
            return mPrefHeightPercentage;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener
            .onPreferenceChange(preference,
                                PreferenceManager
                                    .getDefaultSharedPreferences(preference.getContext())
                                    .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String stringValue = newValue.toString();

                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

                    // Enabled percentages for the matrix scale type only.
                    if (preference.getKey().equals(PREF_SCALE_TYPE)) {
                        MainPreferenceFragment frag =
                            (MainPreferenceFragment) ((Activity) preference.getContext())
                                .getFragmentManager().findFragmentByTag(PREF_FRAGMENT_TAG);
                        if (newValue.equals("MATRIX")) {
                            frag.getPrefWidthPercentage().setEnabled(true);
                            frag.getPrefHeightPercentage().setEnabled(true);
                        } else {
                            frag.getPrefWidthPercentage().setEnabled(false);
                            frag.getPrefHeightPercentage().setEnabled(false);
                        }
                    }
                } else {
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

    @SuppressWarnings("unused")
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final String PREF_FRAGMENT_TAG = "PREF_FRAG";

    // Preference file keys. If these change, preferences.xml may also need to change.
    public static final String PREF_SCALE_TYPE_MATRIX = "MATRIX";
    public static final String PREF_SCALE_TYPE_DEFAULT = PREF_SCALE_TYPE_MATRIX;
    public static final String PREF_SCALE_TYPE = "pref_scale_type";
    public static final String PREF_WIDTH_PERCENTAGE = "pref_width_percentage";
    public static final String PREF_WIDTH_PERCENTAGE_DEFAULT = "20";
    public static final String PREF_HEIGHT_PERCENTAGE = "pref_height_percentage";
    public static final String PREF_HEIGHT_PERCENTAGE_DEFAULT = "20";

}
