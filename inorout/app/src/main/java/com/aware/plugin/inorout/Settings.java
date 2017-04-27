package com.aware.plugin.inorout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

//Settings is copy from AWARE template
public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String STATUS_PLUGIN_INOROUT = "status_plugin_inorout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        syncSettings();
    }

    private void syncSettings() {
        CheckBoxPreference status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_INOROUT);
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_INOROUT).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = (Preference) findPreference(key);

        if( setting.getKey().equals(STATUS_PLUGIN_INOROUT) ) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(getApplicationContext(), key, is_active);
            if( is_active ) {
                Aware.startPlugin(getApplicationContext(), getPackageName());
            } else {
                Aware.stopPlugin(getApplicationContext(), getPackageName());
            }
        }

        Intent refresh = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(refresh);
    }
}