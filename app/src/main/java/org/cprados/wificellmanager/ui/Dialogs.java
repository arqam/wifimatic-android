/*
 * Dialogs.java
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

import org.cprados.wificellmanager.DataManager;
import org.cprados.wificellmanager.ManagerService;
import org.cprados.wificellmanager.R;
import org.cprados.wificellmanager.StateMachine.State;
import org.cprados.wificellmanager.StateMachine.StateEvent;
import org.cprados.wificellmanager.sys.WifiStateManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Class to manage all the applications dialogs*/
public class Dialogs {
        
    /**
     * Builder class for Application About Info dialog
     */
    public static class AboutDialogBuilder extends MyAlertDialogBuilder {

        /** Gravity for About Dialog */
        private static final int ABOUT_DIALOG_GRAVITY = Gravity.CENTER_HORIZONTAL;
        private Context mContext;
        private DialogInterface.OnClickListener mPositiveButtonListener;
        private DialogInterface.OnClickListener mNeutralButtonListener;
        
        public AboutDialogBuilder (Context context) {
            
            // Construct the builder
            super(context);            
            mContext = context;
            
            // Positive listener launches help URL and dismiss the dialog
            mPositiveButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String url = mContext.getResources().getString(R.string.url_app_site);
                    Intent i = (new Intent(Intent.ACTION_VIEW)).setData(Uri.parse(url));
                    mContext.startActivity(i);
                    dialog.dismiss();
                }
            };
            
