/*
 * EventReceiver.java
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

import org.cprados.wificellmanager.sys.WakeLockManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Broadcast receiver that handles events fired by the system and forwards them to the manager service:
 * Handles boot completion, alarm manager periodic events, wifi and connection state changes.
 */
public class EventReceiver extends BroadcastReceiver {
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = EventReceiver.class.getPackage().getName(); 
    
    /** Enables or disables event entry  */
    public static void activateReceiver(Context context, boolean enableReceiver) {

        // Gets boot receiver component name
        ComponentName componentName = new ComponentName(context, EventReceiver.class);
        
        // Gets package manager reference
        PackageManager packageManager = context.getPackageManager();        
        
        if (packageManager != null) {
        
            // Checks current setting        
            int currentlyEnabled = packageManager.getComponentEnabledSetting(componentName);

            // Checks how setting should be
            int targetEnabled = enableReceiver ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            // Set component enabled setting
            if (currentlyEnabled != targetEnabled)
                packageManager.setComponentEnabledSetting(componentName, targetEnabled, PackageManager.DONT_KILL_APP);
        }
    }
    
    /** Configures alarm manager to send broadcasts to this receiver periodically at a given frequency (given in milliseconds)*/
    public static void requestPeriodicEvents (Context context, int[] timeOfDay, long frequency, String action, Intent intent, boolean enable) {

        // Gets alarm manager
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Creates intent message to wrap it within a pending intent
        // object to be given to alarm manager service
        if (intent == null) {
            intent = new Intent(context, EventReceiver.class); 
        }
        else {
            intent.setClass(context, EventReceiver.class);
        }

        // Sets the action
        if (action != null) {
            intent.setAction(action);
        }        
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        
        // Schedules or cancels the alarm
        if (enable) {
            long time = 0;
            if ((timeOfDay!=null) && (timeOfDay.length > 1)) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, time = getNextTimeMillis(timeOfDay[0],timeOfDay[1]), frequency, pIntent);               
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "EventReceiver: Repeating Alarm set: " +  (new Date(time)) + "), frequency(ms)=" + frequency + ", action=" + action);
                }
            }
            else {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, time = System.currentTimeMillis(), frequency, pIntent);
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "EventReceiver: Inexact Repeating Alarm set: " + (new Date(time)) + "), frequency(ms)=" + frequency + ", action=" + action);
                }
            }
        }
        else {
            am.cancel(pIntent);
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "EventReceiver: Alarm canceled: action=" + action);
            }        
        }        
    }
    
    /** Configures alarm manager to send a broadcast to this receiver at a given time */
    public static void requestEvent(Context context, Date date, String action, Intent intent, boolean enable) {

        // Gets alarm manager
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Creates intent message to wrap it within a pending intent
        // object to be given to alarm manager service
        if (intent == null) {
            intent = new Intent(context, EventReceiver.class); 
        }
        else {
            intent.setClass(context, EventReceiver.class);
        }

        // Sets the action
        if (action != null) {
            intent.setAction(action);
        }   

        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        
        // Schedules or cancels the alarm
        if (enable) {
            if (date != null) {
                am.set(AlarmManager.RTC_WAKEUP, date.getTime(), pIntent);
            }
        }
        else {
            am.cancel(pIntent);
        }        
    }
    
    /** Given an hour and minute, returns the time in ms since 1/1/1970 of the next occurrence */
    private static long getNextTimeMillis(int hour, int minute) {
        
        Calendar now  = Calendar.getInstance(); 
        Calendar result  = (Calendar) now.clone();
        
        result.set(Calendar.HOUR_OF_DAY,hour); 
        result.set(Calendar.MINUTE,minute); 
        result.set(Calendar.SECOND,0); 
        
        if(result.before(now)) { 
            result.add(Calendar.DATE,1); 
        }         
        
        return result.getTimeInMillis();
    }

    /** Event reception handler */
    @Override
    public void onReceive(Context context, Intent intent) {

        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "EventReceiver: " + intent);
        }
        
        // Acquires wake lock
        WakeLockManager.getWakeLockManager().acquireWakeLock(context.getApplicationContext());         
        
        // Forwards the intent to the manager service
        ManagerService.forwardEvent(context, null, intent);        
    }
}