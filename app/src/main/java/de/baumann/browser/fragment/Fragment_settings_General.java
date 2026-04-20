package de.baumann.browser.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.preferences.BasePreferenceFragment;
import de.baumann.browser.view.NinjaToast;

public class Fragment_settings_General extends BasePreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState,String rootKey) {
        setPreferencesFromResource(R.xml.preference_general, rootKey);
        initSummary(getPreferenceScreen());
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        Context context = getContext();
        assert context != null;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean customSE = sp.getBoolean("searchEngineSwitch", false);

        ListPreference searchEngines;
        searchEngines= findPreference("sp_search_engine");
        assert searchEngines != null;
        String customSearchEngine = sp.getString("sp_search_engine_custom", "");
        String text = getString(R.string.setting_title_searchEngine) + ": " + getString(R.string.toast_input_empty);

        if(customSE) {
            searchEngines.setEnabled(false);
            if (customSearchEngine.isEmpty()) {
                NinjaToast.show(context, text);
            }
        } else {
            searchEngines.setEnabled(true);
        }

        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (Objects.requireNonNull(p.getTitle()).toString().toLowerCase().contains("password")) {
                p.setSummary("******");
            } else {
                if (p.getSummaryProvider() == null) p.setSummary(editTextPref.getText());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    }
}
