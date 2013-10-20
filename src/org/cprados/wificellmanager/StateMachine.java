/*
 * StateMachine.java
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

import java.util.LinkedList;
import java.util.List;

import org.cprados.wificellmanager.ui.DescribeableElement;

import android.content.res.Resources;
import android.util.Log;

/**
 * Manages state of the system, changes according to events and actions to be performed on each transition
 */
public class StateMachine {
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = StateMachine.class.getPackage().getName();

    /** Possible state change Events the state machine can handle */
    public enum StateEvent implements DescribeableElement {
        INIT, IN, OUT, UNK, CON, DISC, OFF;
        
        /** Returns state event description */
        @Override
        public String getDescription (Resources res, Object ... args) {
            String description = null;
            switch (this) {
            case INIT:
                description = res.getString(R.string.state_event_description_init);
                break;
            case IN:
                description = res.getQuantityString(R.plurals.state_event_description_in, (Integer)args[0], args[0]);
                break;
            case OUT:
                description = res.getString(R.string.state_event_description_out);
                break;
            case UNK:
                description = res.getString(R.string.state_event_description_unk);
                break;
            case CON:
                description = res.getString(R.string.state_event_description_con, args[1]);
                break;
            case DISC:
                description = res.getString(R.string.state_event_description_disc, args[1]);                
                break;
            case OFF:
                description = res.getString(R.string.state_event_description_off, args[1]);
                break;
            }
            return description;
        }
    };

    /** Possible states the system can have */
    public enum State implements DescribeableElement {

        /** Possible state values */ 
        IN_CON(StateEvent.IN, StateEvent.CON), 
        IN_DISC(StateEvent.IN,StateEvent.DISC), 
        IN_OFF(StateEvent.IN, StateEvent.OFF), 
        OUT_DISC(StateEvent.OUT, StateEvent.DISC), 
        OUT_OFF(StateEvent.OUT, StateEvent.OFF),
        OUT_CON(StateEvent.OUT, StateEvent.CON),
        UNK_DISC(StateEvent.UNK, StateEvent.DISC), 
        UNK_OFF(StateEvent.UNK, StateEvent.OFF),
        UNK_CON(StateEvent.UNK, StateEvent.CON);

        /** Cell IN, OUT or UNK part of the state */
        private StateEvent mCellState;
        
        /** Wifi CON, DISC or OFF part of the state */
        private StateEvent mWifiState;

        /** Constructs an State enumerated value instance */
        private State(StateEvent cellState, StateEvent wifiState) {
            this.mCellState = cellState;
            this.mWifiState = wifiState;
        }

        /** Tells if a state coincides with the given cell and wifi states */
        public boolean compare(StateEvent cellState, StateEvent wifiState) {
            return (this.mCellState == cellState && this.mWifiState == wifiState);
        }

        /** Returns the resulting state after moving according to event */
        public State transition(StateEvent event) {
            
            State result = this;

            if (event == StateEvent.CON || event == StateEvent.DISC || event == StateEvent.OFF)
                result = getState(this.mCellState, event);

            else if (event == StateEvent.IN || event == StateEvent.OUT || event == StateEvent.UNK)
                result = getState(event, this.mWifiState);

            return result;
        }
        
        /** Returns cell state */
        public StateEvent getCellState () {
            return mCellState;
        }
        
        /** Returns wifi state */
        public StateEvent getWifiState () {
            return mWifiState;
        }
        
        /** Returns the state value corresponding to given cell and wifi states */
        public static State getState(StateEvent cellState, StateEvent wifiState) {
            
            State result = null;
            
            for (State state : State.values())
                if (state.compare(cellState, wifiState)) {
                    result = state;
                    break;
                }

            return result;
        }
        
        /** Returns state description */
        @Override
        public String getDescription (Resources res, Object ... args) {
            
            String description = null;
            
            switch (this) {
            case IN_CON:
                description = res.getString(R.string.state_description_in_con, args[1]);
                break;
            case IN_DISC:               
                description = res.getQuantityString(R.plurals.state_description_in_disc, (Integer)args[0], args[0]);
                break;
            case IN_OFF:
                description = res.getQuantityString(R.plurals.state_description_in_off, (Integer)args[0], args[0]);
                break;
            case OUT_DISC:
                description = res.getString(R.string.state_description_out_disc);
                break;
            case OUT_OFF:
                description = res.getString(R.string.state_description_out_off);
                break;
            case OUT_CON:
                description = res.getString(R.string.state_description_out_con, args[1]);
                break;
            case UNK_DISC:
                description = res.getString(R.string.state_description_unk_disc);
                break;
            case UNK_OFF:
                description = res.getString(R.string.state_description_unk_off);
                break;
            case UNK_CON:
                description = res.getString(R.string.state_description_unk_con, args[1]);
                break;
            }
            return description;
        }
    };

    /** Possible actions to be fired after an state change */
    public enum StateAction implements DescribeableElement {
        NONE, ADD, ON, OFF, CREATE_DEFERRED_OFF, CANCEL_DEFERRED_OFF, DATA_OFF, DATA_RESTORE;
        
        /** Returns action description */
        @Override
        public String getDescription (Resources res, Object ... args) {

            String description = null;
            
            switch (this) {
            case ADD:
                description = res.getString(R.string.state_action_description_add);                
                break;
            case ON:
                description = res.getString(R.string.state_action_description_on);                
                break;
            case OFF:
                description = res.getString(R.string.state_action_description_off);            
                break;
            // Rest of the actions doesn't need an intuitive description
            default:
                description = this.name();
                break;
            }

            return description;
        }
        
