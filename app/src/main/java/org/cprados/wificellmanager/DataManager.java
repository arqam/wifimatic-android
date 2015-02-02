/*
 * DataManager.java
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

package org.cprados.wificellmanager;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.cprados.wificellmanager.StateMachine.StateEvent;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

/** Class that manages persistent data of the application */
public class DataManager {
    
    /** Full app version */
    private static final boolean FULL_VERSION = false;
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = DataManager.class.getPackage().getName();
    
    /** Key separator for Wifi and WifiCell preferences */
    public static final String KEY_SEPARATOR = "_";

    /** Wifi preferences key initiator */
    public static final String PREFERENCE_WIFI = "preference_wifi" + KEY_SEPARATOR;

    /** Wifi-cell associations key initiator */
    public static final String PREFERENCE_WIFICELL = "preference_wificell" + KEY_SEPARATOR;

    /** Wifi preference action key initiator */
    public static final String PREFERENCE_ACTION_WIFI = "preference_action_wifi" + KEY_SEPARATOR;  
    
    /** Cell enabled preference key initiator */
    public static final String PREFERENCE_CELL_ENABLED = "preference_cell_enabled" + KEY_SEPARATOR;  
    
    /** Activate preference key */
    public static final String PREFERENCE_ACTIVATE = "preference_activate";

    /** Activate preference default value */
    public static final boolean PREFERENCE_DEFAULT_ACTIVATE = false;
    
    /** Activate preference key */
    public static final String PREFERENCE_ADD_WIFIS = "preference_add_wifis";

    /** Activate preference default value */
    public static final boolean PREFERENCE_DEFAULT_ADD_WIFIS = true;
    
    /** Frequency preference key */
    public static final String PREFERENCE_FREQ = "preference_frequency";

    /** Frequency preference default value */
    public static final int PREFERENCE_DEFAULT_FREQ = 15;

    /** Notifications preference key */
    public static final String PREFERENCE_NOT = "preference_notifications";

    /** Notifications preference default value */
    public static final boolean PREFERENCE_DEFAULT_NOT = false;
    
    /** Remembered Wifis list category key */
    public static final String CATEGORY_WIFIS = "category_wifis";   

    /** Application status preference key */
    public static final String PREFERENCE_STATUS = "preference_status";

    /** Edit mode preference key */
    public static final String PREFERENCE_EDIT_MODE = "preference_edit_mode";
    
    /** Edit mode preference default value */
    public static final boolean PREFERENCE_DEFAULT_EDIT_MODE = false;

    /** Currently connected wifi preference key  */
    public static final String PREFERENCE_CURRENT_WIFI = "preference_current_wifi";

    /** Current cell preference key */
    public static final String PREFERENCE_CURRENT_CELL = "preference_current_cell";
    
    /** Currently enabled actions preference ket initiator */
    public static final String PREFERENCE_CURRENT_ACTION = "preference_current_action_";
        
    /** Remembered cells of a wifi category key initiator */
    public static final String CATEGORY_CELL = "category_cells_";

    /** Actions configured for a wifi category key initiator */
    public static final String CATEGORY_ACTIONS_WIFI = "category_actions_wifi_";
    
    /** Time interval preference key */
    public static final String PREFERENCE_TIME_INTERVAL = "preference_time_interval_enable";
    
    /** Time interval default value */
    public static boolean PREFERENCE_DEFAULT_TIME_INTERVAL = false;
    
    /** Time interval start preference key */
    public static final String PREFERENCE_TIME_INTERVAL_BEGIN = "preference_time_interval_begin";
    
    /** Time hour and minute separator */
    public static final String TIME_SEPARATOR = ":";
    
    /** Time interval start default value */
    public static String PREFERENCE_DEFAULT_TIME_INTERVAL_BEGIN = "00" + TIME_SEPARATOR + "00";

    /** Time interval stop preference key */
    public static final String PREFERENCE_TIME_INTERVAL_END = "preference_time_interval_end";
    
    /** Time interval stop default value */
    public static String PREFERENCE_DEFAULT_TIME_INTERVAL_END = "07" + TIME_SEPARATOR + "00";
    
    /** Advanced preference key */
    public static final String PREFERENCE_ADVANCED = "preference_advanced";

    /** Turn on screen preference key */
    public static final String PREFERENCE_TURN_ON_SCREEN = "preference_turn_on_screen";
    
