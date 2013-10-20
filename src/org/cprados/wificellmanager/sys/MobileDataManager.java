/*
 * MobileDataManager.java
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.cprados.wificellmanager.StateMachine.StateAction;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;

/** Manages changes in mobile data connectivity state of the system */
public class MobileDataManager {

    /** Tag for logging this class messages */
    private static final String LOGTAG = MobileDataManager.class.getPackage().getName();
    
    /** Extra name for flag that indicates if any action on mobile data was made */
    private static final String EXTRA_PENDING_MOBILE_DATA_ACTION = WifiStateManager.class.getName() + ".pending_mobile_data_action";
    
    /**
     * Turns off or restores mobile data. Sets pending mobile data action flag to true if it actually turned mobile data off. Clears
     * pending mobile data action flag if it restored mobile data to original state
     */
    public static void setMobileDataState(Context context, StateAction targetState, Bundle stateData) {

        if (stateData != null && (targetState == StateAction.DATA_OFF || targetState == StateAction.DATA_RESTORE)) {

            if (targetState == StateAction.DATA_RESTORE && getPendingMobileDataAction(stateData)) { 

                // Turns on mobile data if it is off
                if (!getMobileDataState(context)) {
                    setMobileDataState (context, true);
                }
                
                // Clears pending mobile data action flag.
                setPendingMobileDataAction(stateData, false);   
            }
            
            else if (targetState == StateAction.DATA_OFF) {

                // Turns off mobile data if it is on
                if (getMobileDataState(context)) {                    
                    if (setMobileDataState (context, false)) {
                        
                        // Only if it was Wi-Fi Matic who turned mobile data off, set mobile data action flag to true
                        // so it will remind to turn it back on later
                        setPendingMobileDataAction(stateData, true);
                    }
                }                
            }
        }
    }

    /** 
     * Checks if mobile data is on or off. Returns true if it is on, false if it is off.
     */
    private static boolean getMobileDataState(Context context) {
        boolean status = false;
        
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);           
            Method method = connectivityManager.getClass().getMethod("getMobileDataEnabled");
            status = (Boolean) method.invoke(connectivityManager);
        } catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        
        return status;
    }
    
    /**
     * Sets Mobile data on or off. Status true turns on, false turns off. Returns true if action succeeded, false otherwise
     */
    private static boolean setMobileDataState(Context context, boolean status) {
        boolean result = false;        
        try {
            // Uses reflection to get access to setMobileDataEnabled method
            final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class<?> conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled",
                    Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            
            // Sets mobile data enabed to target state
            setMobileDataEnabledMethod.invoke(iConnectivityManager, status);
            result = true;
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        return result;
    }
    
    /** Returns if there is a pending mobile data action stored in state data bundle */    
    public static boolean getPendingMobileDataAction (Bundle stateData) {
        boolean result = false;
        result = (stateData != null) ? stateData.getBoolean(EXTRA_PENDING_MOBILE_DATA_ACTION) : false;
        return result;
    }
    
    /** Sets or clears a pending mobile data action to state data bundle */
    public static void setPendingMobileDataAction (Bundle stateData, boolean status) {
        if (stateData != null) {
            if (status) {
                stateData.putBoolean(EXTRA_PENDING_MOBILE_DATA_ACTION, true);
            }
            else {
                stateData.remove(EXTRA_PENDING_MOBILE_DATA_ACTION);
            }
        }
    }
}
