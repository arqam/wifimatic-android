/*
 * TimeIntervalPreferences.java
 * This file is part of WifiCellManager.
 * Copyright (C) 2012 Carlos Prados <wifimatic.app@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cprados.wificellmanager.ui;

import org.cprados.wificellmanager.DataManager;
import org.cprados.wificellmanager.ManagerService;
import org.cprados.wificellmanager.R;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

/** 
 * Preferences Activity to handle configuration of disable wifi time interval
 */
public class TimeIntervalPreferences extends PreferenceActivity implements OnPreferenceChangeListener {
        
    /** Tag for logging this class messages */
    private static final String LOGTAG = TimeIntervalPreferences.class.getPackage().getName();
    
    /** Activity creation callback */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.time_interval_preferences);
        //setContentView(R.layout.preferences_layout);
    }  
        
    /** Activity resume callback */
    @Override
    protected void onResume() {
        super.onResume();
        
        // Dynamically sets this activity preferences enabled or dissabled
        setEnabled(DataManager.getActivate(this));
        
        // Registers preferences change callbacks
        PreferenceScreen screen = getPreferenceScreen();
        
        Preference timeIntervalEnabled = screen.findPreference(DataManager.PREFERENCE_TIME_INTERVAL);
        if (timeIntervalEnabled != null) {
            timeIntervalEnabled.setOnPreferenceChangeListener(this);
        }

        Preference timeIntervalBegin = screen.findPreference(DataManager.PREFERENCE_TIME_INTERVAL_BEGIN);
        if (timeIntervalBegin != null) {
            timeIntervalBegin.setOnPreferenceChangeListener(this);
        }
        
        Preference timeIntervalEnd = screen.findPreference(DataManager.PREFERENCE_TIME_INTERVAL_END);
        if (timeIntervalEnd != null) {
            timeIntervalEnd.setOnPreferenceChangeListener(this);
        }
    }
        
    /** Handler for preferences change event */
    public boolean onPreferenceChange(Preference pref, Object newValue) {

        boolean restart = false;
        
        // Time interval preference changed
        if (pref.getKey().equals(DataManager.PREFERENCE_TIME_INTERVAL)
                && Boolean.parseBoolean(newValue.toString()) != DataManager.getTimeIntervalEnabled(this)) {
            restart = true;
        }
        
        else if (pref.getKey().equals(DataManager.PREFERENCE_TIME_INTERVAL_BEGIN) && 
                !compareTime(newValue.toString(), DataManager.getTimeIntervalBegin(this))) {
            restart = true;
        }
        
        else if (pref.getKey().equals(DataManager.PREFERENCE_TIME_INTERVAL_END) && 
                !compareTime(newValue.toString(), DataManager.getTimeIntervalEnd(this))) {
            restart = true;
        }
        
        if (restart) {            
            // Restarts the service
            Intent intent = new Intent(this.getApplicationContext(), ManagerService.class);
            stopService(intent);
            startService(intent);
        }

        return true;
    }
    
    private boolean compareTime (String timeString, int[] time) {
        
        boolean result = false;
        int [] values = new int[2];
        try {
            String[] timeParts=timeString.split(DataManager.TIME_SEPARATOR);
            values[0] = Integer.parseInt(timeParts[0]); 
            values[1] = Integer.parseInt(timeParts[1]);            
            result = (values[0] == time[0]) && (values[1] == time[1]);
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        
        return result;        
    }
    
    /** Enables or disables options depending on the app status */
    private void setEnabled (boolean enable) {

        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            
            // Enables or disables main scheduler activation checkbox preference
            Preference timeIntervalPref = screen.findPreference(DataManager.PREFERENCE_TIME_INTERVAL);
            if (timeIntervalPref != null) {
                timeIntervalPref.setEnabled(enable);
            }
        }
    }
}