    /** Turn on screen preference default value */
    public static final boolean PREFERENCE_DEFAULT_TURN_ON_SCREEN = false;
    
    /** Force update location preference key */
    public static final String PREFERENCE_FORCE_UPDATE_LOCATION = "preference_force_update_location";
    
    /** Force update location preference default value */
    public static final boolean PREFERENCE_DEFAULT_FORCE_UPDATE_LOCATION = false;
    
    /** Disable wifi after disconnect timeout preference key */
    public static final String PREFERENCE_OFF_AFTER_DISC_TIMEOUT = "preference_off_after_disc_timeout";
    
    /** Disable wifi after disconnect timeout preference default value in seconds */
    public static final int PREFERENCE_DEFAULT_OFF_AFTER_DISC_TIMEOUT = 0;
    
    /** Number of donations preference key */
    public static final String PREFERENCE_DONATIONS = "preference_donations";
    
    /** Number of non-managed donations preference default value */
    public static final int PREFERENCE_DEFAULT_DONATIONS = 0;
    
    /** Has done managed donation preference key */
    public static final String PREFERENCE_HAS_DONATED = "preference_has_donated";
    
    /** Has done managed donation preference default value */
    public static final boolean PREFERENCE_DEFAULT_HAS_DONATED = false;
    
    /** Show example history preference key */
    public static final String PREFERENCE_HISTORY_EXAMPLE = "preference_history_example";
    
    /** First execution preference key */
    public static final String PREFERENCE_WIZARD_STEP = "preference_wizard_step";
    
    /** First execution preference default value */
    public static final int PREFERENCE_DEFAULT_WIZARD_STEP = 1;
    
    /** Restored transactions preference key */
    public static final String PREFERENCE_TRANSACTIONS_RESTORED = "preference_transactions_restored";

    /** Restored transactions default value */
    public static final boolean PREFERENCE_DEFAULT_TRANSACTIONS_RESTORED = false;

    /** Mobile data managed preference key */
    public static final String PREFERENCE_MOBILE_DATA_MANAGED = "preference_mobile_data_managed";

    /** Mobile data managed preference default value */
    public static final boolean PREFERENCE_DEFAULT_MOBILE_DATA_MANAGED = false;
    
    /** Pending mobile data action preference key */
    public static final String PREFERENCE_PENDING_MOBILE_DATA_ACTION = "pending_mobile_data_action";

    /** Unknown location activates Wi-Fi preference key */
    public static final String PREFERENCE_UNK_LOCATION_ACTIVATES_WIFI = "preference_unk_location_activates_wifi";

    /** Unknown location activates Wi-Fi default value */
    public static final boolean PREFERENCE_DEFAULT_UNK_LOCATION_ACTIVATES_WIFI = false;

    /** Inflight wifi state change preference key */
    public static final String PREFERENCE_INFLIGHT_WIFI_ACTION = "preference_inflight_wifi_action";
    
    /** Comparator used to compare cells */
    public static Comparator<int[]> sCellComparator = new Comparator<int[]>() {

        @Override
        public int compare(int[] lhs, int[] rhs) {
            int result = 0;                                    
            
            int l = (lhs.length > 1) ? lhs[1] : 0;
            int r = (rhs.length > 1) ? rhs[1] : 0;
            
            if ((result = l-r) == 0) {
                
                l = (lhs.length > 0) ? lhs[0] : 0;
                r = (rhs.length > 0) ? rhs[0] : 0;                
                result = l-r;
            }
            
            return result;
        }                                
    };
                
