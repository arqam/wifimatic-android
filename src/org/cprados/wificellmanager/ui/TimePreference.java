/*
 * TimePreference.java
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

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
 
/** 
 * Preference to select time of date. Saves selected the value in HH:MM date, but displays the selected value 
 * in 24h or 12h format depending on system wide setting
 */
public class TimePreference extends DialogPreference { 
    
    /** Current hour loaded or saved by this preference */
    protected int mHour=0; 

    /** Current minute loaded or saved by this preference */
    protected int mMinute=0; 
    
    /** Whether time format of the system was set to 24h when this preference was instantiated */
    protected boolean m24HourFormat; 
    
    /** Time Picker View of the alert dialog shown by this preference */
    protected TimePicker mTimePicker; 
    
    /** Time displayed Text View of this preference that is shown in the Preferences Activity */
    protected TextView mTimeDisplay; 
 
    public TimePreference(Context ctxt, AttributeSet attrs) { 
        super(ctxt, attrs); 
        m24HourFormat = DateFormat.is24HourFormat(ctxt); 
    }
 
    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) { 
        super(ctxt, attrs, defStyle); 
        m24HourFormat = DateFormat.is24HourFormat(ctxt); 
    } 
 
    /** Creates the String representation of this preference internal state */
    @Override 
    public String toString() { 
        if(m24HourFormat) { 
            return ((mHour < 10) ? "0" : "") 
                    + Integer.toString(mHour) 
                    + ":" + ((mMinute < 10) ? "0" : "") 
                    + Integer.toString(mMinute); 
        } else { 
            int myHour = mHour % 12; 
            return ((myHour == 0) ? "12" : ((myHour < 10) ? "0" : "") + Integer.toString(myHour)) 
                    + ":" + ((mMinute < 10) ? "0" : "")  
                    + Integer.toString(mMinute)  
                    + ((mHour >= 12) ? " PM" : " AM"); 
        } 
    } 
 

    /** Creates the time picker view of the dialog */
    @Override 
    protected View onCreateDialogView() { 
        mTimePicker=new TimePicker(getContext().getApplicationContext());
        return(mTimePicker); 
    } 
    
    /** Initializes time picker dialog view with this preference internal state data */
    @Override 
    protected void onBindDialogView(View v) { 
        super.onBindDialogView(v);
        initializeDialogView(null);
    } 
    
    /** Initializes time picker dialog view with initial values or state data that is passed */
    private void initializeDialogView (TimeSavedState state) {
        if (mTimePicker != null) {
            mTimePicker.setIs24HourView(m24HourFormat); 
            if(state == null) {            
                mTimePicker.setCurrentHour(mHour); 
                mTimePicker.setCurrentMinute(mMinute);
            }
            else {
                mTimePicker.setCurrentHour(state.currentHour); 
                mTimePicker.setCurrentMinute(state.currentMinute);                
            }
        }
    }
 
    /** Loads the currently selected time in this preferences widget frame, as a text */
    @Override
    public void onBindView(View view) {
        View widgetLayout;
        //int childCounter = 0;
        widgetLayout = view.findViewById(android.R.id.widget_frame);

        //do { 
        //    widgetLayout = ((ViewGroup) view).getChildAt(childCounter); 
        //    childCounter++; 
        //} while (widgetLayout.getId() != android.R.id.widget_frame);  

        if (widgetLayout != null && widgetLayout instanceof ViewGroup) {
            ((ViewGroup) widgetLayout).removeAllViews();
            // Creates the text view with the selected hour
            mTimeDisplay = new TextView(widgetLayout.getContext());
            mTimeDisplay.setText(toString());
            
            // Sets the color
            mTimeDisplay.setTextColor(MyCheckBoxPreference.getSummaryColor());
                        
            // Adds the Text View to the widget frame
            ((ViewGroup) widgetLayout).addView(mTimeDisplay);
            
            // This line fixed the visibility issue
            widgetLayout.setVisibility(View.VISIBLE);
        }
        super.onBindView(view);
    }

    /** Saves the data to the Shared Preference as a String in h:m format */
    @Override 
    protected void onDialogClosed(boolean positiveResult) { 
        super.onDialogClosed(positiveResult); 
 
        if (positiveResult) { 
            mTimePicker.clearFocus(); 
            mHour=mTimePicker.getCurrentHour(); 
            mMinute=mTimePicker.getCurrentMinute(); 
            
            String time=String.valueOf(mHour)+":"+String.valueOf(mMinute); 
 
            if (callChangeListener(time)) { 
                persistString(time); 
                mTimeDisplay.setText(toString()); 
            } 
        } 
    } 
 
    @Override 
    protected Object onGetDefaultValue(TypedArray a, int index) { 
        return(a.getString(index)); 
    } 
 
    /** Obtains initial state for this preference */
    @Override 
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) { 
        String time=null; 
 
        if (restoreValue) { 
            if (defaultValue==null) { 
                time=getPersistedString("00:00"); 
            } 
            else { 
                time=getPersistedString(defaultValue.toString()); 
            } 
        } 
        else { 
            if (defaultValue==null) { 
                time="00:00"; 
            } 
            else { 
                time=defaultValue.toString(); 
            } 
            if (shouldPersist()) { 
                persistString(time); 
            } 
        } 
 
        String[] timeParts=time.split(":"); 
        mHour=Integer.parseInt(timeParts[0]); 
        mMinute=Integer.parseInt(timeParts[1]);; 
    } 
    
    /** Returns current state of this preference and the time picker dialog if shown */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            return superState;
        }

        final TimeSavedState myState = new TimeSavedState(superState);
        myState.isDialogShowing = true;
        myState.currentHour = mTimePicker.getCurrentHour();
        myState.currentMinute = mTimePicker.getCurrentMinute();
        return myState;
    }

    /** Restores current state of this preference and the time picker dialog if shown */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(TimeSavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        TimeSavedState myState = (TimeSavedState) state;
        super.onRestoreInstanceState(null);
        if (myState.isDialogShowing) {
            showDialog(null);
            initializeDialogView(myState);
        }
    }

    /** Class to represent saved internal state of these preferences */
    private static class TimeSavedState extends BaseSavedState {
        boolean isDialogShowing;
        int currentHour;
        int currentMinute;
        Bundle dialogBundle;

        public TimeSavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            currentHour = source.readInt();
            currentMinute = source.readInt();
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeInt(currentHour);
            dest.writeInt(currentMinute);
            dest.writeBundle(dialogBundle);
        }

        public TimeSavedState(Parcelable superState) {
            super(superState);
        }
    }
} 



 