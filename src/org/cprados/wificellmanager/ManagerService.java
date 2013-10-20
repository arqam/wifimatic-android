/*
 * ManagerService.java
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

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cprados.wificellmanager.RequestedActionManager.RequestedAction;
import org.cprados.wificellmanager.StateMachine.State;
import org.cprados.wificellmanager.StateMachine.StateAction;
import org.cprados.wificellmanager.StateMachine.StateEvent;
import org.cprados.wificellmanager.sys.AuditTrailManager;
import org.cprados.wificellmanager.sys.AuditTrailManager.ActivityRecord;
import org.cprados.wificellmanager.sys.CellStateListener;
import org.cprados.wificellmanager.sys.CellStateManager;
import org.cprados.wificellmanager.sys.MobileDataManager;
import org.cprados.wificellmanager.sys.NotificationManager;
import org.cprados.wificellmanager.sys.WakeLockManager;
import org.cprados.wificellmanager.sys.WifiStateManager;
import org.cprados.wificellmanager.ui.DescribeableElement;
import org.cprados.wificellmanager.ui.Preferences;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Main WifiCellManager application service. Receives events determines state changes and perform actions according to
 * the state machine rules
 */
public class ManagerService extends Service {
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = ManagerService.class.getPackage().getName();

    /** Action of the intents sent to the service to make initial actions */
    public static final String INIT_ACTION = ManagerService.class.getName() + ".init";
        
    /** The state machine holding current state of the service and determines actions to be performed on each event */
    private StateMachine mStateMachine;

    /** Bundle object containing current state related information */
    private Bundle mStateData;
    
