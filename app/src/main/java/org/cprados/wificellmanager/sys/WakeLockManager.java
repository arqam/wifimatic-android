/*
 * WakeLockManager.java
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

import org.cprados.wificellmanager.DataManager;

import android.content.Context;
import android.os.PowerManager;

/**
 * Manager class for the application CPU Wake Lock. Wake Lock is acquired from the broadcast receivers and listeners
 * before invoking manager service and is released when the service has finished processing
 */
public class WakeLockManager {

    /** The wake lock tag */
    private static final String WAKELOCK_TAG = WakeLockManager.class.getName() + ".wake_lock";
    
    /** The WakeLockManager instance */
    private static WakeLockManager mInstance = null;

    /** Wake lock to be acquired before running the manager service*/
    private PowerManager.WakeLock mCpuWakeLock = null;

    /** Returns the WakeLockManager instance */
    public static WakeLockManager getWakeLockManager() {
        if (mInstance == null)
            mInstance = new WakeLockManager();
        return mInstance;
    }

    /** Acquires the wake lock */
    public void acquireWakeLock(Context context) {

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (mCpuWakeLock == null) {
            
            if (!DataManager.getTurnOnScreen(context)) {                
                mCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , WAKELOCK_TAG);
                //mCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKELOCK_TAG);
            }

            // Turn on screen fix
            else {                        
                //mCpuWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKELOCK_TAG);
                mCpuWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE , WAKELOCK_TAG);
            }
            
            mCpuWakeLock.acquire();
        }
    }

    /** Release the wake lock */
    public void releaseWakeLock() {
        if (mCpuWakeLock != null) {
            mCpuWakeLock.release();
            mCpuWakeLock = null;
        }
    }
}
