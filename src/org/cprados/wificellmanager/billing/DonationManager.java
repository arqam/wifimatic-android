/*
 * DonationsManager.java
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

package org.cprados.wificellmanager.billing;

import org.cprados.wificellmanager.ui.Preferences;
import org.cprados.wificellmanager.BuildConfig;
import org.cprados.wificellmanager.DataManager;
import org.cprados.wificellmanager.billing.BillingService;
import org.cprados.wificellmanager.billing.Consts;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Manages donation process resources: Billing service and purchase observer
 */

public class DonationManager {
    
    /** Tag for logging this class messages */
    private static final String LOGTAG = DonationManager.class.getPackage().getName();
        
    /** Android Billing Service for donations */    
    private static BillingService sBillingService;
    
    /** Launch the Billing Service */
    public static BillingService runBillingService (Context context) {        
        if (!DataManager.isFullVersion() && sBillingService == null) {
            sBillingService = new BillingService();
            sBillingService.setContext(context);

            // Check if billing is supported.
            if (!sBillingService.checkBillingSupported()) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "Preferences: Billing not supported");
                }
                sBillingService.unbind();
                sBillingService = null;
            }
        }
        
        return sBillingService;
    }
    
    /** Returns Billing Service if it has being previously launched */
    public static BillingService getBillingService () {
        return sBillingService;
    }
    
    /** Releases billing service */
    public static void releaseBillingService () {
        if (sBillingService != null) {
            sBillingService.unbind();
            sBillingService = null;
        }
    }
    
    /** Purchase Observer subclass for the donation process */
    private static class DonationPurchaseObserver extends PurchaseObserver {
        
        private Preferences mActivityPreferences;

        public DonationPurchaseObserver(Activity activity, Handler handler) {            
            super(activity, handler);
            
            if (activity instanceof Preferences) {
                mActivityPreferences = (Preferences)activity;
            }
        }

        @Override
        public void onBillingSupported(boolean supported) {
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "PurchaseObserver: Billing Supported: " + supported);
            }
            if (!supported && sBillingService != null) {
                sBillingService.unbind();
                sBillingService = null;
            }
        }

        @Override
        public void onPurchaseStateChange(Consts.PurchaseState purchaseState, String itemId, int quantity, long purchaseTime,
                String developerPayload) {
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "Purchase State Changed: " + purchaseState + "; ItemId=" + itemId + "; Quantity=" + quantity + "; PurchaseTime="
                        + purchaseTime);
            }
            
            if (mActivityPreferences != null) {
                mActivityPreferences.refreshTabs();
            }
        }

        @Override
        public void onRequestPurchaseResponse(BillingService.RequestPurchase request, Consts.ResponseCode responseCode) {
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "Request Purchase Response: " + request + "; ResponseCode=" + responseCode);
            }
        }

        @Override
        public void onRestoreTransactionsResponse(BillingService.RestoreTransactions request, Consts.ResponseCode responseCode) {
            if (BuildConfig.DEBUG) {
                Log.d(LOGTAG, "Restore Transactions Response: " + request + "; ResponseCode=" + responseCode);
            }
                                         
            // Sets restored transactions
            if (responseCode == Consts.ResponseCode.RESULT_OK && mActivityPreferences != null) {
                DataManager.setRestoredTransations(mActivityPreferences, true);
            }
            
        }
    };
    
    /** The observer that will handle state changes of donation billing process */
    private static DonationPurchaseObserver sPurchaseObserver;

    /** Register the donation process observer */
    public static void runPurchaseObserver (Activity activity) {
        if (!DataManager.isFullVersion() && sPurchaseObserver == null) {
            sPurchaseObserver = new DonationPurchaseObserver(activity, new Handler());
            ResponseHandler.register(sPurchaseObserver);
        }
    }
    
    /** Unregisters observer of purchase process */
    public static void releasePurchaseObserver () {
        if (sPurchaseObserver != null) {
            ResponseHandler.unregister(sPurchaseObserver);
            sPurchaseObserver = null;
        }            
    }    
}
