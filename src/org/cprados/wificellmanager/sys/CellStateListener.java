/*
 * CellStateListener.java
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

import org.cprados.wificellmanager.BuildConfig;
import org.cprados.wificellmanager.ManagerService;

import android.content.Context;
import android.content.Intent;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

/**
 * Phone state listener singleton class, whose instance listens cell location
 * change events and forwards them to the manager service.
 */
public class CellStateListener extends PhoneStateListener {

	/** Tag for logging this class messages */
	private static final String LOGTAG = CellStateListener.class.getPackage().getName();

	/** Application context to invoke the manager service */
	private Context mContext;

	/** Action this listener sends events to */
	private String mAction;

	/** CellStateListener singleton instance variable */
	private static CellStateListener sInstance = null;

	/** Returns the unique instance of this class */
	public static CellStateListener getCellChangeListener(Context appContext,String action) {
		if (sInstance == null)
			sInstance = new CellStateListener(appContext, action);
		return sInstance;
	}

	/**
	 * Creates the listener with a given application context to invoke the
	 * manager service from it
	 */
	private CellStateListener(Context applicationContext, String action) {
		super();
		this.mContext = applicationContext;
		this.mAction = action;
	}

	/** Configures the listener to be launched periodically on each cell change */
	public static void requestCellChangeEvents(Context appContext, String action, boolean enable) {

		// Gets TelephonyManager service
		TelephonyManager tm = (TelephonyManager) appContext
				.getSystemService(Context.TELEPHONY_SERVICE);

		// Subscribes a CellChageListener to TelepphonyManager cell change events
		if (enable) {
			tm.listen(getCellChangeListener(appContext, action), LISTEN_CELL_LOCATION);
		}

		// Cancels CellChageListener subscription the TelephonyManager events
		else {
			tm.listen(CellStateListener.getCellChangeListener(appContext, action),LISTEN_NONE);
			delete();
		}
	}

	/** Handles cell change events */
	@Override
	public void onCellLocationChanged(CellLocation location) {

		if (BuildConfig.DEBUG) {
			Log.d(LOGTAG, "CellStateListener: Location change event: " + location);
		}

		// Creates the intent with location data
		Intent intent = new Intent();

		if (location != null) {
			// GSM or UMTS cell location received
			if (location instanceof GsmCellLocation) {
				intent.putExtra(CellStateManager.EXTRA_CID,((GsmCellLocation) location).getCid());
				intent.putExtra(CellStateManager.EXTRA_LAC,((GsmCellLocation) location).getLac());
				TelephonyManager tm = (TelephonyManager) (mContext.getSystemService(Context.TELEPHONY_SERVICE));
				if (tm != null) {
					intent.putExtra(CellStateManager.EXTRA_OP, tm.getNetworkOperator());
				}
			}
			// CDMA cell location received
			else if (location instanceof CdmaCellLocation) {
				intent.putExtra(CellStateManager.EXTRA_CID,((CdmaCellLocation) location).getBaseStationId());
				intent.putExtra(CellStateManager.EXTRA_LAC,((CdmaCellLocation) location).getSystemId());
                int networkId = ((CdmaCellLocation) location).getNetworkId();
                if (networkId != -1) {
    				intent.putExtra(CellStateManager.EXTRA_OP, String.valueOf(networkId));                	
                }
			}
		}

		// Forwards intent to the manager service
		if (mContext != null && mAction != null) {
			ManagerService.forwardEvent(mContext, mAction, intent);
		}
	}

	/**
	 * Sets the CellStateListener instance variable to null so it can be garbage
	 * collected
	 */
	public static void delete() {
		sInstance.mAction = null;
		sInstance.mContext = null;
		sInstance = null;
	}
}
