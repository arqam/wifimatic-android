/*
 * WifiStateManager.java
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

import java.util.Iterator;
import java.util.List;

import org.cprados.wificellmanager.BuildConfig;
import org.cprados.wificellmanager.StateMachine.StateEvent;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import java.lang.reflect.Method;

import android.util.Log;

/** Manages changes in wifi and connection state of the system */
public class WifiStateManager {
	    
    /** Tag for logging this class messages */
    private static final String LOGTAG = WifiStateManager.class.getPackage().getName();
    
    /** Extra name for the Wifi SSID detected when a wifi state change event is received */
    private static final String EXTRA_SSID = WifiStateManager.class.getName() + ".ssid";
    
    /** Extra name for the current Wifi name: either current SSID or BSSSID */    
    private static final String EXTRA_CURRENT_WIFI = WifiStateManager.class.getName() + ".current_wifi";
    
    /** Extra name for target wifi state if there is a change in progress */    
    private static final String EXTRA_TARGET_WIFI_STATE = WifiStateManager.class.getName() + ".target_wifi_state";    

    /** Extra name for initial wifi state if there is a change in progress */    
    private static final String EXTRA_ORIGIN_WIFI_STATE = WifiStateManager.class.getName() + ".origin_wifi_state";    
        
    /** Extra name for current wifi access point state coming from intents */
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    
    /** 
     * Determines if Wifi state is connected, disconnected or off and updates SSID, BSSID and 
     * current wifi stateData keys 
     */
    public static StateEvent getWifiState(Context context, Intent intent, Bundle stateData) {

        StateEvent result = StateEvent.OFF;
        WifiManager wifiManager = null;
        WifiInfo wifiInfo = null;
        ConnectivityManager conManager = null;
        String ssid = null;
        
        // Retrieves network info to know connection status
        NetworkInfo networkInfo = null;        
        if (intent != null && intent.hasExtra(WifiManager.EXTRA_NETWORK_INFO)) {
            networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        }
        else {
            conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (conManager != null) {
                networkInfo = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            }
        }
        
        // Retrieves wifi state to know if wifi is enabled
        int wifiState = WifiManager.WIFI_STATE_UNKNOWN;
        if (intent != null && intent.hasExtra(WifiManager.EXTRA_WIFI_STATE)) {
            wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        }
        else {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiState = wifiManager.getWifiState();
            }
        } 
                
        // If wifi is enabled and network info says state is connected
        if ((wifiState == WifiManager.WIFI_STATE_ENABLED) && (networkInfo != null) && networkInfo.isConnected()) {

            // Consider status as DISC at least
            result = StateEvent.DISC;
                                    
            // Retrieves wifi info to know connection details
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiInfo = wifiManager.getConnectionInfo();
            }

