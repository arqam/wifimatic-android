/*
 * WifiPreferences.java
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
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;

/** 
 * Preferences Activity to handle configuration that is particular to a specific Wifi 
 * Wifi name has to be passed as an extra to the preferences activity 
 */
public class WifiPreferences extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener, OnItemLongClickListener, OnClickListener {

    /** Name of the wifi this instance of the activity configures */
    public static String EXTRA_ACTIVITY_WIFI_NAME = WifiPreferences.class.getName() + ".activity_wifi_name";
    
    /** Ordinal of the first wifi-cell preference on this activity */
    private static final int FIRST_WIFI_CELL_PREFERENCE = 5;
    
    /** Action to be sent in a broadcast to ask this activity to refresh */
    private static final String REFRESH_UI_ACTION = WifiPreferences.class.getPackage().getName() + ".refresh_ui";
    
    /** Broadcast receiver that receives refresh requests for this activity */
    private BroadcastReceiver mRefreshReceiver = null;
    
    /** Service binder for synchronous communication with Manager Service */
    private ManagerService.ManagerServiceBinder mManagerServiceBinder = null;
    
    /** Service connection for synchronous communication with Manager Service */
    private ServiceConnection mManagerServiceConn;
    
    /** Wifi Name of this preference screen */
    private String mWifiName;
    
    /** Edit bar view */
    private View mEditBarView;
            
