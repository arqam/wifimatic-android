/*
 * CellStateManager.java
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

package org.cprados.wificellmanager.sys;

import java.util.Set;

import org.cprados.wificellmanager.BuildConfig;
import org.cprados.wificellmanager.DataManager;
import org.cprados.wificellmanager.StateMachine;
import org.cprados.wificellmanager.StateMachine.StateAction;
import org.cprados.wificellmanager.StateMachine.StateEvent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

/** Manages changes in cell location state of the system */
public class CellStateManager {
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = CellStateManager.class.getPackage().getName();
        
    /** Extra name for Location Area Code that may be included as extra data in intents to launch this service */
    public static final String EXTRA_LAC = CellStateManager.class.getName() + ".lac";

    /** Extra name for Cell Id that may be included in intents to launch this service */
    public static final String EXTRA_CID = CellStateManager.class.getName() + ".cid";
    
    /** Extra name for operator code that may be included in intents to launch this service */
    public static final String EXTRA_OP = CellStateManager.class.getName() + ".op";
    
    /** Extra name for number of nearby wifis discovered when a wifi state change event is received by this service */
    private static final String EXTRA_NEARBY_WIFIS = CellStateManager.class.getName() + ".nearby_wifis";
    
    /** Extra name initiator for flag indicating if any nearby wifi requires an automatic action */
    private static final String EXTRA_WIFIS_ACTION = CellStateManager.class.getName() + ".wifis_action_";
    
    /** Action of the intents sent to the service to refresh cell location */
    public static final String CELL_CHANGE_ACTION = CellStateManager.class.getName() + ".cell_refresh";

    /** Extra value that denotes an unknown Cell Id or Lac */
    public static final int CELL_UNKNOWN = 0;
    