            if (wifiInfo != null) {
                ssid = wifiInfo.getSSID();                                
                if (ssid != null) {
                    ssid = getCleanSSID(ssid);
                    result = StateEvent.CON;
                }
            }
        }
        else {                       
            // If wifi state is enabled
            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                result = StateEvent.DISC;                
            }        
        }
        
        if(BuildConfig.DEBUG) {
            Log.d(LOGTAG, "WifiStateManager: WiFiState=" + wifiState + ", NetworkInfo=" + networkInfo + ", SSID=" + ssid);
        }
        
        // If there is an in progress change
        StateEvent targetState = getTargetWifiState(stateData);
        StateEvent originState = getOriginWifiState(stateData);
        
        if (targetState != null && originState != null) {
            // Clears target and origin state if state has changed from origin state
            if (result != originState) {                
                if(BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "WifiStateManager: Cleaning inflight wifi state");
                }                
                setTargetWifiState(stateData, null);
                setOriginWifiState(stateData, null);
            }
            // Otherwise return in flight target state
            else {
                result = targetState;
                if(BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "WifiStateManager: returning inflight target wifi state: " + result);
                }                
            }
        }
                
        // Updates current wifi and Ap Mode extras
        setCurrentWifi (stateData, (result == StateEvent.CON) ? ssid : null);
        
        return result;
    }

    /** 
     * Changes Wifi state to connected, disconnected or off
     */
    public static void setWifiState (Context context, StateEvent targetWifiState, Bundle stateData) {
        
        Bundle currentData = new Bundle();
        StateEvent currentWifiState = getWifiState(context, null, currentData);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        
        if ((wifiManager != null) && (currentWifiState !=  targetWifiState)) {

        	boolean inflightWifiStateChange = false;
        	
            // Disable wifi if needed
            if (targetWifiState == StateEvent.OFF) {
                if(BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "WifiStateManager: disabling wifi");
                }                                
                wifiManager.setWifiEnabled(false);
                inflightWifiStateChange = true;
            }
            
            // Enable wifi if needed
            if (currentWifiState == StateEvent.OFF) {
                if(BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "WifiStateManager: enabling wifi");
                }                                
                wifiManager.setWifiEnabled(true);
                inflightWifiStateChange = true;
            }
            
            // Disconnect current wifi is needed
            if ((currentWifiState == StateEvent.CON) && (targetWifiState == StateEvent.DISC)) {
                if(BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "WifiStateManager: disconnecting current wifi network");
                }                                                
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                wifiManager.disableNetwork((wifiInfo != null) ? wifiInfo.getNetworkId() : -1);
                inflightWifiStateChange = true;
            }
            
            // Reconnect wifi is needed
            if (targetWifiState == StateEvent.CON && stateData != null && 
                    (stateData.containsKey(EXTRA_SSID))) {

                // Get target network Id
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                int netId = -1;
                Iterator<WifiConfiguration> iterator = list.iterator();
                while (iterator.hasNext()) {
                    WifiConfiguration wifiConf = iterator.next();
                    if (wifiConf.SSID.equals(stateData.getString(EXTRA_SSID))) {
                        netId = wifiConf.networkId;
                    }
                }
                if (netId != -1) {
                    if(BuildConfig.DEBUG) {
                        Log.d(LOGTAG, "WifiStateManager: setting wifi network " + stateData.getString(EXTRA_SSID) + "enabled");
                    }                                                                    
                    wifiManager.enableNetwork(netId, false);
                    inflightWifiStateChange = true;
                }
            }
            
            // Remembers inflight wifi state change from current to target so
            // it will be considered as actual wifi state untill android confirms it has been changed.
            // As a workaround to coexist with Access Point mode management, it wont give for granted
            // that state change succeeds if Access Point is enabled as the user could cancel
            // the action requested by Wi-Fi Matic.
            if (inflightWifiStateChange && !getApModeEnabled (context, null)) {
                setOriginWifiState(stateData, currentWifiState);
                setTargetWifiState(stateData, targetWifiState);
                if(BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "WifiStateManager: Inflight wifi state change: " + currentWifiState + "-->" + targetWifiState);
                }
            }
            
        }
    }
    
    /** Checks if access point mode is enabled */
    private static boolean getApModeEnabled (Context context, Intent intent) {
		int apStatus = -1;
		
		if (intent != null && intent.hasExtra(EXTRA_WIFI_AP_STATE)) {			
           apStatus = intent.getIntExtra(EXTRA_WIFI_AP_STATE,apStatus);
		}
		else {
			try {
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				Method method = wifiManager.getClass().getMethod("getWifiApState");
				apStatus = ((Integer)method.invoke(wifiManager));
			}
			catch (Exception e) {
				Log.e(LOGTAG, Log.getStackTraceString(e));
			}
		}
		
		if (apStatus > 10) {
			apStatus = apStatus - 10;
		}
		
    	return (apStatus == 3) || (apStatus == 2);
    }
    
    /** Needed after Android 4.2 to remove quotes from SSID name */
    private static String getCleanSSID (String ssid) {
        String result = ssid;
        try {
            if (!ssid.equals(""))
                result = ssid.replace("\"", "");
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        return result;
    }
    
    /** Returns current wifi extra information from stateData bundle */    
    public static String getCurrentWifi (Bundle stateData) {
        String wifi = (stateData != null) ? stateData.getString(EXTRA_CURRENT_WIFI) : null;
        return wifi;
    }
    
    /** Sets current wifi extra information to stateData bundle */
    public static void setCurrentWifi (Bundle stateData, String currentWifi) {
        if (stateData != null) {
            stateData.putString(EXTRA_CURRENT_WIFI, currentWifi);
            stateData.putString(EXTRA_SSID, currentWifi);
        }
    }

    /** Returns target wifi state extra information from stateData bundle */    
    public static StateEvent getTargetWifiState (Bundle stateData) {
        String targetWifiState = (stateData != null) ? stateData.getString(EXTRA_TARGET_WIFI_STATE) : null;
        StateEvent result = null;
        try {
        	result = (targetWifiState != null) ? StateEvent.valueOf(targetWifiState) : null;
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }        
        return result;
    }
    
    /** Sets target wifi state  extra information to stateData bundle */
    public static void setTargetWifiState (Bundle stateData, StateEvent targetWifiState) {
        if (stateData != null) {        	        	
            if (targetWifiState != null) {
            	stateData.putString(EXTRA_TARGET_WIFI_STATE, targetWifiState.name());
            }
            else {
            	stateData.remove(EXTRA_TARGET_WIFI_STATE);
            }
        }
    }

    /** Returns origin wifi state extra information from stateData bundle */    
    public static StateEvent getOriginWifiState (Bundle stateData) {
        String originWifiState = (stateData != null) ? stateData.getString(EXTRA_ORIGIN_WIFI_STATE) : null;
        StateEvent result = null;
        try {
        	result = (originWifiState != null) ? StateEvent.valueOf(originWifiState) : null;
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }        
        return result;
    }
    
    /** Sets origin wifi state extra information to stateData bundle */
    public static void setOriginWifiState (Bundle stateData, StateEvent originWifiState) {
        if (stateData != null) {        	        	
            if (originWifiState != null) {
            	stateData.putString(EXTRA_ORIGIN_WIFI_STATE, originWifiState.name());
            }
            else {
            	stateData.remove(EXTRA_ORIGIN_WIFI_STATE);
            }
        }
    }
    
    /** Returns inflight wifi change extra information from stateData bundle, composed of 
     * origin plus target wifi states */    
    public static StateEvent[] getInflightWifiAction (Bundle stateData) {
    	StateEvent[] result = null;
    	
    	StateEvent originWifiState = getOriginWifiState(stateData);
    	StateEvent targetWifiState = getTargetWifiState(stateData);
    	
    	if (originWifiState != null && targetWifiState != null) {
    		result = new StateEvent[2];
    		result[0] = originWifiState;
    		result[1] = targetWifiState;
    	}
    	    	
    	return result;    	
    }

    
    /** Sets inflight wifi change extra information to stateData bundle, composed of 
     * origin plus target wifi states */
    public static void setInflightWifiAction (Bundle stateData, StateEvent[] action) {

    	if (stateData != null && action != null && action.length > 1) {
    		setOriginWifiState (stateData, action[0]);
    		setTargetWifiState (stateData, action[1]);
    	}
    }

}
