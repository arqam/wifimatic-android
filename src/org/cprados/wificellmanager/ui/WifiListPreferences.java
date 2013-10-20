/*
 * WifiListPreferences.java
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

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.cprados.wificellmanager.DataManager;
import org.cprados.wificellmanager.ManagerService;
import org.cprados.wificellmanager.R;
import org.cprados.wificellmanager.StateMachine.StateAction;
import org.cprados.wificellmanager.StateMachine.StateEvent;
import org.cprados.wificellmanager.sys.CellStateManager;
import org.cprados.wificellmanager.sys.NotificationManager;
import org.cprados.wificellmanager.sys.WifiStateManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;

/**
 * Main wifi cell manager program Activity based on PreferencesActivity. Handles
 * program management options, start and stop, and permits to monitor program
 * status
 */
public class WifiListPreferences extends PreferenceActivity implements OnPreferenceClickListener, OnClickListener, OnItemLongClickListener {

    /** Action to be sent in a broadcast to ask this activity to refresh */
    private static final String REFRESH_UI_ACTION = WifiListPreferences.class.getPackage().getName() + ".refresh_ui";
    
    /** Ordinal of the first wifi preference on this activity */
    private static final int FIRST_WIFI_PREFERENCE = 1;
            
    /** Broadcast receiver that receives refresh requests for this activity */
    private BroadcastReceiver mRefreshReceiver = null;

    /** Service binder for synchronous communication with Manager Service */
    private ManagerService.ManagerServiceBinder mManagerServiceBinder = null;
    
    /** Service connection for synchronous communication with Manager Service */
    private ServiceConnection mManagerServiceConn;
    
    /** Edit bar view */
    private View mEditBarView;