    /**
     * Determines if current cell location is inside or outside a known area and updates cid, lac, and nearby wifis
     * state data extras
     */
    public static StateEvent getCellState (Context context, Intent intent, Bundle stateData) {
        StateEvent result = null;
        int cid = CELL_UNKNOWN;
        int lac = CELL_UNKNOWN;
        String op = null;
        
        // Retrieves Cell Id, Lac and Operator code from Telephony Manager
        if (intent == null || !intent.hasExtra(EXTRA_CID) || !intent.hasExtra(EXTRA_LAC) || !intent.hasExtra(EXTRA_OP)) {
            
            // Force update location fix if configured
            forceLocationUpdate(context);
        
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            CellLocation location = tm.getCellLocation();
            
            if (location != null) {
                // GSM or UMTS cell location received
                if (location instanceof GsmCellLocation) {
                    cid = ((GsmCellLocation) location).getCid();
                    lac = ((GsmCellLocation) location).getLac();
                    op = tm.getNetworkOperator();
                }
                // CDMA cell location received
                else if (location instanceof CdmaCellLocation) {
                    cid = ((CdmaCellLocation) location).getBaseStationId();
                    lac = ((CdmaCellLocation) location).getSystemId();
                    int networkId = ((CdmaCellLocation) location).getNetworkId();
                    op = (networkId != -1) ? String.valueOf(networkId) : null;
                }
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "CellStateManager: Location obtained: [" + lac + ", " + cid + ", " + op + "]");
                }
            }
        }
        
        // Retrieves Cell Id, Lac and Operator code from intent
        else {
            cid = intent.getIntExtra(EXTRA_CID, CELL_UNKNOWN);
            lac = intent.getIntExtra(EXTRA_LAC, CELL_UNKNOWN);
            op = intent.getStringExtra(EXTRA_OP);
        }
                                        
        // Calculates number of nearby wifis and determines cell state
        Set<String> wifis = null;
        int numWifis = 0;        
        if ((cid > CELL_UNKNOWN) && (lac > CELL_UNKNOWN)) {            
            
            // Cell state is IN
        	if ((DataManager.getCellEnabled(context, cid, lac) &&
                    (wifis = DataManager.getWifisByCell(context, cid, lac))!= null) && 
                    (numWifis = DataManager.getCountWifisEnabled(context, wifis)) > 0) {
                result = StateEvent.IN;
                // Saves the flags that indicate if auto on and off actions are enabled in this cell (for any wifi in this cell)
                stateData.putBoolean(EXTRA_WIFIS_ACTION + StateAction.ON, DataManager.getWifiAction(context, StateAction.ON, wifis));
                stateData.putBoolean(EXTRA_WIFIS_ACTION + StateAction.OFF, DataManager.getWifiAction(context, StateAction.OFF, wifis));
            }
        	
            // Cell state is OUT
            else {
                // Clear the flag that indicate if auto on action is enabled in this cell 
                stateData.putBoolean(EXTRA_WIFIS_ACTION + StateAction.ON, true);
                result = StateEvent.OUT;
            }            
        }

        // Cell state is UNK (no coverage)
        else if (op == null || op.equals("")) {
        	result = StateEvent.UNK;
        }

        // Saves state data information: Cell Id, Lac, Operator and number of nearby wifis in this cell
        if (result != null) {
            stateData.putInt(EXTRA_CID, cid);
            stateData.putInt(EXTRA_LAC, lac);
            stateData.putString(EXTRA_OP, op);
            stateData.putInt(EXTRA_NEARBY_WIFIS, numWifis);
        }
        
        // Discards location change
        else {
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "CellStateManager: Operator: " + op + ".Fake -1 location discarded");
            }
        }
                
        return result;
    }    
    
    /** Returns Cid extra information from stateData bundle */
    public static int getCid (Bundle stateData) {
        int cid = (stateData != null) ? stateData.getInt(EXTRA_CID, CELL_UNKNOWN) : CELL_UNKNOWN;
        return cid;
    }
    
    /** Returns Lac extra information from stateData bundle */    
    public static int getLac (Bundle stateData) {
        int lac = (stateData != null) ? stateData.getInt(EXTRA_LAC, CELL_UNKNOWN) : CELL_UNKNOWN;
        return lac;
    }
    
    /** Sets Lac and Cid extra information to the stateData bundle */
    public static void setCurrentCell (Bundle stateData, int[] currentCell) {
        if (stateData != null && currentCell != null && currentCell.length > 1) {
            stateData.putInt(EXTRA_CID, currentCell[0]);
            stateData.putInt(EXTRA_LAC, currentCell[1]);
        }
    }
 
    /** Returns Operator extra information from stateData bundle */    
    public static String getOperator (Bundle stateData) {
        String op = (stateData != null) ? stateData.getString(EXTRA_OP) : null;
        return op;
    }

    /** Sets Operator extra information to the stateData bundle */
    public static void setOperator (Bundle stateData, String op) {
        if (stateData != null && op != null && op.length() > 1) {
            stateData.putString(EXTRA_OP, op);
        }
    }
    
    /** Returns number of nearby wifis extra information from stateData bundle */    
    public static int getNearbyWifis (Bundle stateData) {
        int numWifis = (stateData != null) ? stateData.getInt(EXTRA_NEARBY_WIFIS, 0) : 0;
        return numWifis;
    }    
    
    /** Sets number of nearby wifis extra information to the stateData bundle*/
    public static void setNearbyWifis (Bundle stateData, int numWifis) {
        if (stateData != null ) {
            stateData.putInt(EXTRA_NEARBY_WIFIS, numWifis);
        }
    }

    /** Returns whether action ON or OFF enabled extra information from stateData bundle */
    public static boolean getActionEnabled (Bundle stateData, StateMachine.StateAction action) {
        boolean wifiAction = true;        
        if (action !=null && stateData != null) {
            String key = EXTRA_WIFIS_ACTION + action.name();            
            wifiAction = (stateData != null && stateData.containsKey(key))? stateData.getBoolean(key) : true;
        }
        return wifiAction;
    }    

    /** Sets whether action ON or OFF are enabled extra information to the stateData bundle */
    public static void setActionEnabled (Bundle stateData, StateMachine.StateAction action, boolean value) {
        if (stateData != null && action !=null) { 
            stateData.putBoolean(EXTRA_WIFIS_ACTION + action.name(), value);
        }
    }
    
    /** Fix to force android to refresh cell id and lac before getting it via Telephony Manager API */
    private static void forceLocationUpdate(Context context) {

        if (DataManager.getForceUpdateLocation(context)) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            if (lm != null) {

                double latitude = 40.189326;
                double longitude = -2.732304;

                Intent intent = new Intent("forceLocationUpdate");
                PendingIntent proximityIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
                lm.addProximityAlert(latitude, longitude, 100000, 1000, proximityIntent);
            }
        }
    }
}
