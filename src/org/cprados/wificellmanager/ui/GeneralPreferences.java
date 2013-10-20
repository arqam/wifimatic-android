/*
 * GeneralPreferences.java
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
import org.cprados.wificellmanager.StateMachine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * Preferences activity to handle program management options, 
 * start and stop, and permits to monitor program status
 */
public class GeneralPreferences extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {

    /** Action to be sent in a broadcast to ask this activity to refresh */
    private static final String REFRESH_UI_ACTION = GeneralPreferences.class.getPackage().getName() + ".refresh_ui";
            
    /** Broadcast receiver that receives refresh requests for this activity */
    private BroadcastReceiver mRefreshReceiver = null; 

    /** Activity creation callback */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preferences);
    }

    /** Activity resume callback */
    @Override
    protected void onResume() {
        super.onResume();
        
        // Registers preferences click and change callbacks
        PreferenceScreen screen = getPreferenceScreen();
        screen.findPreference(DataManager.PREFERENCE_ACTIVATE).setOnPreferenceClickListener(this);
        screen.findPreference(DataManager.PREFERENCE_FREQ).setOnPreferenceChangeListener(this);
        screen.findPreference(DataManager.PREFERENCE_ADVANCED).setOnPreferenceClickListener(this);
        
        // Registers refresh receiver callback
        registerReceiver(mRefreshReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                refreshUI();
            }
        }, new IntentFilter(REFRESH_UI_ACTION));
        
        // Refreshes UI preferences that may have changed
        refreshUI();        
    }

    /** Activity pause callback */
    @Override
    protected void onPause() {
        super.onPause();

        // Unregister refresh receiver callback
        if (mRefreshReceiver != null) {
            unregisterReceiver(mRefreshReceiver);
            mRefreshReceiver = null;
        }
    }

    /** Handler for preferences click event */
    public boolean onPreferenceClick(Preference pref) {

        // Activate preference clicked
        if (pref.getKey().equals(DataManager.PREFERENCE_ACTIVATE)) {
            Intent intent = new Intent(this.getApplicationContext(), ManagerService.class);
            
            // Activate the service requesting init actions
            if (DataManager.getActivate(this)) {                
                startService(intent.setAction(ManagerService.INIT_ACTION));
                
                // Disable welcome dialog
                DataManager.setWizardStep(this, Dialogs.WelcomeDialogBuilder.FINISHED_STEP);
            }
            else {
                // Deactivate the service
                stopService(intent);
                                
                // Refreshes the UI
                refreshUI();
            }            
        }
        
        else if (pref.getKey().equals(DataManager.PREFERENCE_ADVANCED)) {

            // Start advanced preferences activity
            Intent intent = new Intent(this, AdvancedPreferences.class);
            startActivity(intent);            
        }

        return true;
    }

    /** Handler for preferences change event */
    public boolean onPreferenceChange(Preference pref, Object newValue) {

        // Check frequency preference changed
        if (pref.getKey().equals(DataManager.PREFERENCE_FREQ)) {

            // Service was running and frequency preference actually changed
            if (DataManager.getActivate(this) && (Integer.parseInt((String) newValue) != DataManager.getFrequency(this))) {

                // Restarts the service
                Intent intent = new Intent(this.getApplicationContext(), ManagerService.class);
                stopService(intent);
                startService(intent);
            }
        }

        return true;
    }
    
    /** Refresh Preferences activity UI */
    private void refreshUI() {

        // Refresh activate preference value and status message
        PreferenceScreen screen = getPreferenceScreen();
        CheckBoxPreference prefActivate = (CheckBoxPreference) screen.findPreference(DataManager.PREFERENCE_ACTIVATE);
        prefActivate.setSummary(getStatusMessage());
        prefActivate.setChecked(DataManager.getActivate(this));
    }
    
    /** Determines the status message of the application shown in UI */
    private String getStatusMessage() {

        String statusMessage = null;
        Resources res = getResources();
        StateMachine.State currentState = DataManager.getState(this);

        // State machine has not been initialized jet
        if (currentState == null) {
            // System is not initialized jet
            statusMessage = res.getString(R.string.preference_summary_activate);
        }
        // Get current state description
        else {
            statusMessage = currentState.getDescription(res,DataManager.getNumWifisOfCurrentCell(this),DataManager.getCurrentWifi(this));
        }
        
        return statusMessage;
    }    
}