    /** Activity creation callback */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_list_preferences);

        
        // Adds the edit bar layout
        LayoutInflater inflater = getLayoutInflater();
        if (inflater != null) {
            mEditBarView = inflater.inflate(R.layout.wifi_list_edit_bar_layout, null);
            if (mEditBarView != null) {
                ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                ViewParent parent = getListView().getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).addView(mEditBarView, params);
                }
            }
        }        
    }
    
    /** Activity resume callback */
    @Override
    protected void onResume() {
        super.onResume(); 
        
        // Cleans status bar
        NotificationManager.removeNotification(this);
        
        // Registers button click callbacks
        findViewById(R.id.button_enable_wifis).setOnClickListener(this);
        findViewById(R.id.button_delete_wifis).setOnClickListener(this);

        // Registers refresh UI receiver callback
        registerReceiver(mRefreshReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                refreshUI();
            }
        }, new IntentFilter(REFRESH_UI_ACTION));
        
        // Creates a service connection for synchronous communication with manager service
        if (DataManager.getActivate(this)) {            
            // Bind to Manager Service
            Intent intent = new Intent(this, ManagerService.class);
            getApplicationContext().bindService(intent, mManagerServiceConn = new ServiceConnection () {
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    mManagerServiceBinder = (ManagerService.ManagerServiceBinder) binder;                    
                }
                public void onServiceDisconnected(ComponentName name) {
                    mManagerServiceBinder = null;                    
                }                
            }, Context.BIND_AUTO_CREATE);
        }
        
        // Registers prefrences long click listener
        ListView listView = getListView();
        listView.setOnItemLongClickListener(this);

        // Refreshes UI preferences that may have changed
        refreshUI();        
    }

    /** Activity pause callback */
    @Override
    protected void onPause() {
        super.onPause();
        
        // Set edit mode
        DataManager.setEditMode(this, false);

        // Unregister refresh receiver callback
        if (mRefreshReceiver != null) {
            unregisterReceiver(mRefreshReceiver);
            mRefreshReceiver = null;
        }
        
        // Destroys manager service connection 
        if (mManagerServiceConn != null) {
            getApplicationContext().unbindService(mManagerServiceConn);
            mManagerServiceConn = null;
        }
    }
    
    /** Activity close callback */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEditBarView = null;
    }

    /** Handler for preferences click event */
    @Override
    public boolean onPreferenceClick(Preference pref) {

        // A wifi preference was clicked
        if (pref.getKey().startsWith(DataManager.PREFERENCE_WIFI)) {
            
            // In edit mode refresh buttons bar
            if (DataManager.getEditMode(this)) {
                // When nothing selected to edit 
                if (!refreshWifiPreferences(false)) {
                    // Set edit mode off
                    DataManager.setEditMode(this, false);

                    // Do a refresh of the UI
                    refreshWifiPreferences(true);
                }
            }
            
            // In not edit mode
            else {
                // Start the preferences activity for clicked wifi
                String wifi = pref.getKey().substring(DataManager.PREFERENCE_WIFI.length());
                Intent intent = (new Intent(this, WifiPreferences.class)).putExtra(WifiPreferences.EXTRA_ACTIVITY_WIFI_NAME, wifi);
                startActivity(intent);
            }
        }

        return true;
    }

    /** Handler for long click event on any item of the activity */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        boolean result = false;

        // Check if edit mode is on and app is active
        if (!DataManager.getEditMode(this) && DataManager.getActivate(this)) {

            // Set edit mode
            DataManager.setEditMode(this, true);

            // Get the position of the preference selected
            int idWifiPref = position - FIRST_WIFI_PREFERENCE;
            
            if (idWifiPref >= 0) {

                // Get the selected wifi 
                PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_WIFIS);
                Preference pref = cat.getPreference(idWifiPref);
                String key = pref.getKey();
                String wifi = key.substring(DataManager.PREFERENCE_WIFI.length());

                // Mark the selected wifi
                DataManager.setWifiSelected(this, wifi, true);

                // Do a refresh of the UI
                refreshWifiPreferences(true);
                result = true;
            }
        }

        return result;
    }
    
    public void onBackPressed() {        
        // Check if edit mode is on
        if (DataManager.getEditMode(this)) {
            
            // Set edit mode off
            DataManager.setEditMode(this, false);

            // Do a refresh of the UI
            refreshWifiPreferences(true);
        }
        else {
            super.onBackPressed();
        }
    }
    
    /** Handles click event over this activity view buttons */
    public void onClick(View view) {

        switch (view.getId()) {

        case R.id.button_enable_wifis:
            
            if (view instanceof Button) {
                boolean enable = ((Button)view).getText().equals(getResources().getString(R.string.button_title_enable_wifi));
            
                // enable/disable selected wifis preferences
                enableSelectedWifisPreferences(enable);
           
                // Spawn cell refresh event to the manager service
                ManagerService.forwardEvent(this, CellStateManager.CELL_CHANGE_ACTION, null);                        
            }
            break;
        
        case R.id.button_delete_wifis:
            // Delete selected wifis preferences
            deleteSelectedWifisPreferences();
           
            // Spawn cell refresh event to the manager service
            ManagerService.forwardEvent(this, CellStateManager.CELL_CHANGE_ACTION, null);            
            break;            
        }
        
        // Set edit mode to false
        DataManager.setEditMode(this, false);

        // Refresh wifi preferences and buttons bar
        refreshWifiPreferences(true);      
    }
        
    /** Refresh Preferences activity UI */
    private void refreshUI() {

        // Refresh wifi preferences list and buttons bar
        refreshWifiPreferences(true);        
    }

    /** Dynamically creates preferences UI objects of each wifi stored and shows delete button if required */
    private boolean refreshWifiPreferences(boolean refreshAll) {

        boolean active = DataManager.getActivate(this);
        boolean editMode = DataManager.getEditMode(this) && active;        
        Set<String> currentCellWifis = null;
        if (refreshAll && DataManager.getCurrentCellEnabled(this)) {
            currentCellWifis = DataManager.getWifisOfCurrentCell(this);
        }
        
        // Remove all preferences if refreshing the whole list
        PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_WIFIS);
        if (refreshAll) {
            cat.removeAll();
        }
        
        // Retrieves the set of wifis
        Set<String> wifis = DataManager.getAllWifis(this);
        int numWifis = wifis.size();
        boolean enableButtons = false;
        boolean toggleEnableWifi = true;

        // Iterates over wifi preferences
        int pos = 0;
        for (Iterator<String> iterator = wifis.iterator(); iterator.hasNext();pos++) {
            String wifi = iterator.next();            
                        
            // Button have to be shown if any wifi preference is marked as selected
            boolean selected = DataManager.getWifiSelected(this, wifi);
            enableButtons = selected || enableButtons;
            
            // Enable wifi button has to be shown if all marked items are disabled
            boolean enabledMark = DataManager.getWifiEnabled(this, wifi);
            toggleEnableWifi = ((selected && !enabledMark) || !selected ) && toggleEnableWifi;
                        
            // Add wifi preference if refreshing list is required
            if (refreshAll) {
                boolean currentWifi = wifi.equals(DataManager.getCurrentWifi(this));
                boolean currentCell = (currentCellWifis != null) && currentCellWifis.contains(wifi);
                addWifiPreference(cat, wifi, pos, numWifis, editMode, currentCell, currentWifi, enabledMark, active);
            }
        }
        
        // Make sure buttons are only enabled in edit mode and active
        enableButtons &= editMode &&  active;

        // Shows or hides the buttons bar
        showButtonsBar(enableButtons, toggleEnableWifi);
        
        return enableButtons;
    }

    /** Dynamically creates a wifi preference UI object */
    private void addWifiPreference(PreferenceCategory cat, String wifi, int position, int numWifis,
            boolean editMode, boolean isCurrentCell, boolean isCurrentWifi, boolean enabledMark, boolean active) {

        Preference pref = null;
        int color = getColorStripe (active && enabledMark, isCurrentWifi, isCurrentCell);
        
        // Check box preference
        if (editMode) {
            pref = new MyCheckBoxPreference(this);
            ((MyCheckBoxPreference) pref).setColorStripe(color);                            
            ((MyCheckBoxPreference) pref).setGrey(!enabledMark);
        }

        // Icon preference
        else { 
            pref = new IconPreference(this); 

            ((IconPreference) pref).setColorStripe(color);
            ((IconPreference) pref).setBold(isCurrentWifi);
            if (isCurrentCell) {
                ((IconPreference) pref).setMyIcon(getResources().getDrawable(R.drawable.ic_preference_wifi_grey));
            }
        }
        
        // Set preference layout
        pref.setLayoutResource(R.layout.preference);
        
        // Sets key, title, and persistence
        pref.setKey(DataManager.PREFERENCE_WIFI + wifi);
        pref.setTitle(wifi);
        pref.setPersistent(true);
        pref.setEnabled((enabledMark || editMode) && active);                
             
        // Set the position in the list
        if (!enabledMark)
            position += numWifis * 3;
        else if (!isCurrentCell)
            position += numWifis * 2;
        else if (!isCurrentWifi)
            position += numWifis;
        pref.setOrder(position);

        // Setup the summary
        if (enabledMark) {
            Vector<int[]> cells = DataManager.getCellsbyWifi(this, wifi);
            //int numCells = cells.size();
            int numCells = DataManager.getCountCellsEnabled(this, cells);
            String summary = getResources().getQuantityString(R.plurals.preference_summary_wifi, numCells, numCells);
            pref.setSummary(summary);
        }
        else {
            pref.setSummary(R.string.preference_summary_wifi_disabled);
        }

        // Setup callbacks
        pref.setOnPreferenceClickListener(this);

        // Add the preference
        cat.addPreference(pref);        
    }
    
    private int getColorStripe (boolean drawStripe, boolean isCurrentWifi, boolean isCurrentCell) {
        int result = 0;

        if (drawStripe) {
            
            if (isCurrentWifi) {
                result = getResources().getColor(R.color.color_audit_trail_green);
            }
            else if (isCurrentCell) {
                result = getResources().getColor(R.color.color_audit_trail_yellow);                    
            }
            else {
                result = getResources().getColor(R.color.color_audit_trail_red);                                        
            }
        }        
        return result;
    }

    /** Dynamically shows or hides the edit mode buttons bar and set properties of the buttons */    
    private void showButtonsBar(boolean show, boolean toggleEnableWifi) {

        if (mEditBarView != null) {
            mEditBarView.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show) {
                View buttonEnable = findViewById(R.id.button_enable_wifis);
                if (buttonEnable != null && buttonEnable instanceof Button) {
                    if (toggleEnableWifi) {
                        ((Button) buttonEnable).setText(R.string.button_title_enable_wifi);
                    }
                    else {
                        ((Button) buttonEnable).setText(R.string.button_title_disable_wifi);
                    }
                }
            }
        }
    }

    /** Removes all wifi preferences UI objects that are selected by user and hides delete button */
    private void deleteSelectedWifisPreferences() {

        PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_WIFIS);

        if (cat != null) {

            // Gets current wifi if any
            String currentWifi = DataManager.getCurrentWifi(this);

            // Remove wifi preferences with value set true
            int numWifis = cat.getPreferenceCount();
            for (int i = 0; i < numWifis; i++) {

                Preference pref = cat.getPreference(i);
                if (pref != null && pref instanceof CheckBoxPreference && ((CheckBoxPreference) pref).isChecked()) {

                    String key = pref.getKey();
                    String wifi = DataManager.getWifiOfWifiPreference(key);

                    if (wifi != null) {
                        // Deletes the wifi preference from the list
                        deleteWifiPreference(cat, key, wifi, wifi.equals(currentWifi));
                        
                        // Restores loop values
                        i--;
                        numWifis--;
                    }
                }
            }
        }
    }

    /** Removes a wifi preference UI object */
    private void deleteWifiPreference(PreferenceCategory cat, String key, String wifi, boolean isCurrentWifi) {

        // Remove the UI object preference
        Preference pref = cat.findPreference(key);
        cat.removePreference(pref);

        // Disconnects wifi before current wifi is deleted
        if (isCurrentWifi && DataManager.getWifiAction(this, StateAction.OFF, wifi)) {

            if (DataManager.getActivate(this)) {
                // Disconnects 
                WifiStateManager.setWifiState(this, StateEvent.DISC, null);

                // Synchronously forward wifi state change event to the manager service
                if (mManagerServiceBinder != null)
                    mManagerServiceBinder.syncForwardEvent(this, WifiManager.NETWORK_STATE_CHANGED_ACTION, null);
            }
        }

        // Remove the stored preference and all wificell associations of this wifi
        DataManager.deleteWifiCells(this, wifi);
    }

    /** Set a value to all wifi preferences UI objects that are selected by user */
    private void enableSelectedWifisPreferences(boolean value) {

        PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_WIFIS);
        if (cat != null) {

            // Remove wifi preferences with value set true
            int numWifiCells = cat.getPreferenceCount();
            for (int i = 0; i < numWifiCells; i++) {

                Preference pref = cat.getPreference(i);
                if (pref != null && pref instanceof CheckBoxPreference && ((CheckBoxPreference) pref).isChecked()) {

                    String key = pref.getKey();
                    String wifi = DataManager.getWifiOfWifiPreference(key);

                    if (wifi != null) {
                        // Bulk set all wifi actions to the wifi
                        DataManager.setWifiEnabled(this, wifi, value);

                        // Unmark the wifi preference
                        DataManager.setWifiSelected(this, wifi, false);
                    }
                }
            }
        }
    }
}
