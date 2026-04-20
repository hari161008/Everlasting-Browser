package de.baumann.browser.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.activity.Settings_ProfileList;
import de.baumann.browser.activity.Settings_Profile;
import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.preferences.BasePreferenceFragment;

public class Fragment_settings_Privacy extends BasePreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_privacy, rootKey);
        Context context = getContext();
        assert context != null;
        initSummary(getPreferenceScreen());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        Preference sp_ad_block = findPreference("sp_ad_block");
        assert sp_ad_block != null;
        sp_ad_block.setSummary(getString(R.string.setting_summary_adblock) + "\n\n" + AdBlock.getHostsDate(getContext()));

        Preference settings_profile = findPreference("settings_profile");
        assert settings_profile != null;
        settings_profile.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_Profile.class);
            requireActivity().startActivity(intent);
            return false;
        });
        Preference edit_standard = findPreference("edit_standard");
        assert edit_standard != null;
        edit_standard.setOnPreferenceClickListener(preference -> {
            sp.edit().putString("listToLoad", "standard").apply();
            Intent intent = new Intent(getActivity(), Settings_ProfileList.class);
            requireActivity().startActivity(intent);
            return false;
        });
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
        assert key != null;
        if (key.equals("sp_ad_block") || key.equals("ab_hosts") || key.equals("custom_adblock")) {
            AdBlock.downloadHosts(getActivity());
        }
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
