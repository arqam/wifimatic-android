/*
 * Preferences.java
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
import org.cprados.wificellmanager.StateMachine.StateEvent;
import org.cprados.wificellmanager.sys.NotificationManager;
import org.cprados.wificellmanager.sys.WifiStateManager;

import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

/** Main wifi cell manager program Activity based on Tab Activity */
public class Preferences extends TabActivity {
    
    /** Action to be sent in a broadcast to ask this activity to refresh */
    private static final String REFRESH_UI_ACTION = Preferences.class.getPackage().getName() + ".refresh_ui";

    /** Tag for logging this class messages */
    private static final String LOGTAG = Preferences.class.getPackage().getName();
    
    /** Id of the general preferences tab */
    public static final int TAB_GENERAL = 0;

    /** Id of the wifi list preferences tab */
    public static final int TAB_WIFIS = 1;

    /** Id of the time preferences tab */
    public static final int TAB_TIME = 2;

    /** Id of the donate tab */
    public static final int TAB_DONATE = 3;
    
    /** Id of the audit trail tab */
    public static final int TAB_AUDIT = 4;
    
    /** Internal Id of the help about dialog */
    private static final int DIALOG_ABOUT = 0;

    /** Internal Id of the welcome dialog */
    private static final int DIALOG_WELCOME = 1;
    
    /** Activity is drawn with classic theme  */
    private static final boolean THEME_CLASSIC = (android.os.Build.VERSION.SDK_INT < 11);
    
    /** Donate Activity Class reference, to be linked in run time*/
    private static Class<?> sDonateActivity;

    /** Audit Trail Activity Class reference, to be linked in run time */
    private static Class<?> sAuditTrailActivity;
    
