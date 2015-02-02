/*
 * AdvancedPreferences.java
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
import org.cprados.wificellmanager.R;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/** 
 * Preferences Activity to handle advanced configuration options
 */
public class AdvancedPreferences extends PreferenceActivity implements OnPreferenceChangeListener {
    
    /** Id of the dialog to confirm screen on preference */
    private static final int DIALOG_CONFIRM_SCREEN_ON = 0;
    
    /** Id of the dialog to confirm force update preference */
    private static final int DIALOG_CONFIRM_FORCE_UPDATE = 1;
    
    /** Id of the dialog to confirm disconnection timeout preference */
    private static final int DIALOG_CONFIRM_DISC_TIMEOUT = 2;
    
    /** Id of the dialog to confirm mobile data management */
    private static final int DIALOG_CONFIRM_MOBILE_DATA_MANAGED = 3;

    /** Id of the dialog to confirm mobile data management */
    private static final int DIALOG_UNK_LOCATION_ACTIVATES_WIFI = 4;
    
    /** Activity creation callback */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(org.cprados.wificellmanager.R.xml.advanced_preferences);                
    }           
    
    /** Activity resume callback */
    @Override
    protected void onResume() {
        super.onResume();

        // Registers preferences click and change callbacks
        PreferenceScreen screen = getPreferenceScreen();
        screen.findPreference(DataManager.PREFERENCE_TURN_ON_SCREEN).setOnPreferenceChangeListener(this);
        screen.findPreference(DataManager.PREFERENCE_FORCE_UPDATE_LOCATION).setOnPreferenceChangeListener(this);
        screen.findPreference(DataManager.PREFERENCE_OFF_AFTER_DISC_TIMEOUT).setOnPreferenceChangeListener(this);
        screen.findPreference(DataManager.PREFERENCE_MOBILE_DATA_MANAGED).setOnPreferenceChangeListener(this);
        screen.findPreference(DataManager.PREFERENCE_UNK_LOCATION_ACTIVATES_WIFI).setOnPreferenceChangeListener(this);
    }
    
    /**
     * Handler for this Activity dialogs creation
     */
    protected Dialog onCreateDialog(int id) {

        Dialog result = null;

        switch (id) { 
        case DIALOG_CONFIRM_SCREEN_ON:

            result = (new Dialogs.ConfirmPreferenceDialogBuilder(this, 
                    R.string.dialog_text_turn_on_screen, 
                    findPreference(DataManager.PREFERENCE_TURN_ON_SCREEN), 
                    Boolean.valueOf(false))).create();
            break;

        case DIALOG_CONFIRM_FORCE_UPDATE:

            result = (new Dialogs.ConfirmPreferenceDialogBuilder(this,
                    R.string.dialog_text_force_update_location, 
                    findPreference(DataManager.PREFERENCE_FORCE_UPDATE_LOCATION), 
                    Boolean.valueOf(false))).create();
            break;
            
        case DIALOG_CONFIRM_DISC_TIMEOUT:
            result = (new Dialogs.ConfirmPreferenceDialogBuilder(this,
                    R.string.dialog_text_off_after_disc_timeout, 
                    findPreference(DataManager.PREFERENCE_OFF_AFTER_DISC_TIMEOUT), 
                    String.valueOf(DataManager.PREFERENCE_DEFAULT_OFF_AFTER_DISC_TIMEOUT))).create();                   
            break;

        case DIALOG_CONFIRM_MOBILE_DATA_MANAGED:

            result = (new Dialogs.ConfirmPreferenceDialogBuilder(this, 
                    R.string.dialog_text_mobile_data_managed, 
                    findPreference(DataManager.PREFERENCE_MOBILE_DATA_MANAGED), 
                    Boolean.valueOf(false))).create();
            break;
            
        case DIALOG_UNK_LOCATION_ACTIVATES_WIFI:

            result = (new Dialogs.ConfirmPreferenceDialogBuilder(this, 
                    R.string.dialog_text_unk_location_activates_wifi,
                    findPreference(DataManager.PREFERENCE_UNK_LOCATION_ACTIVATES_WIFI), 
                    Boolean.valueOf(false))).create();
            break;
        }

        return result;
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        // Turn on screen preference changed
        if (preference.getKey().equals(DataManager.PREFERENCE_TURN_ON_SCREEN)) {
            if (newValue != null && Boolean.parseBoolean(newValue.toString())) {
                showDialog(DIALOG_CONFIRM_SCREEN_ON);
            }
        }

        // Force update location preference changed
        else if (preference.getKey().equals(DataManager.PREFERENCE_FORCE_UPDATE_LOCATION)) {
            if (newValue != null && Boolean.parseBoolean(newValue.toString())) {
                showDialog(DIALOG_CONFIRM_FORCE_UPDATE);
            }
        }
        
        else if (preference.getKey().equals(DataManager.PREFERENCE_OFF_AFTER_DISC_TIMEOUT)) {
            if (newValue != null && !newValue.toString().equals(String.valueOf(DataManager.PREFERENCE_DEFAULT_OFF_AFTER_DISC_TIMEOUT))) {
                showDialog(DIALOG_CONFIRM_DISC_TIMEOUT);
            }            
        }

        else if (preference.getKey().equals(DataManager.PREFERENCE_MOBILE_DATA_MANAGED)) {
            if (newValue != null && Boolean.parseBoolean(newValue.toString())) {
                showDialog(DIALOG_CONFIRM_MOBILE_DATA_MANAGED);
            }
        }
        
        else if (preference.getKey().equals(DataManager.PREFERENCE_UNK_LOCATION_ACTIVATES_WIFI)) {
            if (newValue != null && Boolean.parseBoolean(newValue.toString())) {
                showDialog(DIALOG_UNK_LOCATION_ACTIVATES_WIFI);
            }
        }

        return true;
    }
}
