/*
 * NotificationManager.java
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

import java.util.Date;

import org.cprados.wificellmanager.DataManager;
import org.cprados.wificellmanager.R;
import org.cprados.wificellmanager.StateMachine.StateAction;
import org.cprados.wificellmanager.ui.DescribeableElement;
import org.cprados.wificellmanager.ui.Preferences;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

/** Class to manage Status bar notifications */
public class NotificationManager {

    /** Id of the notification in the notifications bar */
    private static final int sNid = 0;

    /** Adds or updates a notification in the system status bar appropriate for an action */
    public static void notifyAction(Context context, StateAction action, DescribeableElement cause, Date date, Bundle stateData) {

        if (context != null) {
            Resources res = context.getResources();         

            // Notification title is the action description
            String title = (action != null) ? action.getDescription(res) : null;

            String text;
            if (action != StateAction.ADD) {               
                Object[] args = {CellStateManager.getNearbyWifis(stateData), 
                        WifiStateManager.getCurrentWifi(stateData), 
                        DataManager.getOffAfterDiscTimeout(context)};
                
                // Notification text is the action cause description
                text = (cause != null) ? cause.getDescription(res, args) : null;
            }
            else {
                // Notification text is the wifi name
                text = WifiStateManager.getCurrentWifi(stateData);
            }

            // Puts a notification in the notifications bar
            if (text != null && title != null) {
                putNotification(context, title, text, date);
            }                        
        }
    }

    /** Adds or updates a notification in the system status bar */
    public static void putNotification(Context context, String title, String text, Date date) {

        if (DataManager.getNotifications(context)) {
            int icon = R.drawable.ic_status_bac_icon;
            long when = date.getTime();
            context = context.getApplicationContext();

            Intent notificationIntent = new Intent(context, Preferences.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification(icon, text, when);
            notification.setLatestEventInfo(context, title, text, contentIntent);

            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(sNid, notification);
        }
    }

    /** Removes the notification from the system status bar */
    public static void removeNotification(Context context) {

        context = context.getApplicationContext();
        android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(sNid);
    }
}
