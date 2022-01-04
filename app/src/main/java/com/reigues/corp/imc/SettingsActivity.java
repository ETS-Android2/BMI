package com.reigues.corp.imc;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import static android.content.pm.PackageManager.GET_META_DATA;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            int label = getPackageManager().getActivityInfo(getComponentName(), GET_META_DATA).labelRes;
            if (label != 0) {
                setTitle(label);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String languageToLoad = sharedPref.getString("key_language", "-1");
        //String languageToLoad = "en";
        if (!languageToLoad.equals("-1")) {
            //String language = SharedPrefUtils.getSavedLanguage(); // Helper method to get saved language from SharedPreferences
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return updateResourcesLocale(context, locale);
            }

            return updateResourcesLocaleLegacy(context, locale);

        }

        return context;

    }

    @TargetApi(Build.VERSION_CODES.N)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        SharedPreferences sharedPref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            sharedPref = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));

            ListPreference defaultWeightUnitPreference = findPreference("key_default_weight_unit");
            ListPreference defaultHeightUnitPreference = findPreference("key_default_height_unit");
            ListPreference addButtonPlacementPreference = findPreference("key_add_button_placement");
            ListPreference languagePreference = findPreference("key_language");

            if (defaultWeightUnitPreference != null) {
                defaultWeightUnitPreference.setOnPreferenceChangeListener(this);
                changeTitle(defaultWeightUnitPreference);
            }
            if (defaultHeightUnitPreference != null) {
                defaultHeightUnitPreference.setOnPreferenceChangeListener(this);
                changeTitle(defaultHeightUnitPreference);
            }
            if (addButtonPlacementPreference != null) {
                addButtonPlacementPreference.setOnPreferenceChangeListener(this);
                changeTitle(addButtonPlacementPreference);
            }
            if (languagePreference != null) {
                languagePreference.setOnPreferenceChangeListener(this);
                changeTitle(languagePreference);
            }

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String stringValue = newValue.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                changeTitle(listPreference, index);
                if (listPreference.getKey().equals("key_language")) {
                    String languageToLoad = getResources().getStringArray(R.array.pref_default_language_values)[index];
                    Locale locale = new Locale(languageToLoad);
                    Locale.setDefault(locale);
                    Configuration config = new Configuration();
                    config.locale = locale;
                    getResources().updateConfiguration(config, getResources().getDisplayMetrics());

                    //Objects.requireNonNull(getActivity()).finish();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    //Objects.requireNonNull(getActivity()).onBackPressed();
                }
            }
            return true;
        }

        private void changeTitle(ListPreference listPreference, int indexValue) {
            if (indexValue >= 0) {
                CharSequence entry = null;
                String title = null;
                switch (listPreference.getKey()) {
                    case "key_default_weight_unit":
                        title = getString(R.string.pref_title_default_weight_unit);
                        entry = getResources().getStringArray(R.array.pref_default_weight_unit_entries)[indexValue];
                        break;
                    case "key_default_height_unit":
                        title = getString(R.string.pref_title_default_height_unit);
                        entry = getResources().getStringArray(R.array.pref_default_height_unit_entries)[indexValue];
                        break;
                    case "key_add_button_placement":
                        title = getString(R.string.pref_title_add_button_placement);
                        entry = getResources().getStringArray(R.array.pref_add_button_placement_entries)[indexValue];
                        break;
                    case "key_language":
                        title = getString(R.string.pref_title_language);
                        entry = getResources().getStringArray(R.array.pref_default_language_entries)[indexValue];
                        break;
                }
                listPreference.setTitle(title + " : " + entry);
            }
        }

        private void changeTitle(ListPreference listPreference) {
            if (listPreference.getKey().equals("key_language")) {
                Locale entry = new Locale(sharedPref.getString("key_language", Locale.getDefault().toString()));
                if (entry.getISO3Language().equals("fra")) {
                    changeTitle(listPreference, 1);
                    listPreference.setValueIndex(1);
                } else {
                    changeTitle(listPreference, 0);
                    listPreference.setValueIndex(0);
                }


            } else {
                changeTitle(listPreference, Integer.parseInt(sharedPref.getString(listPreference.getKey(), "0")));
            }
        }

    }
}