            // Neutral listener launches google play page
            mNeutralButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String url = !DataManager.isFullVersion() ? 
                            mContext.getResources().getString(R.string.url_google_play_site) : 
                            mContext.getResources().getString(R.string.url_amazon_marketplace_site);
                    Intent i = (new Intent(Intent.ACTION_VIEW)).setData(Uri.parse(url));
                    mContext.startActivity(i);
                    dialog.dismiss();
                }
            };
        }

        /** Creates the application about dialog */
        @Override
        public AlertDialog create() {
            
            // Sets Alert dialog properties
            setTitle(R.string.app_version);
            setIcon(R.drawable.ic_launcher_icon);
            setGravity(ABOUT_DIALOG_GRAVITY);
            setCancelable(true);
            setMessage(R.string.activity_about_text_info);

            // Sets Alert Dialog Buttons and action handlers
            setPositiveButton(R.string.button_title_help, mPositiveButtonListener);
            setNeutralButton(R.string.button_title_google_play, mNeutralButtonListener);

            return super.create();
        }
    }    

    /** 
     * Builder class for Confirm Preference Change dialog
     */
    public static class ConfirmPreferenceDialogBuilder extends MyAlertDialogBuilder {
        
        /** Preference to confirm with the dialog */
        private Preference mPreference;
        
        /** Text for the dialog */
        private int mTextId;
        
        /** Default value for the preference */
        private Object mDefVal;
        
        /** Button and key listeners for the dialog */
        private DialogInterface.OnClickListener mPositiveButtonListener;
        private DialogInterface.OnClickListener mNegativeButtonListener;
        private DialogInterface.OnKeyListener mKeyListerner;

        /** Builds a Confirm Preference Dialog for the given preference, with the given text and default value */
        public ConfirmPreferenceDialogBuilder (Context context, int textId, final Preference preference, Object defVal) {

            // Construct the builder
            super(context);            
            mTextId = textId;
            mPreference = preference;
            mDefVal = defVal;
            
            // Positive listener that dismisses the dialog
            mPositiveButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            };
            
            // Negative listener that reverts the value of the preference to the default
            mNegativeButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    cancelConfirmDialog();
                    dialog.dismiss();
                }
            };

            // Back key code listener that reverts the value of the preference to the default
            mKeyListerner = new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                        cancelConfirmDialog();
                        dialog.dismiss();
                        return true;
                    }
                    return false;
                }
            };                        
        }

        /** Builds a confirm preference alert dialog */
        @Override
        public AlertDialog create() {

            // Sets Alert dialog properties
            setTitle(mPreference != null ? mPreference.getTitle() : "");
            setIcon(R.drawable.ic_dialog_info);
            setMessage(mTextId);
            setCancelable(true);

            // Sets Alert Dialog Buttons and action handlers
            setPositiveButton(R.string.dialog_button_confirm_preference_ok, mPositiveButtonListener);
            setNegativeButton(R.string.dialog_button_confirm_preference_cancel, mNegativeButtonListener);
            setOnKeyListener(mKeyListerner);
            
            return super.create();
        }

        /** Sets the preference to the default value depending on the type of preference */
        private void cancelConfirmDialog() {

            if (mPreference != null) {
                if (mPreference instanceof CheckBoxPreference) {
                    boolean value = (mDefVal != null && mDefVal instanceof Boolean) ? ((Boolean) mDefVal).booleanValue() : false;
                    ((CheckBoxPreference) mPreference).setChecked(value);
                }
                else if (mPreference instanceof ListPreference) {
                    if (mDefVal != null) {
                        ((ListPreference) mPreference).setValue(mDefVal.toString());
                    }
                }
                else if (mPreference instanceof EditTextPreference) {
                    if (mDefVal != null) {
                        ((EditTextPreference) mPreference).setText(mDefVal.toString());
                    }
                }
            }
        }
    }
    
    /** 
     * Builder class for Welcome Dialog wizard 
     */
    public static class WelcomeDialogBuilder extends MyAlertDialogBuilder {

        /** First step possible for the Welcome Dialog */
        public static final int FIRST_STEP = 1;

        /** Last step possible for the Welcome Dialog */
        public static final int LAST_STEP = 4;
        
        /** Finished step for the Welcome Dialog */
        public static final int FINISHED_STEP = 0;

        /** Current step of this welcome dialog */
        private int mStep = FIRST_STEP;
        
        /** Context to build the dialog */
        private Context mContext;
        
        /** Listener of the dialog buttons */
        private DialogInterface.OnClickListener mButtonListener;
        
        /** Listener of the back key */
        private DialogInterface.OnKeyListener mKeyListerner;
        
        /** Builds a Welcome Dialog in a given step */
        public WelcomeDialogBuilder(Context context) {
            
            // Creates the builder
            super (context);
            mContext = context;
            mStep = DataManager.getWizardStep(mContext);
            
            // Positive button goes one step back, negative goes one step forward
            mButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (id == MyAlertDialogBuilder.BUTTON_LEFT) {
                        if (mStep > FIRST_STEP) {
                            // Refresh Alert Dialog according to step
                            mStep--;
                            refreshWelcomeDialog((AlertDialog) dialog);
                        }
                    }
                    else if (id == MyAlertDialogBuilder.BUTTON_RIGHT) {

                        if (mStep < LAST_STEP) {
                            // Refresh Alert Dialog according to step
                            mStep++;
                            refreshWelcomeDialog((AlertDialog) dialog);
                        }
                        else {
                            // Disable welcome dialog when finished
                            dialog.dismiss();
                            View check = ((AlertDialog) dialog).findViewById(R.id.dialog_welcome_extra_toggle);
                            if (check != null && check instanceof CheckBox && ((CheckBox) check).isChecked()) {
                                mStep = FINISHED_STEP;
                            }
                        }
                    }
                    DataManager.setWizardStep(mContext, mStep);
                }
            };
            
            // Back key code listener 
            mKeyListerner = new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {                                         
                        // Dismisses the dialog and the owner activity
                        Activity owner = ((AlertDialog) dialog).getOwnerActivity();                        
                        if (owner != null) {
                            owner.finish();
                        }           
                        dialog.dismiss();                        
                        return true;
                    }
                    return false;
                }
            };                                    
        }

        /** Refresh Alert Dialog according to step */
        private void refreshWelcomeDialog (AlertDialog dialog) {
            
            // Update message
            MyAlertDialogBuilder.setMessage(dialog, getText());

            // Update positive button
            MyAlertDialogBuilder.setButtonVisibility(dialog, MyAlertDialogBuilder.BUTTON_LEFT, getButtonVisibility(MyAlertDialogBuilder.BUTTON_LEFT));
            MyAlertDialogBuilder.setButtonText(dialog, MyAlertDialogBuilder.BUTTON_LEFT, getButtonText(MyAlertDialogBuilder.BUTTON_LEFT));

            // Update negative button
            MyAlertDialogBuilder.setButtonVisibility(dialog, MyAlertDialogBuilder.BUTTON_RIGHT, getButtonVisibility(MyAlertDialogBuilder.BUTTON_RIGHT));
            MyAlertDialogBuilder.setButtonText(dialog, MyAlertDialogBuilder.BUTTON_RIGHT, getButtonText(MyAlertDialogBuilder.BUTTON_RIGHT));

            MyAlertDialogBuilder.setExtraView(dialog, getView(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));            
        }

        /** Builds the application about dialog */
        @Override
        public AlertDialog create() {            

            // Sets Alert dialog properties
            setTitle(R.string.app_name);
            setIcon(R.drawable.ic_launcher_icon);
            setCancelable(true);
            setMessage(getText());
            setExtraView(getView(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                
            // Uses negative button to go forward, positive button to go back 
            setLeftButton(getButtonText(MyAlertDialogBuilder.BUTTON_LEFT), mButtonListener);
            setRightButton(getButtonText(MyAlertDialogBuilder.BUTTON_RIGHT), mButtonListener);
            setOnKeyListener(mKeyListerner);
                        
            // Set button visibility
            setButtonVisibility(MyAlertDialogBuilder.BUTTON_LEFT, getButtonVisibility(MyAlertDialogBuilder.BUTTON_LEFT));
            setButtonVisibility(MyAlertDialogBuilder.BUTTON_RIGHT, getButtonVisibility(MyAlertDialogBuilder.BUTTON_RIGHT));
                                              
            return super.create();
        }
                
        /** Calculates buttons visibility depending on step */
        private int getButtonVisibility(int wichButton) {            
            int result = View.VISIBLE;
            if (mStep == FIRST_STEP && wichButton != MyAlertDialogBuilder.BUTTON_RIGHT) {
                result = View.GONE;
            }
            return result;
        }
        
        private String getCurrentlyConnectedWifi() {
            String result = null;
            State state = DataManager.getState(mContext);
            
            if (state == State.IN_CON || state == State.OUT_CON || state == State.UNK_CON) {                
                result = DataManager.getCurrentWifi(mContext);                
            }            
            return result;
        }
        
        /** Calculates the text of the dialog depending on step */
        private CharSequence getText() {            
            CharSequence result = null;
            Resources res = mContext.getResources();
            
            switch (mStep) {
            case 1:
                result = res.getText(R.string.dialog_text_welcome_1);
                break;
            case 2:
                result = res.getText(R.string.dialog_text_welcome_2);
                break;
            case 3:
                String wifi = getCurrentlyConnectedWifi();
                if (wifi == null) {                              
                    result = res.getText(R.string.dialog_text_welcome_3a);
                }
                else {                
                    result = Html.fromHtml(res.getString(R.string.dialog_text_welcome_3b, wifi));
                }
                break;
            case 4:
                result = res.getText(R.string.dialog_text_welcome_4);
                break;
            }
            return result;
        }

        /** Calculates the text of the buttons in the dialog depending on step */
        private int getButtonText(int whichButton) {
            
            int result = R.string.app_name;

            if (whichButton == MyAlertDialogBuilder.BUTTON_RIGHT) {
                result = (mStep == LAST_STEP) ? R.string.dialog_button_welcome_finish : R.string.dialog_button_welcome_next;
            }

            else if (whichButton == MyAlertDialogBuilder.BUTTON_LEFT) {
                result = R.string.dialog_button_welcome_back;
            }
            
            return result;
        }

        /** Generates custom view of the dialog depending on step */
        private View getView() {

            View result = null;
            LayoutInflater inflater;

            // Selects text and icon
            switch (mStep) {
            
            // Step 1: Enable Wi-Fi Matic 
            case 2:
                inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);                
                result = inflater.inflate(R.layout.dialog_welcome_extra_layout, null);
                if (result != null && result instanceof LinearLayout) {
                    
                    // Setups the checkbox                    
                    final View button = result.findViewById(R.id.dialog_welcome_extra_toggle);                    
                    if (button != null && button instanceof CheckBox) {
                        
                        // Show the checkbox
                        ((CheckBox) button).setChecked(DataManager.getActivate(mContext));
                        button.setVisibility(View.VISIBLE);
                        
                        // Sets the checkbox listener                        
                        ((CheckBox)button).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                // Enable or disable the app
                                DataManager.setActivate(mContext, isChecked);                                

                                // Start or stop the service
                                Intent intent = new Intent(mContext.getApplicationContext(), ManagerService.class);                                
                                if (isChecked) {
                                    mContext.startService(intent.setAction(ManagerService.INIT_ACTION));                                    
                                }
                                else {
                                    mContext.stopService(intent);
                                }
                                
                                // Refresh main preferences activity
                                Preferences.requestRefresh(mContext);
                            }                            
                        });
                    }

                    // Setups the label
                    View label = result.findViewById(R.id.dialog_welcome_extra_text);
                    if (label != null && label instanceof TextView) {
                        // Sets the text label
                        ((TextView) label).setText(R.string.dialog_text_welcome_activate);
                        label.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                ((CheckBox) button).setChecked(!((CheckBox) button).isChecked());                                
                            }                            
                        });
                    }
                }                
                break;

            // Step 2: Go to Wi-Fi Settings
            case 3:                
                if (getCurrentlyConnectedWifi() == null) {
                    inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    result = inflater.inflate(R.layout.dialog_welcome_extra_layout, null);

                    if (result != null && result instanceof LinearLayout) {

                        // Setups the button
                        View.OnClickListener listener = null;
                        View button = result.findViewById(R.id.dialog_welcome_extra_button);
                        if (button != null && button instanceof ImageButton) {
                            ((ImageButton) button).setImageResource(R.drawable.ic_settings_wifi);

                            // Shows the button
                            button.setVisibility(View.VISIBLE);

                            // Sets the button listener
                            listener = new OnClickListener() {
                                public void onClick(View v) {

                                    // Turn on Wi-Fi if it was off
                                    StateEvent wifiState = WifiStateManager.getWifiState(mContext, null, null);
                                    if (wifiState == StateEvent.OFF) {
                                        WifiStateManager.setWifiState(mContext, StateEvent.DISC, null);
                                    }

                                    // Go to Wifi settings
                                    Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    mContext.startActivity(intent);
                                }
                            };
                            button.setOnClickListener(listener);
                        }

                        // Setups the text label
                        View label = result.findViewById(R.id.dialog_welcome_extra_text);
                        if (label != null && label instanceof TextView) {
                            ((TextView) label).setText(R.string.dialog_text_welcome_wifi_settings);
                            label.setOnClickListener(listener);
                        }
                    }
                }
                break;
                
            // Step 3: Finish
            case 4:
                inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);                
                result = inflater.inflate(R.layout.dialog_welcome_extra_layout, null);
                if (result != null && result instanceof LinearLayout) {
                    
                    // Setups the checkbox                    
                    final View button = result.findViewById(R.id.dialog_welcome_extra_toggle);
                    if (button != null && button instanceof CheckBox) {
                        
                        // Show the checkbox
                        button.setVisibility(View.VISIBLE);
                        ((CheckBox)button).setChecked(true);
                        
                        // Sets the checkbox listener
                        ((CheckBox)button).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            }                            
                        });
                    }

                    // Setups the label
                    View label = result.findViewById(R.id.dialog_welcome_extra_text);
                    if (label != null && label instanceof TextView) {
                        // Sets the text label
                        ((TextView) label).setText(R.string.dialog_text_welcome_dont_show);
                        label.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                ((CheckBox) button).setChecked(!((CheckBox) button).isChecked());                                
                            }                            
                        });
                    }
                }                
                break;                
            }

            return result;
        }
    }    
}
