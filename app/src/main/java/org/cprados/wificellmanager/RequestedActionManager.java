/*
 * RequestedActionManager.java 
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


import org.cprados.wificellmanager.StateMachine.State;
import org.cprados.wificellmanager.StateMachine.StateAction;
import org.cprados.wificellmanager.StateMachine.StateEvent;
import org.cprados.wificellmanager.ui.DescribeableElement;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

/** Handler of intents sent to the Manager Service to request it explicitly to an action */
public class RequestedActionManager {
    
    /** Extra name for the action requested in and event */
    private static final String EXTRA_ACTION = RequestedActionManager.class.getName() + ".action";
    
    /** Initiator of actions of the intents sent to the service for explicit action requests*/
    public static final String EXPLICIT_ACTION_REQ = ManagerService.class.getName() + ".explicit_action_req_";
    
    /** Action requests handled by the Manager Service */
    public enum RequestedAction implements DescribeableElement {
        SCHEDULED_OFF, SCHEDULED_ON, DEFERRED_OFF;
    
        /** Returns the requested action description */
        @Override
        public String getDescription (Resources res, Object...args) {
            
            String description = null;
            
            switch (this) {
            case SCHEDULED_OFF:
                description = res.getString(R.string.requested_action_scheduled_off);
                break;
            case SCHEDULED_ON:
                description = res.getString(R.string.requested_action_scheduled_on);
                break;
            case DEFERRED_OFF:
                description = res.getString(R.string.requested_action_deferred_off,args[2]);
                break;
            }
            
            return description;
        }
    };
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = RequestedActionManager.class.getPackage().getName();
                
    /**
     * Determines requested state action in the intent
     */
    public static StateAction getStateAction (Context context, Intent intent, State currentState) {
        
        StateAction result = StateAction.NONE;
        
        if (intent != null) {
        
            String extraAction = intent.getStringExtra(EXTRA_ACTION);
            
            if (extraAction != null) {             
        
                // Scheduled or deferred disable wifi event received
                if (extraAction.equals(RequestedAction.SCHEDULED_OFF.toString()) || 
                        extraAction.equals(RequestedAction.DEFERRED_OFF.toString())) {

                    // Action is turn off wifi if state is not off
                    StateEvent wifiState = currentState.getWifiState();
                    if (wifiState != StateEvent.OFF) {
                        result = StateAction.OFF;      
                    }
                }

                // Scheduled enable wifi event received
                else if (extraAction.equals(RequestedAction.SCHEDULED_ON.toString())) {
                    
                    // Action is turn wifi on if there are nearby wifis and it is off
                    StateEvent cellState = currentState.getCellState();
                    StateEvent wifiState = currentState.getWifiState();
                    if (cellState == StateEvent.IN && wifiState == StateEvent.OFF) {                    
                        result = StateAction.ON;
                    }
                }
            }
        }
        
        Log.d(LOGTAG, "RequestedActionManager: Action Requested: " + result);
    
        return (result);        
    }    
    
    /** Builds an explicit requested event intent */
    public static Intent createRequestedAction (RequestedAction requestedAction) {

        Intent result = new Intent();
        
        if (result != null) {
            result.putExtra(EXTRA_ACTION, requestedAction.toString());
        }
        
        return (result);        
    }
    
    /** Return the explicit requested action from an intent */
    public static RequestedAction getRequestedAction(Intent intent) {

        RequestedAction result = null;

        if (intent != null) {
            String extraAction = intent.getStringExtra(EXTRA_ACTION);
            if (extraAction != null) {
                try {
                    result = RequestedAction.valueOf(extraAction);
                }
                catch (Exception e) {
                    Log.e(LOGTAG, Log.getStackTraceString(e));
                }
            }
        }

        return result;
    }
}