        public boolean isDeactivable () {
            return (this == ADD || this == ON || this == OFF);
        }
    };
        
    /** List of actions to be done next determined in last state transitions */
    private List<StateAction> mNextActions = null;

    /** Current state of the state machine */
    private State mCurrentState;
    
    /** Last event processed */
    private StateEvent lastEvent = null;

    /** Constructs the state machine initialized to the specified states */
    public StateMachine(StateEvent cellState, StateEvent wifiState) {
        mCurrentState = State.getState((cellState == null) ? StateEvent.UNK : cellState, wifiState);
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "StateMachine: Initial state (obtained): " + mCurrentState);
        }
    }
    
    /** Constructs the state machine initialized to the specified state */
    public StateMachine(State state) {
        mCurrentState = state;
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "StateMachine: Initial state (loaded): " + mCurrentState); 
        }
    }

    /** Manages state changes and returns corresponding actions */
    public List<StateAction> manageStateChange(StateEvent stateEvent, int startId) {

        State nextState = mCurrentState.transition(stateEvent);
        List<StateAction> nextActions = new LinkedList<StateAction>();

        // General state changes actions
        manageGeneralStateChange (stateEvent, nextActions);

        // Deferred off handling based just on Wifi state changes
        manageWifiStateChange (stateEvent, nextActions);
        
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "StateMachine(" + startId + "): Event: " + stateEvent + 
                    "; State: " + mCurrentState + "-->" + nextState + "; Action: " + nextActions);
        }
        
        lastEvent = stateEvent;
        mCurrentState = nextState;
        return (mNextActions = nextActions);
    }
    
    /** Manages cell & wifi state changes */
    private List<StateAction> manageGeneralStateChange(StateEvent stateEvent, List<StateAction> nextActions) {

        // General state changes actions
        switch (mCurrentState) {

        case IN_CON:
            if (stateEvent == StateEvent.OUT) {
                nextActions.add(StateAction.ADD);
            }
            break;

        case IN_DISC:
            if (stateEvent == StateEvent.OUT) {
                nextActions.add(StateAction.OFF);
            }
            else if (stateEvent == StateEvent.CON) {
                nextActions.add(StateAction.ADD);                
            }
            break;

        case IN_OFF:
            if (stateEvent == StateEvent.INIT) {
                nextActions.add(StateAction.ON);
            }
            else if (stateEvent == StateEvent.CON) {
                nextActions.add(StateAction.ADD);
            }
            break;

        case OUT_CON:
            if (stateEvent == StateEvent.INIT) {
                nextActions.add(StateAction.ADD);
            }
            break;
            
        case OUT_DISC:
            if (stateEvent == StateEvent.INIT) {
                nextActions.add(StateAction.OFF);
            }
            else if (stateEvent == StateEvent.CON) {
                nextActions.add(StateAction.ADD);
            }
            break;

        case OUT_OFF:
            if (stateEvent == StateEvent.CON) {
                nextActions.add(StateAction.ADD);
            }
            else if (stateEvent == StateEvent.IN) {
                nextActions.add(StateAction.ON);
            }
            else if (stateEvent == StateEvent.UNK) {
                // v1.3.4: Action later filtered out if unk location activates preference is not set 
                nextActions.add(StateAction.ON);
            }
            break;
            
        case UNK_CON:
            if (stateEvent == StateEvent.OUT){
                nextActions.add(StateAction.ADD);
            }
            break;

        case UNK_DISC:
            if (stateEvent == StateEvent.OUT){
                nextActions.add(StateAction.OFF);
            }
            break;

        case UNK_OFF:
            if (stateEvent == StateEvent.IN){
                nextActions.add(StateAction.ON);
            }
            else if (stateEvent == StateEvent.INIT) {
                // v1.3.4: Action later filtered out if unk location activates preference is not set 
                nextActions.add(StateAction.ON);                
            }
            break;
        }
        
        return (nextActions);
    }

    /** Manages changes based just on Wifi state */
    private List<StateAction> manageWifiStateChange (StateEvent stateEvent, List<StateAction> nextActions) {

        // Deferred off handling based just on Wifi state changes
        switch (mCurrentState.mWifiState) {
            case CON:
                if (stateEvent == StateEvent.DISC) {
                    nextActions.add(StateAction.CREATE_DEFERRED_OFF);
                    nextActions.add(StateAction.DATA_RESTORE);
                }
                else if (stateEvent == StateEvent.OFF) {
                    nextActions.add(StateAction.DATA_RESTORE);
                }
                else if (stateEvent == StateEvent.INIT) {
                    nextActions.add(StateAction.DATA_OFF);
                }
                break;
                
            case DISC:
                if (stateEvent == StateEvent.CON) {
                    nextActions.add(StateAction.CANCEL_DEFERRED_OFF);   
                    nextActions.add(StateAction.DATA_OFF);
                }
                else if (stateEvent == StateEvent.OFF) {
                    nextActions.add(StateAction.CANCEL_DEFERRED_OFF);   
                }
                else if (stateEvent == StateEvent.INIT) {
                    nextActions.add(StateAction.DATA_RESTORE);
                }
                break;
            
            case OFF:
                if (stateEvent == StateEvent.CON) {
                    nextActions.add(StateAction.DATA_OFF);
                }
                break;
                
            default:
            	break;
        }

        return (nextActions);
    }
        
    /** Returns next action to be done depending on last state change */
    public List<StateAction> getNextActions() {
        return mNextActions;
    }

    /** Returns current State */
    public State getCurrentState() {
        return mCurrentState;
    }
    
    /** Returns last event delivered to state machine or null if machine has just been initialized */
    public StateEvent getLastEvent () {
        return lastEvent;
    }
}