    // Obtains references to activity classes in runtime. 
    // If classes are available activities will be shown in tabs
    // Otherwise tabs donate/audit will not be shown
    static {
    	try {
    		sDonateActivity = Class.forName("org.cprados.wificellmanager.ui.DonatePreferences");
    		sAuditTrailActivity = Class.forName("org.cprados.wificellmanager.ui.AuditTrailActivity");
    	}
    	catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
    	}
    }

    /** Creation callback */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.tab_layout);                

        // Add the tabs
        Resources res = getResources();
        TabHost tabHost = getTabHost();
        addTab(tabHost, GeneralPreferences.class, res.getString(R.string.tab_title_options), res.getDrawable(R.drawable.ic_tab_general));
        addTab(tabHost, WifiListPreferences.class, res.getString(R.string.tab_title_wifi_list), res.getDrawable(R.drawable.ic_tab_wifi_list));
        addTab(tabHost, TimeIntervalPreferences.class, res.getString(R.string.tab_title_time_options), res.getDrawable(R.drawable.ic_tab_time));
        addTab(tabHost, sDonateActivity, res.getString(R.string.tab_title_audit_trail), res.getDrawable(R.drawable.ic_tab_audit));
        addTab(tabHost, sAuditTrailActivity, res.getString(R.string.tab_title_audit_trail), res.getDrawable(R.drawable.ic_tab_audit));                        
        
        // By default Hides audit trail
        showTab(TAB_AUDIT, false, TAB_DONATE);       
        
        // Center tabs widget
        tabHost.getTabWidget().setGravity(Gravity.CENTER);

        if (savedInstanceState == null) {
            // Creates Welcome Dialog the first time the activity is created
            welcomeDialog();
        }
    }

    /** If app has not been configured shows welcome dialog otherwise disables it */
    private void welcomeDialog() {
        
        int currentStep = DataManager.getWizardStep(this);
        
        // Checks if welcome dialg is enabled
        if ((currentStep != Dialogs.WelcomeDialogBuilder.FINISHED_STEP)) {
        
            // Check if app was configured before, manually and wizard has not been started before
            if ((currentStep == Dialogs.WelcomeDialogBuilder.FIRST_STEP) && 
                    (DataManager.getActivate(this) || (DataManager.getAllWifis(this).size() > 0))) {
                // Disables welcome dialog
                DataManager.setWizardStep(this,Dialogs.WelcomeDialogBuilder.FINISHED_STEP);
            }
            else {
                // Shows welcome dialog to proceed with configuration
                showDialog(DIALOG_WELCOME);
            }
        }
    }

    /** Resume callback */
    @Override
    protected void onResume() {
        super.onResume();

        // Cleans status bar
        NotificationManager.removeNotification(this);
        
        // Refresh tabs
        refreshTabs();
    }

    /** Refresh tabs shown */
    public void refreshTabs () {        
        // If user has donated hide donation tab and show audit trail
        if (DataManager.getHasDonated(this, false)) {
            showTab(TAB_DONATE, false, TAB_AUDIT);
            showTab(TAB_AUDIT, true, TAB_AUDIT);            
        }
        else {
            showTab(TAB_DONATE, true, TAB_DONATE);
            showTab(TAB_AUDIT, false, TAB_DONATE);                        
        }
    }

    /** Destroy callback */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }

    /** Adds a tab to the main tab activity */
    private void addTab(TabHost tabHost, Class<?> tabActivityClass, String tabName, Drawable tabDrawable) {

        if (tabHost != null && tabActivityClass != null) {
            Intent intent = new Intent().setClass(this, tabActivityClass);
            String label = (!THEME_CLASSIC) ? "" : tabName;
            TabHost.TabSpec spec = tabHost.newTabSpec(tabName).setIndicator(label, tabDrawable).setContent(intent);
            tabHost.addTab(spec);
        }                
    }
    
    /** Shows or hides a tab in the main tab activity */
    private void showTab (int index, boolean show, int alternate) {
        TabHost tabHost = getTabHost();
        if (tabHost != null) {
            TabWidget tabWidget = tabHost.getTabWidget();
            int current = tabHost.getCurrentTab();            
            if (tabWidget != null) {
                View view = tabWidget.getChildTabViewAt(index);
                if (view != null) {
                    view.setVisibility(show ? View.VISIBLE : View.GONE);
                    view.setEnabled(show);
                    if (current == index && !show) {
                        tabHost.setCurrentTab(alternate);
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
        inflater.inflate(R.menu.menu_preferences, menu);
        return true;
    }

    /**
     * Handler of options menu preparation: Shows menu only if active and not in edit mode Toggle Wifi on/off option
     * menu title depending on current wifi state
     * */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean result = false;

        // If options menu has to be shown
        if (!DataManager.getEditMode(this) && DataManager.getActivate(this)) {

            // Gets current wifi state
            StateEvent wifiState = WifiStateManager.getWifiState(this, null, null);

            // Determines the title for the toggle wifi menu option, inverse of current state
            String title = null;
            if (wifiState == StateEvent.OFF) {
                title = getResources().getString(R.string.context_menu_label_toggle_wifi_on);
            }
            else {
                title = getResources().getString(R.string.context_menu_label_toggle_wifi_off);
            }

            // Set the title of the toggle wifi menu option
            menu.findItem(R.id.context_menu_toggle_wifi).setTitle(title);
            result = true;
        }

        return result;
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

            // Refresh wifi preferences list and delete button
            requestRefresh(this);
            getTabHost().setCurrentTab(TAB_WIFIS);
        }

        // Toggle wifi option selected
        else if (item.getItemId() == R.id.context_menu_toggle_wifi) {

            Resources res = getResources();
            String action = null;
            if (item.getTitle() != null) {
                // Gets required action: on or off
                action = item.getTitle().toString();

                // Changes wifi state accordingly
                if (action.equals(res.getString(R.string.context_menu_label_toggle_wifi_on))) {
                    WifiStateManager.setWifiState(this, StateEvent.DISC, null);
                    Toast.makeText(this, R.string.toast_label_switching_wifi_on, Toast.LENGTH_LONG).show();
                }
                else if (action.equals(res.getString(R.string.context_menu_label_toggle_wifi_off))) {
                    WifiStateManager.setWifiState(this, StateEvent.OFF, null);
                    Toast.makeText(this, R.string.toast_label_switching_wifi_off, Toast.LENGTH_LONG).show();
                }
            }
        }

        // Help option selected
        else if (item.getItemId() == R.id.context_menu_help) {
            showDialog(DIALOG_ABOUT);
        }

        return true;
    }

    /**
     * Handler for this Activity dialogs creation
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {

        Dialog result = null;

        switch (id) {

        // Constructs help about dialog box 
        case DIALOG_ABOUT:
            result = (new Dialogs.AboutDialogBuilder(this)).create();
            break;

        case DIALOG_WELCOME:
            result = (new Dialogs.WelcomeDialogBuilder(this)).create();
            break;            
        }

        return result;
    }

    /** Notifies the Preferences Activity of changes to refresh the UI */
    public static void requestRefresh (Context context) {       

        // Creates the intent
        Intent intent = new Intent(REFRESH_UI_ACTION);
            
        // Sends a broadcast to the  preferences activity to refresh contents
        context.sendBroadcast(intent);   
    }
}