    /** Returns activate preference */
    public static boolean getActivate(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_ACTIVATE, PREFERENCE_DEFAULT_ACTIVATE);
    }

    /** Sets activate preference */
    public static void setActivate(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_ACTIVATE, status).commit();
    }
    
    /** Returns activate preference */
    public static boolean getAddWifis(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_ADD_WIFIS, PREFERENCE_DEFAULT_ADD_WIFIS);
    }

    /** Sets activate preference */
    public static void setAddWifis(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_ADD_WIFIS, status).commit();
    }
    
    /** Checks if a wifi exists in the wifi list */
    public static boolean isExistantWifi (Context context, String wifi) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.contains(PREFERENCE_WIFI + wifi); 
    }
    
    /** Returns frequency preference */
    public static int getFrequency(Context context) {

        int result;
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            result = Integer.parseInt(p.getString(PREFERENCE_FREQ, Integer.toString(PREFERENCE_DEFAULT_FREQ)));
        }
        catch (NumberFormatException e) {
            result = PREFERENCE_DEFAULT_FREQ;
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        return result;
    }

    /** Sets frequency preference */
    public static void setFrequency(Context context, int frequency) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putString(PREFERENCE_FREQ, String.valueOf(frequency)).commit();
    }

    /** Returns notifications preference */
    public static boolean getNotifications(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_NOT, PREFERENCE_DEFAULT_NOT);
    }

    /** Sets notifications preference */
    public static void setNotifications(Context context, boolean show) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_NOT, show).commit();
    }

    /** Returns the status message */
    public static StateMachine.State getState(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        StateMachine.State result = null;
        String state = p.getString(PREFERENCE_STATUS,null);
        if (state != null) {
            try {
                result = StateMachine.State.valueOf(state); 
            }
            catch (Exception e) { 
                Log.e(LOGTAG, Log.getStackTraceString(e));
            }
        }
                
        return result;
    }

    /** Sets the status message */
    public static void setState(Context context, StateMachine.State state) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (state != null) {
            p.edit().putString(PREFERENCE_STATUS, state.name()).commit();
        }
        else {
            p.edit().remove(PREFERENCE_STATUS).commit();
        }
    }

    /** Returns the edit mode status of the UI*/
    public static boolean getEditMode(Context context) {
        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_EDIT_MODE,PREFERENCE_DEFAULT_EDIT_MODE);
    }

    /** Sets the edit mode status of the UI*/
    public static void setEditMode(Context context, boolean editMode) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_EDIT_MODE, editMode).commit();
    }

    /** Returns the currently connected wifi or null if there isn't a wifi connected */
    public static String getCurrentWifi(Context context) {
        try {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            return p.getString(PREFERENCE_CURRENT_WIFI,null);
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
            return ".";
        }
    }

    /** Sets currently connected wifi */
    public static void setCurrentWifi(Context context, String wifi) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (wifi != null) {
            p.edit().putString(PREFERENCE_CURRENT_WIFI, wifi).commit();
        }
        else {            
            p.edit().remove(PREFERENCE_CURRENT_WIFI).commit();
        }
    }

    /** Returns if an action is the currently enabled */
    public static boolean getCurrentAction(Context context, StateMachine.StateAction action) {

        boolean result = true;
        
        if (action != null) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            result = p.getBoolean(PREFERENCE_CURRENT_ACTION + action.name(), true);
        }
        
        return result;
    }

    /** Sets if an action is the currently enabled */
    public static void setCurrentAction(Context context, StateMachine.StateAction action, boolean value) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (action != null) {
            if (!value) {
                p.edit().putBoolean(PREFERENCE_CURRENT_ACTION + action.name(), value).commit();
            }
            else {
                p.edit().remove(PREFERENCE_CURRENT_ACTION + action.name()).commit();
            }
        }
    }
    
    /** Returns the current cell id and lac or {0,0} if unknown */
    public static int[] getCurrentCell(Context context) {
        
        int[] result = {0,0};
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String cell = p.getString(PREFERENCE_CURRENT_CELL, null);
        if (cell != null) {
            String parts[] = cell.split(KEY_SEPARATOR);
            try {
                result[0] = Integer.parseInt(parts[0]);
                result[1] = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException e) {Log.e(LOGTAG, Log.getStackTraceString(e));}
            catch (ArrayIndexOutOfBoundsException e) {Log.e(LOGTAG, Log.getStackTraceString(e));}
        }
        
        return result;
    }

    /** Sets the current cell id and lac */
    public static void setCurrentCell(Context context, int cid, int lac) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String value = cid + KEY_SEPARATOR + lac;
        if (cid != 0 && lac != 0) {
            p.edit().putString(PREFERENCE_CURRENT_CELL, value).commit();
        }
        else {
            p.edit().remove(PREFERENCE_CURRENT_CELL).commit();
        }            
    }

    /** Returns if an action for a wifi is enabled */
    public static boolean getWifiAction(Context context, StateMachine.StateAction action, String wifi) {

        boolean result = true;

        if (wifi != null && action != null && action.isDeactivable()) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            String key = PREFERENCE_ACTION_WIFI +  action.name() + KEY_SEPARATOR + wifi;
            result = p.getBoolean(key, true);
        }

        return result;
    }
    
    /** Returns if an action for any wifi of a set is enabled */
    public static boolean getWifiAction(Context context, StateMachine.StateAction action, Set<String> wifis) {

        boolean result = true;

        if (wifis != null && action != null) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            for (Iterator<String> iterator = wifis.iterator(); iterator.hasNext();) {
                String wifi = iterator.next();
                String key = PREFERENCE_ACTION_WIFI +  action.name() + KEY_SEPARATOR + wifi;
                if (result = p.getBoolean(key, true))
                    break;
            }
        }

        return result;
    }

    /** Sets actions done for a wifi */
    public static void setWifiAction(Context context, StateMachine.StateAction action, String wifi, boolean value) {

        if (wifi != null && action != null) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            String key = PREFERENCE_ACTION_WIFI + action.name() + KEY_SEPARATOR + wifi;
            if (!value) {
                p.edit().putBoolean(key, value).commit();
            }
            else {
                p.edit().remove(key).commit();
            }
        }
    }
    
    /** Returns if any action of the wifi is enabled */
    public static boolean getWifiEnabled (Context context, String wifi) {

        boolean result = false;
        
        // Bulk set all wifi actions to the wifi
        for (StateMachine.StateAction action : StateMachine.StateAction.values()) {
            if ((action.isDeactivable())  && 
                    (result |= DataManager.getWifiAction(context, action, wifi))) {
                break;
            }
        }
        return result;
    }
    
    /** Returns number of wifis enabled */
    public static int getCountWifisEnabled (Context context, Set<String> wifis) {

        int result = 0;

        if (wifis != null) {
            // Iterate over wifi list
            for (Iterator<String> iterator = wifis.iterator(); iterator.hasNext();) {                                
                // Iterate over wifi actions
                String wifi = iterator.next();
                if (getWifiEnabled (context, wifi))
                    result++;
            }
        }
        return result;
    }
    
    /** Bulk set all deactivable wifi actions of the wifi to the specified value */
    public static void setWifiEnabled (Context context, String wifi, boolean value) {

        for (StateMachine.StateAction action : StateMachine.StateAction.values()) {
            if (action.isDeactivable())
                DataManager.setWifiAction(context, action, wifi, value);
        }        
    }            

    /** Returns if cell is enabled. Affects all wifi-cell associations */
    public static boolean getCellEnabled(Context context, int cellId, int lac) {

        boolean result = true;

        if (cellId!= 0 && lac != 0) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            String key = PREFERENCE_CELL_ENABLED +  cellId + KEY_SEPARATOR + lac;
            result = p.getBoolean(key, true);
        }
        else {
            result = false;
        }

        return result;
    }
    
    /** Sets if cell is enabled. Affects all wifi-cell associations */
    public static void setCellEnabled(Context context, int cellId, int lac, boolean enabled) {
        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String key = PREFERENCE_CELL_ENABLED +  cellId + KEY_SEPARATOR + lac;
        
        if (!enabled) {
            p.edit().putBoolean(key, enabled).commit();
        }
        else {
            p.edit().remove(key).commit();
        }                        
    }

    /** Returns number of cells enabled */
    public static int getCountCellsEnabled (Context context, Vector<int[]> cells) {

        int result = 0;

        if (cells != null) {
            // Iterate over cells list
            for (Iterator<int[]> iterator = cells.iterator(); iterator.hasNext();) {                                
                // Iterate over wifi actions
                int[] cell = iterator.next();
                if (cell != null && cell.length > 1 && getCellEnabled (context, cell[0], cell[1]))
                    result++;
            }
        }
        return result;
    }
    
    /** Get the enabled value of a wifi preference */
    public static boolean getWifiSelected(Context context, String wifi) {
        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_WIFI + wifi, false);
    }

    /** Set the enabled value of a wifi preference */
    public static void setWifiSelected(Context context, String wifi, boolean enabled) {
        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_WIFI + wifi, enabled).commit();
    }

    /** Get the enabled value of a wifi cell preference */
    public static boolean getWifiCellSelected(Context context, String wifi, int cellId, int lac) {
        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String base64 = Base64.encodeToString(wifi.getBytes(), Base64.NO_WRAP);
        String key = PREFERENCE_WIFICELL + base64 + KEY_SEPARATOR + cellId + KEY_SEPARATOR + lac;
        boolean result = false;
        
        // Version <15 store this preference as String and will throw ClassCastException first time
        try {
            result = p.getBoolean(key, false);
        }
        catch (Exception e) {
            setWifiCellSelected(context, wifi, cellId, lac, false);
        }
        
        return result;
    }
    
    /** Set the enabled value of a wifi cell preference */
    public static void setWifiCellSelected(Context context, String wifi, int cellId, int lac, boolean enabled) {
        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String base64 = Base64.encodeToString(wifi.getBytes(), Base64.NO_WRAP);
        String key = PREFERENCE_WIFICELL + base64 + KEY_SEPARATOR + cellId + KEY_SEPARATOR + lac;
        p.edit().putBoolean(key, enabled).commit();
    }

    /** Delete a wifi-cell preference */
    public static void deleteWifiCell(Context context, String wifi, int cellId, int lac) {
        
        // Removes the wifi-cell association
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String base64 = Base64.encodeToString(wifi.getBytes(), Base64.NO_WRAP);
        String key = PREFERENCE_WIFICELL + base64 + KEY_SEPARATOR + cellId + KEY_SEPARATOR + lac;
        p.edit().remove(key).commit();        
        
        // Cleans cell enabled/disabled mark if it is the last wifi the cell is assigned to
        Set<String> wifis = getWifisByCell(context, cellId, lac);
        if (wifis == null || wifis.size() <= 0) {
            setCellEnabled(context, cellId, lac, true);
        }                                
    }
    
    /** 
     * Save a Wifi and associated Cell in the preferences 
     * @return true if wifi cell did not exist before and was added 
     * */
    public static boolean addWifiCell(Context context, String wifi, int cellId, int lac) {

        // Adds the wifi preference
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = p.edit();
        String key = PREFERENCE_WIFI + wifi;
        editor.putBoolean(key, getWifiSelected(context, wifi));
        
        // Saves the wifi cell association preference and commits
        String base64 = Base64.encodeToString(wifi.getBytes(), Base64.NO_WRAP);
        key = PREFERENCE_WIFICELL + base64 + KEY_SEPARATOR + cellId + KEY_SEPARATOR + lac;
        boolean result = !p.contains(key);
        //editor.putString(key, wifi).commit();
        editor.putBoolean(key, getWifiCellSelected(context, wifi, cellId, lac));
        editor.commit();
        
        return result;
    }

    /** Delete all Wifi Cell associations of a Wifi in the preferences */
    public static void deleteWifiCells(Context context, String wifi) {

        // Deletes the wifi preference
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String key = PREFERENCE_WIFI + wifi;
        p.edit().remove(key).commit();
        
        // Deletes all wifi cell, actions associations
        String base64 = Base64.encodeToString(wifi.getBytes(), Base64.NO_WRAP);
        Set<String> keys = p.getAll().keySet();

        Set<int[]> deletedCells = new TreeSet<int[]>(sCellComparator);
        Set<int[]> remainigCells = new TreeSet<int[]>(sCellComparator);
        
        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {            
            key = iterator.next();

            // Preference is a wifi-cell
            if (key.startsWith(PREFERENCE_WIFICELL)) {

                int[] cell = DataManager.getCellOfWifiCellPreference(key);                
                if (cell != null && cell.length > 1) {
                    
                    // Preference is a wifi-cell of given wifi
                    if (key.startsWith(PREFERENCE_WIFICELL + base64 + KEY_SEPARATOR)) {
                        p.edit().remove(key).commit();                
                        deletedCells.add(cell);
                    }
                    // Preference os a wifi-cell of other wifi
                    else {
                        remainigCells.add(cell);
                    }
                }
            }
            // Preference is an action of given wifi
            else if (key.startsWith(PREFERENCE_ACTION_WIFI) && key.endsWith(KEY_SEPARATOR + wifi)) {
                p.edit().remove(key).commit();                                
            }
        }
        
        // Enable all cells whose wifi-cell has been deleted and don't belong to another cell
        deletedCells.removeAll(remainigCells);
        for (int[] element : deletedCells ) {
            if (element != null && element.length > 1) {
                DataManager.setCellEnabled(context, element[0], element[1], true);
            }
        }
    }

    /** Gets Wifis associated with a given cell */
    public static Set<String> getWifisByCell(Context context, int cellId, int lac) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> keys = p.getAll().keySet();
        TreeSet<String> result = new TreeSet<String>();

        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            String key = iterator.next();
            if (key.startsWith(PREFERENCE_WIFICELL) && key.endsWith(KEY_SEPARATOR + cellId + KEY_SEPARATOR + lac)) {
                result.add(getWifiOfWifiCellPreference(key));
            }
        }

        return result;
    }
        
    /** Parses wifi cell preference key to return wifi name */
    public static String getWifiOfWifiCellPreference(String wifiCellPreferenceKey) {

        String wifi = null;
        if (wifiCellPreferenceKey != null && wifiCellPreferenceKey.startsWith(PREFERENCE_WIFICELL)) {
            String parts[] = wifiCellPreferenceKey.split(KEY_SEPARATOR);
            if (parts.length > 2) {
                String base64 = parts[2];
                try {
                    wifi = new String(Base64.decode(base64, Base64.NO_WRAP));
                }
                catch (IllegalArgumentException e) {
                    Log.e(LOGTAG, Log.getStackTraceString(e));    
                }
            }
        }
        return wifi;
    }
    
    /** Parses wifi cell preference key to return cell id and lac */    
    public static int[] getCellOfWifiCellPreference(String wifiCellPreferenceKey) {

        int[] cell = { 0, 0 };
        if (wifiCellPreferenceKey != null && wifiCellPreferenceKey.startsWith(PREFERENCE_WIFICELL)) {
            String parts[] = wifiCellPreferenceKey.split(KEY_SEPARATOR);
            try {
                cell[0] = Integer.parseInt(parts[3]);
                cell[1] = Integer.parseInt(parts[4]);
            }
            catch (NumberFormatException e) {
                Log.e(LOGTAG, Log.getStackTraceString(e));
                cell = null;
            }
            catch (ArrayIndexOutOfBoundsException e) {
                Log.e(LOGTAG, Log.getStackTraceString(e));
                cell = null;
            }
        }
        else {
            cell = null;
        }
        return cell;
    }

    /** Parses wifi preference key to return wifi name */
    public static String getWifiOfWifiPreference(String wifiPreferenceKey) {
        String wifi = null;
        if (wifiPreferenceKey != null && wifiPreferenceKey.startsWith(DataManager.PREFERENCE_WIFI) && !wifiPreferenceKey.startsWith(PREFERENCE_WIFICELL)) {
            wifi = wifiPreferenceKey.substring(DataManager.PREFERENCE_WIFI.length());            
        }
        return wifi;
    }

    /** Returns wifis associated with the current cell or null if there isn't or current cell cannot be determined */
    public static Set<String> getWifisOfCurrentCell(Context context) {

        int[] currentCell = getCurrentCell(context);
        Set<String> result = null;

        if (currentCell != null && currentCell[0] != 0 && currentCell[1] != 0) {
            result = getWifisByCell(context, currentCell[0], currentCell[1]);
        }

        return result;
    }

    /** Returns number of wifis associated with the current cell */
    public static int getNumWifisOfCurrentCell(Context context) {

        int[] currentCell = getCurrentCell(context);
        int result = 0;

        if (currentCell != null && currentCell[0] != 0 && currentCell[1] != 0) {
            Set <String> currentWifis = getWifisByCell(context, currentCell[0], currentCell[1]);
            result = currentWifis != null ? getCountWifisEnabled(context, currentWifis) : 0;
        }

        return result;
    }
    
    /** Returns wifis associated with the current cell or null if there isn't or current cell cannot be determined */
    public static boolean getCurrentCellEnabled(Context context) {

        int[] currentCell = getCurrentCell(context);
        boolean result = true;

        if (currentCell != null && currentCell.length > 1 && currentCell[0] != 0 && currentCell[1] != 0) {
            result = getCellEnabled(context, currentCell[0], currentCell[1]);
        }

        return result;
    }
    
    /** Gets all Wifis saved */
    public static Set<String> getAllWifis(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> keys = p.getAll().keySet();
        TreeSet<String> result = new TreeSet<String>();
        int keyIndex = DataManager.PREFERENCE_WIFI.length();

        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            String key = iterator.next();
            if (key.startsWith(PREFERENCE_WIFI) && !key.startsWith(PREFERENCE_WIFICELL)) {      
                String wifi = key.substring(keyIndex);
                result.add(wifi);
            }
        }

        return result;
    }

    /** Get the cells associated to a wifi */
    public static Vector<int[]> getCellsbyWifi(Context context, String wifi) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String base64 = Base64.encodeToString(wifi.getBytes(), Base64.NO_WRAP);
        Set<String> keys = p.getAll().keySet();
        Vector<int[]> result = new Vector<int[]>();

        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            String key = iterator.next();
            if (key.startsWith(PREFERENCE_WIFICELL + base64 + KEY_SEPARATOR)) {                
                int[] element = getCellOfWifiCellPreference(key);
                if (element != null) {
                    result.add(element);                    
                }                
            }
        }        
        Collections.sort(result, sCellComparator);

        return result;
    }
    
    /** Returns time interval enable preference */
    public static boolean getTimeIntervalEnabled(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_TIME_INTERVAL, PREFERENCE_DEFAULT_TIME_INTERVAL);
    }

    /** Sets time interval enable preference */
    public static void setTimeIntervalEnabled(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_TIME_INTERVAL, status).commit();
    }

    /** Returns time interval start preference */
    public static int[] getTimeIntervalBegin (Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String value = p.getString(PREFERENCE_TIME_INTERVAL_BEGIN, PREFERENCE_DEFAULT_TIME_INTERVAL_BEGIN); 
        
        int [] result = new int[2];
        try {
            String[] timeParts=value.split(TIME_SEPARATOR); 
            result[0] = Integer.parseInt(timeParts[0]); 
            result[1] = Integer.parseInt(timeParts[1]);
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }            
        return result;
    }

    /** Returns time interval start and stop preferences */
    public static int[] getTimeIntervalEnd(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String value = p.getString(PREFERENCE_TIME_INTERVAL_END, PREFERENCE_DEFAULT_TIME_INTERVAL_END);
        
        int [] result = new int[2];
        try {
            String[] timeParts=value.split(TIME_SEPARATOR); 
            result[0] = Integer.parseInt(timeParts[0]); 
            result[1] = Integer.parseInt(timeParts[1]);
            
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }            
        return result;
    }

    /** Sets time interval begin preference */
    public static void setTimeIntervalBegin(Context context, int hour, int minute) {

        String value = String.valueOf(hour) + TIME_SEPARATOR + String.valueOf(minute);        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putString(PREFERENCE_TIME_INTERVAL_BEGIN, value).commit();
    }

    /** Sets time interval end preference */
    public static void setTimeIntervalEnd(Context context, int hour, int minute) {

        String value = String.valueOf(hour) + TIME_SEPARATOR + String.valueOf(minute);        
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putString(PREFERENCE_TIME_INTERVAL_END, value).commit();
    }

    /** Returns turn on screen preference */
    public static boolean getTurnOnScreen(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_TURN_ON_SCREEN, PREFERENCE_DEFAULT_TURN_ON_SCREEN);
    }

    /** Sets turn on screen preference */
    public static void setTurnOnScreen(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_TURN_ON_SCREEN, status).commit();
    }
    
    /** Returns turn on screen preference */
    public static boolean getForceUpdateLocation(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_FORCE_UPDATE_LOCATION, PREFERENCE_DEFAULT_FORCE_UPDATE_LOCATION);
    }

    /** Sets turn on screen preference */
    public static void setForceUpdateLocation(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_FORCE_UPDATE_LOCATION, status).commit();
    }
    
    /** Returns off after disc timeout preference value in seconds */
    public static int getOffAfterDiscTimeout (Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        int result;
        try {
            result = Integer.parseInt(p.getString(PREFERENCE_OFF_AFTER_DISC_TIMEOUT, Integer.toString(PREFERENCE_DEFAULT_OFF_AFTER_DISC_TIMEOUT)));
        }
        catch (NumberFormatException e) {
            result = PREFERENCE_DEFAULT_OFF_AFTER_DISC_TIMEOUT;
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        return result;           
    }
    
    /** Sets off after disc timeout preference value in seconds */
    public static void setOffAfterDiscTimeout(Context context, int value) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putString(PREFERENCE_OFF_AFTER_DISC_TIMEOUT, String.valueOf(value)).commit();
    }

    /** Increases or decreases donations counter and returns the value */
    public static int setDonationsCounter(Context context, boolean addOrRefund) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        
        int donations = p.getInt(PREFERENCE_DONATIONS, PREFERENCE_DEFAULT_DONATIONS);
        donations = addOrRefund ? donations+1: donations-1;
        if (donations >= 0) {
            p.edit().putInt(PREFERENCE_DONATIONS, donations).commit();
        }
        return donations;
    }
    
    /** Returns donations counter */
    public static int getDonationsCounter(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);        
        int donations = p.getInt(PREFERENCE_DONATIONS, PREFERENCE_DEFAULT_DONATIONS);
        return donations;
    }        

    /** Sets whether user has purchased a managed donation */
    public static void setHasDonated(Context context, boolean value) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_HAS_DONATED, value).commit();
    }
    
    /** Returns whether user has purchased a donation managed or not managed*/
    public static boolean getHasDonated(Context context, boolean managedOnly) {
        
        boolean result = FULL_VERSION;
        if (!FULL_VERSION) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);        
            boolean hasDonated = p.getBoolean(PREFERENCE_HAS_DONATED, PREFERENCE_DEFAULT_HAS_DONATED);
        
            if (!managedOnly) {
                int donations = p.getInt(PREFERENCE_DONATIONS, PREFERENCE_DEFAULT_DONATIONS);
                result = (hasDonated || (donations > 0));
            }
            else {
                result = hasDonated;
            }
        }
        return result;
    }
    
    /** Returns first time execution preference */
    public static int getWizardStep(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getInt(PREFERENCE_WIZARD_STEP, PREFERENCE_DEFAULT_WIZARD_STEP);
    }

    /** Sets first time execution  preference */
    public static void setWizardStep(Context context, int step) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putInt(PREFERENCE_WIZARD_STEP, step).commit();
    }

    /** Returns first time execution preference */
    public static boolean getRestoredTransactions(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_TRANSACTIONS_RESTORED, PREFERENCE_DEFAULT_TRANSACTIONS_RESTORED);
    }

    /** Sets first time execution  preference */
    public static void setRestoredTransations(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_TRANSACTIONS_RESTORED, status).commit();
    }

    /** Returns mobile data managed preference */
    public static boolean getMobileDataManaged(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_MOBILE_DATA_MANAGED, PREFERENCE_DEFAULT_MOBILE_DATA_MANAGED);
    }

    /** Sets mobile data managed preference */
    public static void setMobileDataManaged(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_MOBILE_DATA_MANAGED, status).commit();
    }

    /** Returns if there is a pending mobile data action from preferences */
    public static boolean getPendingMobileDataAction(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_PENDING_MOBILE_DATA_ACTION, false);
    }

    /** Sets or clears a pending mobile data action to a preference */
    public static void setPendingMobileDataAction(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (status) {
            p.edit().putBoolean(PREFERENCE_PENDING_MOBILE_DATA_ACTION, status).commit();
        }
        else {
            p.edit().remove(PREFERENCE_PENDING_MOBILE_DATA_ACTION).commit();
        }
    }
        
    /** Returns unk location activates wifi preference */
    public static boolean getUnkLocationActivates(Context context) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFERENCE_UNK_LOCATION_ACTIVATES_WIFI, PREFERENCE_DEFAULT_UNK_LOCATION_ACTIVATES_WIFI);
    }

    /** Sets unk location activates wifi preference */
    public static void setUnkLocationActivates(Context context, boolean status) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        p.edit().putBoolean(PREFERENCE_UNK_LOCATION_ACTIVATES_WIFI, status).commit();
    }

    /** Returns if there is an inflight wifi state change from preferences */
    public static StateEvent[] getInflightWifiAction(Context context) {

    	StateEvent[] result = null;
    	
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String action = p.getString(PREFERENCE_INFLIGHT_WIFI_ACTION, null);
        
        if (action != null) {
        	String[] tokens = action.split(TIME_SEPARATOR);        	
        	if (tokens != null && tokens.length > 1) {
        		result = new StateEvent[2];
        		try {        		
        			result[0] = StateEvent.valueOf(tokens[0]);
        			result[1] = StateEvent.valueOf(tokens[1]);
        		}
        		catch (Exception e) {
                    Log.e(LOGTAG, Log.getStackTraceString(e));
        			result = null;
        		}
        	}
        }
        
        return result;
    }

    /** Saves or clears inflight wifi action data to preferences */
    public static void setInflightWifiAction (Context context, StateEvent[] wifiChange) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (wifiChange != null && wifiChange.length > 1) {        	
            p.edit().putString(PREFERENCE_INFLIGHT_WIFI_ACTION, wifiChange[0] + TIME_SEPARATOR + wifiChange[1]).commit();
        }
        else {
            p.edit().remove(PREFERENCE_INFLIGHT_WIFI_ACTION).commit();
        }
    }
    
    /** Returns if it is full version of the app*/
    public static boolean isFullVersion () {
        return FULL_VERSION;
    }
}