    /** Activity creation callback */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_preferences);
                        
        // Set activity title
        Bundle extras = getIntent().getExtras();
        mWifiName = extras.getString(EXTRA_ACTIVITY_WIFI_NAME);
        this.setTitle(mWifiName);
        
        // Set the wifi associated to this preferences
        PreferenceScreen screen = getPreferenceScreen();
        setPreferencesWifiName (screen, mWifiName);
        
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
        if (pref.getKey().startsWith(DataManager.PREFERENCE_WIFICELL)) {
            
            // In edit mode refresh buttons bar
            if (DataManager.getEditMode(this)) {
                // When nothing selected to edit 
                if (!refreshWifiCellPreferences(false)) {
                    // Set edit mode off
                    DataManager.setEditMode(this, false);

                    // Do a refresh of the UI
                    refreshWifiCellPreferences(true);
                }
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
            int idWifiCellPref = position - FIRST_WIFI_CELL_PREFERENCE;

            if (idWifiCellPref >= 0) {

                // Get the selected wifi 
                PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_CELL + mWifiName);
                if (cat != null) {
                    Preference pref = cat.getPreference(idWifiCellPref);
                    String key = pref.getKey();
                    String wifi = DataManager.getWifiOfWifiCellPreference(key);
                    int[] cell = DataManager.getCellOfWifiCellPreference(key);

                    // Mark the selected wifi
                    if ((wifi !=null) && (cell !=null) && (cell.length > 1)) {
                        DataManager.setWifiCellSelected(this, wifi, cell[0], cell[1], true);
                    }

                    // Do a refresh of the UI
                    refreshWifiCellPreferences(true);
                    result = true;
                }
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
            refreshWifiCellPreferences(true);
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
            
                // enable/disable cells of selected wifi cell preferences
                enableSelectedWifiCellPreferences(enable);
           
                // Spawn cell refresh event to the manager service
                ManagerService.forwardEvent(this, CellStateManager.CELL_CHANGE_ACTION, null);                        
            }
            break;
        
        case R.id.button_delete_wifis:
            // Delete selected wifi cell preferences
            deleteSelectedWifiCellPreferences();
           
            // Spawn cell refresh event to the manager service
            ManagerService.forwardEvent(this, CellStateManager.CELL_CHANGE_ACTION, null);            
            break;            
        }
        
        // Set edit mode to false
        DataManager.setEditMode(this, false);

        // Refresh wifi preferences and buttons bar
        refreshWifiCellPreferences(true);      
    }
    
    /** Refresh Preferences activity UI */
    private void refreshUI() {

        // Refresh wifi cell preferences list and buttons bar
        refreshWifiCellPreferences(true);        
    }

    /** Dynamically creates preferences UI objects of each wifi cell stored and shows delete button if required */
    private boolean refreshWifiCellPreferences(boolean refreshAll) {

        boolean active = DataManager.getActivate(this);
        boolean editMode = DataManager.getEditMode(this) && active;        
        
        // Remove all preferences if refreshing the whole list
        PreferenceCategory catCells = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_CELL + mWifiName);
        if (refreshAll) {
            catCells.removeAll();
        }
        
        // Gets current cell
        int[] currentCell = DataManager.getCurrentCell(this);
        
        // Gets all cells of the wifi
        Vector<int[]> cells = DataManager.getCellsbyWifi(this, mWifiName); 
        int numCells = cells.size();
        boolean enableButtons = false;
        boolean toggleEnableWifiCell = true;
        boolean isCurrentWifi = mWifiName.equals(DataManager.getCurrentWifi(this));

        // Iterates over wifi cell preferences
        int pos = 0;
        for (Iterator<int[]> iterator = cells.iterator(); iterator.hasNext();pos++) {
            int[] cell = iterator.next();
            if (cell != null && cell.length > 1) {

                // Button bar have to be shown if any wifi preference is marked as selected
                boolean selected = DataManager.getWifiCellSelected(this, mWifiName, cell[0], cell[1]);
                enableButtons = selected || enableButtons;
            
                // Enable wifi cell button has to be shown if all marked items are disabled
                boolean enabledMark = DataManager.getCellEnabled(this, cell[0], cell[1]);
                toggleEnableWifiCell = ((selected && !enabledMark) || !selected ) && toggleEnableWifiCell;

                if (refreshAll) {

                    boolean isCurrentCell = (currentCell != null) && (currentCell.length > 1) && 
                            (currentCell[0] != 0) && (currentCell[1] != 0) && 
                            (currentCell[0] == cell[0]) && (currentCell[1] == cell[1]);

                    addWifiCellPreference(catCells, cell, pos, numCells, editMode, enabledMark, isCurrentCell, isCurrentWifi, active);
                }
            }
        }
               
        // Make sure buttons are only enabled in edit mode and active
        enableButtons &= editMode &&  active;

        // Shows or hides the buttons bar
        showButtonsBar(enableButtons, toggleEnableWifiCell);
        
        return enableButtons;
    }
    
    /** Dynamically add a wifi cell preference */
    private void addWifiCellPreference (PreferenceCategory cat, int[] cell,  int position, int numCells,
            boolean editMode, boolean enabledMark, boolean isCurrentCell, boolean isCurrentWifi, boolean active) {

        Preference cellPref = null;
        int color = getColorStripe (active && enabledMark, isCurrentWifi, isCurrentCell);

        // Check box preference
        if (editMode) {
            cellPref = new MyCheckBoxPreference(this);            
            ((MyCheckBoxPreference) cellPref).setColorStripe(color);            
            ((MyCheckBoxPreference) cellPref).setGrey(!enabledMark);
        }

        // Icon preference
        else {
            cellPref = new IconPreference(this);
            ((IconPreference) cellPref).setColorStripe(color);
            ((IconPreference) cellPref).setBold(isCurrentCell);
        }

        // Set preference layout
        cellPref.setLayoutResource(R.layout.preference);

        // Sets key, title, and persistence
        Resources res = getResources();
        String title = String.format(res.getString(R.string.preference_title_cell_description), cell[1], cell[0]);        
        String base64 = Base64.encodeToString(mWifiName.getBytes(), Base64.NO_WRAP);
        String key = DataManager.PREFERENCE_WIFICELL + base64 + 
                DataManager.KEY_SEPARATOR + cell[0] + 
                DataManager.KEY_SEPARATOR + cell[1];
        cellPref.setKey(key);
        cellPref.setTitle(title);
        cellPref.setPersistent(true);
        cellPref.setEnabled((enabledMark || editMode) && active);  
        
        // Set the position in the list
        if (!enabledMark)
            position += numCells * 2;
        else if (!isCurrentCell)
            position += numCells;
        cellPref.setOrder(position);
        
        // Set the summary 
        if (enabledMark) {
            cellPref.setSummary(R.string.preference_summary_cell_description);
        }
        else {            
            cellPref.setSummary(R.string.preference_summary_cell_disabled);
        }
        
        // Setup callbacks
        cellPref.setOnPreferenceClickListener(this);           
        
        // Add the preference
        cat.addPreference(cellPref);        
    }
    
    private int getColorStripe (boolean drawStripe, boolean isCurrentWifi, boolean isCurrentCell) {
        int result = 0;
        if (drawStripe) {
            if (isCurrentWifi && isCurrentCell) {
                result = getResources().getColor(R.color.color_audit_trail_green);
            }
            else if (!isCurrentWifi && isCurrentCell) {
                result = getResources().getColor(R.color.color_audit_trail_yellow);                    
            }
            else {
                result = getResources().getColor(R.color.color_audit_trail_red);                                        
            }            
        }
        return result;
    }
    
    /** Dynamically shows or hides the edit mode buttons bar and set properties of the buttons */    
    private void showButtonsBar(boolean show, boolean toggleEnableWifiCell) {

        if (mEditBarView != null) {
            mEditBarView.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show) {
                View buttonEnable = findViewById(R.id.button_enable_wifis);
                if (buttonEnable != null && buttonEnable instanceof Button) {
                    if (toggleEnableWifiCell) {
                        ((Button) buttonEnable).setText(R.string.button_title_enable_wifi);
                    }
                    else {
                        ((Button) buttonEnable).setText(R.string.button_title_disable_wifi);
                    }
                }
            }
        }
    }
        
    /** Handler for preferences change event */
    public boolean onPreferenceChange(Preference pref, Object newValue) {

        // Spawn cell refresh event to the manager service
        ManagerService.forwardEvent(this, CellStateManager.CELL_CHANGE_ACTION, null);

        return true;
    }
    
    /** Update the statically lodaded preferences to the dynamically obtained wifi name */
    private void setPreferencesWifiName (PreferenceScreen screen, String wifiName) {

        // Retrieve all the statically added categories
        PreferenceCategory catActions = (PreferenceCategory) screen.findPreference(DataManager.CATEGORY_ACTIONS_WIFI);        
        PreferenceCategory catCells = (PreferenceCategory) screen.findPreference(DataManager.CATEGORY_CELL);        

        // Update categories key
        if (catCells != null) {
            catCells.setKey(catCells.getKey() + wifiName);
        }
        
        if (catActions != null) {
            catActions.setKey(catActions.getKey() + wifiName);

            // Retrieve all statically added preferences            
            int numPref = catActions.getPreferenceCount();
            for (int i = 0; i < numPref; i++) {

                // Rename preference key
                Preference preference = catActions.getPreference(i);
                preference.setKey(preference.getKey() + wifiName);
                preference.setPersistent(true);

                if (preference instanceof CheckBoxPreference) {
                    
                    // Reset initial value
                    boolean value = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(preference.getKey(), true);
                    ((CheckBoxPreference) preference).setChecked(value);

                    // Set change listener
                    preference.setOnPreferenceChangeListener(this);
                }
            }
        }        
    }
    
    /** Removes all wifi-cell preferences UI objects that are selected by user */
    private void deleteSelectedWifiCellPreferences() {

        PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_CELL + mWifiName);
        if (cat != null) {
            
            // Gets current wifi if any
            String currentWifi = DataManager.getCurrentWifi(this);

            // Gets current cell if any
            int[] currentCell = DataManager.getCurrentCell(this);

            // Remove wifi-cell preferences with value set true
            int numWifiCells = cat.getPreferenceCount();
            for (int i = 0; i < numWifiCells; i++) {

                Preference pref = cat.getPreference(i);
                if (pref != null && pref instanceof CheckBoxPreference && ((CheckBoxPreference) pref).isChecked()) {

                    String key = pref.getKey();
                    String wifi = DataManager.getWifiOfWifiCellPreference(key);
                    int[] cell = DataManager.getCellOfWifiCellPreference(key);

                    if ((wifi != null) && (cell != null) && (cell.length > 1)) {

                        boolean isCurrentWifiCell = (wifi.equals(currentWifi) && (currentCell != null) && (currentCell.length > 1)
                                && (currentCell[0] == cell[0]) && (currentCell[1] == cell[1]));

                        // Deletes the wifi preference from the list
                        deleteWifiCellPreference(cat, key, wifi, cell[0], cell[1], isCurrentWifiCell);                        
                        
                        // Restores loop values
                        i--;
                        numWifiCells--;                        
                    }
                }
            }
        }

        // Gets number of cells of this preference screen wifi
        Vector<int[]> cells = DataManager.getCellsbyWifi(this, mWifiName);

        // There are no more cells associated to this preference screen wifi 
        if (cells != null && !(cells.size() > 0)) {

            // Removes wifi preference
            DataManager.deleteWifiCells(this, mWifiName);

            // Closes this preference screen
            this.finish();
        }

    }

    /** Removes a wifi preference UI object */
    private void deleteWifiCellPreference(PreferenceCategory cat, String key, String wifi, int cellId, int lac, boolean isCurrentWifiCell) {

        // Remove the UI object preference
        Preference pref = cat.findPreference(key);
        cat.removePreference(pref);

        // Disconnects wifi before current wifi-cell is deleted
        if (isCurrentWifiCell && DataManager.getWifiAction(this, StateAction.OFF, wifi)) {

            if (DataManager.getActivate(this)) {
                // Disconnects 
                WifiStateManager.setWifiState(this, StateEvent.DISC, null);

                // Synchronously forward wifi state change event to the manager service
                if (mManagerServiceBinder != null)
                    mManagerServiceBinder.syncForwardEvent(this, WifiManager.NETWORK_STATE_CHANGED_ACTION, null);
            }
        }

        // Remove the stored preference and all wificell associations of this wifi
        DataManager.deleteWifiCell(this, wifi, cellId, lac);
    }

    /** Set a value to all wifi preferences UI objects that are selected by user */
    private void enableSelectedWifiCellPreferences(boolean value) {

        PreferenceCategory cat = (PreferenceCategory) getPreferenceScreen().findPreference(DataManager.CATEGORY_CELL + mWifiName);
        if (cat != null) {
            // Remove wifi-cell preferences with value set true
            int numWifiCells = cat.getPreferenceCount();
            for (int i = 0; i < numWifiCells; i++) {

                Preference pref = cat.getPreference(i);
                if (pref != null && pref instanceof CheckBoxPreference && ((CheckBoxPreference) pref).isChecked()) {

                    String key = pref.getKey();
                    String wifi = DataManager.getWifiOfWifiCellPreference(key);
                    int[] cell = DataManager.getCellOfWifiCellPreference(key);

                    if ((wifi != null) && (cell != null) && (cell.length > 1)) {

                        // Enable or dissable this cell
                        DataManager.setCellEnabled(this, cell[0], cell[1], value);

                        // Unmark the preference
                        DataManager.setWifiCellSelected(this, wifi, cell[0], cell[1], false);
                    }
                }
            }
        }
    }
    
    /** Handler of options menu creation */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_wifi_prefereces, menu);
        return true;
    }

    /**
     * Handler of options menu preparation: Shows menu only if active and not in edit mode
     * */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);        
        return (!DataManager.getEditMode(this) && DataManager.getActivate(this));
    }
    
    /**
     * Handler of options menu selection: Edit: Sets edit mode and shows buttons bar Toggle Wifi: changes wifi status
     * Help: Shows help about app dialog
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Edit wifi list option selected
        if (item.getItemId() == R.id.context_menu_edit) {

            // Set edit mode
            DataManager.setEditMode(this, true);

            // Refresh wifi cell preferences list and delete button
            refreshUI();
        }
        return true;
    }       

}