    /** Handles service creation */
    @Override
    public void onCreate(){
        if (BuildConfig.DEBUG){
            Log.d(LOGTAG, "ManagerService: Creating Service");
        }

        // Constructs the state machine and state data
        if (mStateMachine == null) {
            
            // Ties to load current state and state data from saved data
            if (loadState()) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "ManagerService: Spawn initial Cell Change event");
                }
                forwardEvent (this, CellStateManager.CELL_CHANGE_ACTION, null);
            }
            
            // Initializes the state machine and state data from current device state
            else {
                StateEvent cellState = CellStateManager.getCellState(this, null, mStateData);
                StateEvent wifiState = WifiStateManager.getWifiState(this, null, mStateData);
                mStateMachine = new StateMachine(cellState, wifiState);                
            }
        }
        
        // Setups callbacks that will start the service according to user preferences
        subscribeService(true);
   }

    /** Handles service deletion */
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "ManagerService: Destroying Service");
        }

        // Cleans the callback that starts the service
        subscribeService(false);

        // Cleans state machine and state data 
        mStateMachine = null;
        mStateData = null;

        // Clears state and refreshes preferences UI
        saveState();        
    }

    /** Configures the service to be called back according to user preferences */
    private void subscribeService(boolean enable) {

        // Setup the receiver to listen events 
        EventReceiver.activateReceiver(this, enable);
        
        // Requests periodic events from the Event Receiver to refresh cell location
        EventReceiver.requestPeriodicEvents(getApplicationContext(), null, DataManager.getFrequency(this)* 60000, CellStateManager.CELL_CHANGE_ACTION, null, enable);

        // Requests events from the cell change listener 
        CellStateListener.requestCellChangeEvents(getApplicationContext(), CellStateManager.CELL_CHANGE_ACTION, enable);

        // Schedules daily disable and enable Wi-Fi events
        boolean intervalEnabled = DataManager.getTimeIntervalEnabled(this);
        int[] begin = DataManager.getTimeIntervalBegin(this);
        int[] end = DataManager.getTimeIntervalEnd(this);
        
        // Checks if time interval is enabled and begin different from end or disabling
        if ((intervalEnabled && (begin!=null) && (begin.length>1) && (end!=null) && (end.length>1) && 
                ((begin[0] != end[0]) || (begin[1] != end[1]))) || 
                !enable) {
            
            // Setup daily scheduled off explicit request event
            EventReceiver.requestPeriodicEvents(getApplicationContext(), begin, 
                    AlarmManager.INTERVAL_DAY, RequestedActionManager.EXPLICIT_ACTION_REQ + RequestedAction.SCHEDULED_OFF, 
                    RequestedActionManager.createRequestedAction(RequestedAction.SCHEDULED_OFF), enable);
            
            // Setup daily scheduled on explicit request event
            EventReceiver.requestPeriodicEvents(getApplicationContext(), end, 
                    AlarmManager.INTERVAL_DAY, RequestedActionManager.EXPLICIT_ACTION_REQ + RequestedAction.SCHEDULED_ON, 
                    RequestedActionManager.createRequestedAction(RequestedAction.SCHEDULED_ON), enable);
        }
        
        // In case of being stopped not from the UI set correct state 
        if (DataManager.getActivate(this) != enable) {
            DataManager.setActivate(this, enable);
        }        
    }

    /** Handles events sent to the service */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        try {
            State initialState = mStateMachine.getCurrentState();
            Date intentDate = new Date();
            
            // Builds action plan for received intent and updates state accordingly
            List<StateAction> actionPlan = buildPlan(intent, mStateMachine, mStateData, startId);
            
            // Gets explicitly requested action from the intent
            RequestedAction requestedAction = RequestedActionManager.getRequestedAction(intent);

            // Perform the actions determined
            performPlan(actionPlan, requestedAction, intentDate);
            
            // Saves activity record to audit trail 
            recordActivity (initialState,  mStateMachine.getCurrentState(), actionPlan, requestedAction, intentDate, mStateData);

            // Saves current state and refreshes preferences UI
            saveState();
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
        finally {
            // Releases wake lock
            WakeLockManager.getWakeLockManager().releaseWakeLock();
        }
        
        return START_STICKY;
    }
    
    /** Analyzes the intent received, updates the current state and builds action plan to process it  */
    private List<StateAction> buildPlan(Intent intent, StateMachine stateMachine, Bundle stateData, int startId) {

        List<StateAction> actionPlan = null;
        
        // Service is being restarted after its process has gone away            
        if (intent == null) {
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "ManagerService(" + startId + "): Service restarted after process has gone away");
            }
        }

        else {                        
            // Determine the event received and the action            
            String intentAction = intent.getAction();

            // Service is being restarted by user
            if (intentAction == null) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "ManagerService(" + startId + "): Service restarted by user action");
                }
            }

            // Service is being started by user for first time
            else if (intentAction.equals(INIT_ACTION)) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "ManagerService(" + startId + "): Service initialized by user");
                }
                actionPlan = validatePlan(stateMachine.manageStateChange(StateEvent.INIT, startId));
            }

            // Cell change event has been received
            else if (intentAction.equals(CellStateManager.CELL_CHANGE_ACTION)) {
                actionPlan = validatePlan(stateMachine.manageStateChange(WifiStateManager.getWifiState(this, intent, stateData), startId));
                actionPlan.addAll(validatePlan(stateMachine.manageStateChange(CellStateManager.getCellState(this, intent, stateData), startId)));
            }

            // Wifi state change event has been received
            else if (intentAction.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    || intentAction.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                actionPlan = validatePlan(stateMachine.manageStateChange(CellStateManager.getCellState(this, intent, stateData), startId));
                actionPlan.addAll(validatePlan(stateMachine.manageStateChange(WifiStateManager.getWifiState(this, intent, stateData), startId)));
            }

            // Explicit action request event received
            else if (intentAction.startsWith(RequestedActionManager.EXPLICIT_ACTION_REQ)) {

                // Refresh state and retrieve requested action from intent
                actionPlan = validatePlan(stateMachine.manageStateChange(CellStateManager.getCellState(this, intent, stateData), startId));
                actionPlan.add(validateAction(RequestedActionManager.getStateAction(this, intent, stateMachine.getCurrentState())));
            }

            // Other events are discarded
            else {
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "ManagerService(" + startId + "): Unhandled event received: " + intentAction);
                }
            }                        
        }
        
        return (actionPlan);
    }
    
    /** Checks whether an action is inhibited according to settings for current state */
    private StateAction validateAction(StateAction action) {

        boolean enabled = true;
        switch (action) {

        case ON:
            // Validate time interval 
            if (DataManager.getTimeIntervalEnabled(this)) {

                // Checks if current hour is outside disable wifi hours interval
                int[] beginString = DataManager.getTimeIntervalBegin(this);
                int[] endString = DataManager.getTimeIntervalEnd(this);

                if (beginString != null && beginString.length > 1 && endString != null && endString.length > 1) {
                    int begin = beginString[0] * 60 + beginString[1];
                    int end = endString[0] * 60 + endString[1];
                    Date nowDate = new Date();
                    int now = nowDate.getHours() * 60 + nowDate.getMinutes();

                    if (end >= begin)
                        enabled = (now < begin) || (now >= end);
                    else
                        enabled = (now < begin) && (now >= end);
                }
            }
            
            // Validates if turning on wifi on unknown locations behavior is active
            if ((mStateMachine.getCurrentState().getCellState() == StateEvent.UNK)) {
                enabled &= DataManager.getUnkLocationActivates(this);
            }
                        
        case OFF:
            // Checks if ON action is enabled for any wifi in this cell and 
            // checks if OFF action was enabled for any wifi in the last cell where status was IN
            enabled &= CellStateManager.getActionEnabled(mStateData, action);
            break;

        case ADD:
            // Checks if wifi is new and add wifis is enabled or 
            // wifi is existant its ADD locations preference is enabled
            String wifi = WifiStateManager.getCurrentWifi(mStateData);
            enabled &= (!DataManager.isExistantWifi(this, wifi) && DataManager.getAddWifis(this) || DataManager.isExistantWifi(this, wifi)
                    && DataManager.getWifiAction(this, StateAction.ADD, wifi));
                    
            // An unknow or dissabled cell is not added
            //enabled &= DataManager.getCellEnabled(this, CellStateManager.getCid(mStateData), CellStateManager.getLac(mStateData));
            enabled &= (CellStateManager.getCid(mStateData) >= CellStateManager.CELL_UNKNOWN)
                    && (CellStateManager.getLac(mStateData) >= CellStateManager.CELL_UNKNOWN);
            break;
            
        case CREATE_DEFERRED_OFF:
        case CANCEL_DEFERRED_OFF:
            enabled = (DataManager.getOffAfterDiscTimeout(this) != 0);
            break;
        
        case DATA_OFF:
            // Turns off mobile data only if mobile data management is enabled and if wifi is new or not disabled by user
            wifi = WifiStateManager.getCurrentWifi(mStateData);
            enabled = (DataManager.getMobileDataManaged(this)) && (!DataManager.isExistantWifi(this, wifi) || DataManager.getWifiEnabled(this, wifi));                        
            break;

        case DATA_RESTORE:
            // Restores mobile data only if mobile data management is enabled
            enabled = DataManager.getMobileDataManaged(this);            
            break;

        default:
        	break;

        }

        return (enabled ? action : StateAction.NONE);
    }
    
    /** Checks whether any action of the list is inhibited according to settings for current state */
    private List<StateAction> validatePlan(List<StateAction> actions) {
    
        if (actions != null) {            
            // Validates each action of the plan
            for (Iterator<StateAction> iterator = actions.iterator(); iterator.hasNext();) {                
                // Removes invalidated actions from the plan
                if (validateAction(iterator.next()) == StateAction.NONE) {
                    iterator.remove();
                }
            }
        }        
        return (actions);
    }
        
    /** Performs an action of the plan */
    private boolean performAction(StateAction action, RequestedAction requestedAction, Date date) {
        boolean result = false;        
        
        // Performs required action
        switch (action) {

        // Handles enable wifi action
        case ON:
            // Turns wifi on
            WifiStateManager.setWifiState(this, StateEvent.DISC, mStateData);

            // Checks if it is an explicitly requested action or if it is caused by an state change
            DescribeableElement cause = requestedAction == null ? mStateMachine.getCurrentState().getCellState() : requestedAction;
                                    
            // Puts the notification in the notifications bar            
            NotificationManager.notifyAction(this, StateAction.ON, cause, date, mStateData);
            result = true;
            break;

        // Handles switch off wifi action
        case OFF:

            // Turn wifi off
            WifiStateManager.setWifiState(this, StateEvent.OFF, mStateData);
            
            // Checks if it is an explicitly requested action or if it is caused by an state change
            cause = requestedAction == null ? mStateMachine.getCurrentState().getCellState(): requestedAction;

            // Puts the notification in the notifications bar
            NotificationManager.notifyAction(this, StateAction.OFF, cause, date, mStateData);
            result = true;
            break;

        // Handles add wifi cell action
        case ADD:

            String wifi = WifiStateManager.getCurrentWifi(mStateData);
            int cid = CellStateManager.getCid(mStateData);
            int lac = CellStateManager.getLac(mStateData);

            // Adds the wifi cell association if it did not exist before
            if (DataManager.addWifiCell(this, wifi, cid, lac)) {

                // Puts the notification in the notifications bar
                NotificationManager.notifyAction(this, StateAction.ADD, mStateMachine.getCurrentState().getWifiState(), date, mStateData);
                
                // Refresh cell state on change of cell data by an ADD action
                forwardEvent(this, CellStateManager.CELL_CHANGE_ACTION, null); 
                result = true;
            }
            break;
        
        // Handles deferred off action
        case CREATE_DEFERRED_OFF:
            
            // Schedule a requested action to execute deferred off
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, DataManager.getOffAfterDiscTimeout(this));            
            EventReceiver.requestEvent(getApplicationContext(), calendar.getTime(), 
            		RequestedActionManager.EXPLICIT_ACTION_REQ + RequestedAction.DEFERRED_OFF, 
                    RequestedActionManager.createRequestedAction(RequestedAction.DEFERRED_OFF), true);
            result = true;
            break;
        
        // Handles cancel deferred off action
        case CANCEL_DEFERRED_OFF:
            
            // Cancel any requested action to execute deferred off
            EventReceiver.requestEvent(getApplicationContext(), null, 
            		RequestedActionManager.EXPLICIT_ACTION_REQ + RequestedAction.DEFERRED_OFF, 
                    RequestedActionManager.createRequestedAction(RequestedAction.DEFERRED_OFF), false);            
            result = true;
            break;        

        // Handles mobile data on and off actions
        case DATA_OFF:        
        case DATA_RESTORE:
            
            // Turns off mobile data
            MobileDataManager.setMobileDataState(getApplicationContext(), action, mStateData);            
            result = true;
            break;
        
        default:
        	break;
        }        

        return result;
    }
    
    /** Performs an action plan  */
    private List<StateAction> performPlan(List<StateAction> actions, RequestedAction requestedAction, Date date) {
        
        if (actions != null) {            
            // Perform each action of the plan in order
            for (Iterator<StateAction> iterator = actions.iterator(); iterator.hasNext();) {                
                StateAction action = iterator.next();
                // Removes any action from the plan that is not executed
                if (!performAction(action, requestedAction, date)) {
                    iterator.remove();
                }
            }
        }        
        return actions;
    }
    
    /** Saves an activity record to audit trail */
    private void recordActivity(State initialState, State finalState, List<StateAction> actionPlan, RequestedAction requestedAction,
            Date date, Bundle stateData) {

        if (DataManager.getHasDonated(this, false)) {
            ActivityRecord record = null;

            // It only records one action at a time. Priority is On, Off, Add. Otherwise no action is recorded
            StateAction action = null;
            if (actionPlan != null) {
                action = actionPlan.contains(StateAction.ON) ? StateAction.ON : actionPlan.contains(StateAction.OFF) ? StateAction.OFF
                        : actionPlan.contains(StateAction.ADD) ? StateAction.ADD : null;
            }

            // Check if there is state change or a explicitly requested action being done
            if (initialState != finalState || action != null) {

                // Creates an activity record. Action and requested action might be null, but there must be 
                // an state change or an explicitly requested action
                Object[] args = { CellStateManager.getNearbyWifis(stateData), WifiStateManager.getCurrentWifi(stateData),
                        DataManager.getOffAfterDiscTimeout(this) };

                record = new ActivityRecord(finalState, action, requestedAction, date, args);
            }

            // Records activity record
            if (record != null) {
                AuditTrailManager.getInstance(getApplicationContext()).writeRecord(record);
            }
        }
    }

    /** Saves current state */
    private void saveState () {

        // Saves currentState of state machine
        DataManager.setState(this, mStateMachine != null ? mStateMachine.getCurrentState() : null);
                            
        // Saves current cell state data
        DataManager.setCurrentCell(this, CellStateManager.getCid(mStateData), CellStateManager.getLac(mStateData));
        
        // Saves actions enabled state data
        DataManager.setCurrentAction(this, StateAction.ON, CellStateManager.getActionEnabled(mStateData, StateAction.ON));
        DataManager.setCurrentAction(this, StateAction.OFF, CellStateManager.getActionEnabled(mStateData,StateAction.OFF));

        // Saves current wifi state data
        DataManager.setCurrentWifi(this, WifiStateManager.getCurrentWifi(mStateData));
        
        // Saves any pending mobile data action
        DataManager.setPendingMobileDataAction(this, MobileDataManager.getPendingMobileDataAction(mStateData));
        
        // Saves any inflight wifi state change action
        DataManager.setInflightWifiAction(this, WifiStateManager.getInflightWifiAction(mStateData));
                
        // Refreshes preferences UI
        Preferences.requestRefresh(this);
    }
    
    /** Loads state and state data from data manager and returns if it was available */
    private boolean loadState() {
        
        boolean result = false;

        // Initializes state data bundle
        if (mStateData == null)
            mStateData = new Bundle();
        
        // Constructs state machine already initialized to saved state
        StateMachine.State savedState = DataManager.getState(this);
        
        if (result = (savedState != null)) {
            mStateMachine = new StateMachine(savedState);

            // Loads current cell state data
            int[] currentCell = DataManager.getCurrentCell(this);
            CellStateManager.setCurrentCell(mStateData, currentCell);

            // Loads current number of nearby wifis state data
            Set<String> currentWifis = DataManager.getWifisByCell(this, currentCell[0], currentCell[1]);
            CellStateManager.setNearbyWifis(mStateData, currentWifis != null ? currentWifis.size() : 0);

            // Load actions enabled state data
            CellStateManager.setActionEnabled(mStateData, StateAction.ON, DataManager.getCurrentAction(this, StateAction.ON));
            CellStateManager.setActionEnabled(mStateData, StateAction.OFF, DataManager.getCurrentAction(this, StateAction.OFF));

            // Loads current wifi state data
            WifiStateManager.setCurrentWifi(mStateData, DataManager.getCurrentWifi(this));
            
            // Loads any pending mobile data action state data
            MobileDataManager.setPendingMobileDataAction(mStateData, DataManager.getPendingMobileDataAction(this));            

            // Loads any inflight wifi state change action
            WifiStateManager.setInflightWifiAction(mStateData, DataManager.getInflightWifiAction(this));            
        }
        
        return result;
    }
 
    /** Send an event to the manager service with the required action and intent */
    public static void forwardEvent (Context context, String action, Intent intent) {

        if (DataManager.getActivate(context)) {
            
            // Creates and populates the intent 
            if (intent == null) {
                intent = new Intent(context, ManagerService.class); 
            }
            else {
                intent.setClass(context, ManagerService.class);
            }

            // Sets the action
            if (action != null) {
                intent.setAction(action);
            }
            
            // Spawns the manager service 
            context.startService(intent);
        }
    }
    
    /** Handles service bind, returns the service binder to send synchronous events */
    @Override
    public IBinder onBind(Intent intent) {
        return new ManagerServiceBinder();
    }
    
    /** Binder that allows to send events synchronously to the service */
    public class ManagerServiceBinder extends Binder {
        
        /** Synchronously sends an even to the service */
        public void syncForwardEvent (Context context, String action, Intent intent) {

            // Creates and populates the intent 
            if (intent == null) {
                intent = new Intent(context, ManagerService.class); 
            }
            else {
                intent.setClass(context, ManagerService.class);
            }

            // Sets the action
            if (action != null) {
                intent.setAction(action);
            }

            // Forwards to onStartCommand and returns when finished
            onStartCommand(intent, 0, 0);            
        }        
    }        
}
