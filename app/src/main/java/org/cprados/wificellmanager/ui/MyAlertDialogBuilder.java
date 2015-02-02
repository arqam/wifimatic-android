/*
 * MyAlertDialogBuilder.java
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

import org.cprados.wificellmanager.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Customized Alert Dialog Builder that allows to setup text style, gravity, auto link mask of the alert dialogs and
 * to add an extra view after the message.
 * It builds dialogs whose buttons do not auto close the dialog after they are clicked, overriding standard
 * behavior of Alert Dialog.
 * Provides convenience static methods to modify these properties of the alert dialogs after they are created
 */
public abstract class MyAlertDialogBuilder extends AlertDialog.Builder {
    
    /** Custom Text View of the dialog that will be created */
    private TextView mCustomMessageView;
    
    /** Custom Layout View of the dialog that will be created */
    private LinearLayout mCustomLayoutView;
    
    /** Position of the custom extra view in the linear layout*/
    private static final int sExtraViewPostion = 1;
    
    /** Dialog is drawn with classic theme (negative button on the right positive on the left) */
    private static final boolean THEME_CLASSIC = (android.os.Build.VERSION.SDK_INT < 14);
    
    /** Right button id of the alert dialog, depending on the theme */
    public static final int BUTTON_LEFT = (THEME_CLASSIC ? AlertDialog.BUTTON_POSITIVE : AlertDialog.BUTTON_NEGATIVE);

    /** Right button id of the alert dialog, depending on the theme */
    public static final int BUTTON_RIGHT = (!THEME_CLASSIC ? AlertDialog.BUTTON_POSITIVE : AlertDialog.BUTTON_NEGATIVE);
    
    /** Dummy dialog button click listener for initial setup of dialog buttons. Does nothing */
    private OnClickListener mDummyButtonClickListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {}
    };
    
    /** Show listener holding reference to the dialog button click listeners */
    public class DialogShowListener implements DialogInterface.OnShowListener {
        
        /** Listener for the positive button click event */
        public DialogInterface.OnClickListener mPositiveListener;
        
        /** Listener for the negative button click event */        
        public DialogInterface.OnClickListener mNegativeListener;

        /** Listener for the neutral button click event */                        
        public DialogInterface.OnClickListener mNeutralListener;
        
        /** Visibility of the positive button */
        public int mPositiveVisible = View.VISIBLE;

        /** Visibility of the positive button */
        public int mNegativeVisible = View.VISIBLE;

        /** Visibility of the positive button */
        public int mNeutralVisible = View.VISIBLE;
        
        /** Whether the dialog this listener is attached has already been shown */
        private boolean mShown = false;
             
        /** Sets a View listener to the dialog button view that invokes the given On Click Listener */
        private void setButtonListener(final AlertDialog dialog, final int buttonId, final DialogInterface.OnClickListener listener) {

            if (listener != null) {                                
                Button button = dialog.getButton(buttonId);
                if (button != null && listener != null) {
                    button.setOnClickListener(new View.OnClickListener() {            
                        public void onClick(View v) {
                            listener.onClick(dialog, buttonId);                    
                        }
                    });
                }                    
            }           
        }

        /** Sets the dialog buttons views listeners after the dialog is shown the first time */
        public void onShow(DialogInterface dialog) {
            
            if (!mShown && dialog != null && dialog instanceof AlertDialog) {                

                // Setups negative button
                if (mNegativeListener != null) {
                    setButtonVisibility((AlertDialog) dialog, DialogInterface.BUTTON_NEGATIVE, mNegativeVisible);
                    setButtonListener((AlertDialog) dialog, DialogInterface.BUTTON_NEGATIVE, mNegativeListener);
                }
                
                // Setups neutral button 
                if (mNeutralListener != null) {
                    setButtonVisibility((AlertDialog) dialog, DialogInterface.BUTTON_NEUTRAL, mNeutralVisible);
                    setButtonListener((AlertDialog) dialog, DialogInterface.BUTTON_NEUTRAL, mNeutralListener);
                }

                // Setups positive button 
                if (mPositiveListener != null) {
                    setButtonVisibility((AlertDialog) dialog, DialogInterface.BUTTON_POSITIVE, mPositiveVisible);
                    setButtonListener((AlertDialog) dialog, DialogInterface.BUTTON_POSITIVE, mPositiveListener);
                }
                                                
                mShown = true;
            }                      
        }           
    };
    
    /** On Dialog show listener for the dialog that will be created */
    private DialogShowListener mDialogShowListener;
    
    /** Creates and returns the only instance of the dialog show listener used by this builder */
    private DialogShowListener getDialogShowListener () {
        if (mDialogShowListener == null) {
            mDialogShowListener = new DialogShowListener();
        }        
        return mDialogShowListener;
    }
    
    /** Constructs an alert dialog builder whose dialogs admit customized gravity and text style */
    public MyAlertDialogBuilder(Context context) {

        // Call superclass constructor
        super(context);

        // Setup customized view of the Alert Dialogs created by this builder
        if (context != null) {

            // Inflate the view 
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.dialog_custom_layout, null);

            // Get the message Text View inside the view
            if (dialogView != null) {
                                
                // Replace the view of the dialogs created by this class
                View customLayoutView = dialogView.findViewById(R.id.alert_dialog_layout);
                if (customLayoutView != null && customLayoutView instanceof LinearLayout) {
                    mCustomLayoutView = (LinearLayout) customLayoutView;
                    
                    View customMessageView = customLayoutView.findViewById(R.id.alert_dialog_message);
                    if (customMessageView != null && customMessageView instanceof TextView) {
                        mCustomMessageView = (TextView) customMessageView;                    
                    }                                    
                }
                                
                setView(dialogView);
            }                    
        }
    }
    
    /** Setup message of the dialog that will be created */
    @Override
    public MyAlertDialogBuilder setMessage(int messageId) {

        if (mCustomMessageView != null) {
            mCustomMessageView.setText(messageId);
        }
        return this;
    }

    @Override
    /** Setup message of the dialog that will be created */
    public MyAlertDialogBuilder setMessage(CharSequence message) {

        if (mCustomMessageView != null && message != null) {
            mCustomMessageView.setText(message);
        }
        return this;
    }
    
    /** Setup message of a dialog built using this builder class */
    public static void setMessage(AlertDialog dialog, int message) {

        View messageView = dialog.findViewById(R.id.alert_dialog_message);
        if (messageView != null && messageView instanceof TextView) {
            ((TextView) messageView).setText(message);
        }
    }
        
    /** Setup message of a dialog built using this builder class */
    public static void setMessage(AlertDialog dialog, CharSequence message) {

        View messageView = dialog.findViewById(R.id.alert_dialog_message);
        if (messageView != null && messageView instanceof TextView) {
            ((TextView) messageView).setText(message);
        }
    }
    
    /** Setup gravity of the text inside the dialog that will be created */
    public MyAlertDialogBuilder setGravity(int gravity) {
        if (mCustomMessageView != null) {
            mCustomMessageView.setGravity(gravity);
        }
        return this;
    }
    
    /** Setup gravity of the text inside a dialog built using this builder class  */
    public static void setGravity(AlertDialog dialog, int gravity) {

        View messageView = dialog.findViewById(R.id.alert_dialog_message);
        if (messageView != null && messageView instanceof TextView) {
            ((TextView) messageView).setGravity(gravity);
        }
    }

    /** Setup auto link mask of the text inside the dialog that will be created. */
    public MyAlertDialogBuilder setAutoLinkMask(int mask) {
        if (mCustomMessageView != null) {
            mCustomMessageView.setAutoLinkMask(mask);
        }
        return this;
    }

    /** Setup auto link mask of the text inside a dialog created by this builder class */
    public static void setAutoLinkMask(AlertDialog dialog, int mask) {

        View messageView = dialog.findViewById(R.id.alert_dialog_message);
        if (messageView != null && messageView instanceof TextView) {
            ((TextView) messageView).setAutoLinkMask(mask);
        }
    }
    
    /** Setup a custom extra view in the dialog that will be created */
    public MyAlertDialogBuilder setExtraView(View child, LayoutParams params) {   
                
        if (mCustomLayoutView != null) {
            // Remove custom extra view if there is one already setup
            if (mCustomLayoutView.getChildCount() > sExtraViewPostion) {
                mCustomLayoutView.removeViewAt(sExtraViewPostion);
            }
            // Add custom extra view
            if (child != null) {
                mCustomLayoutView.addView(child, sExtraViewPostion, params);            
            }    
        }
        return this;
    }
    
    /**  Setup a custom extra view in a dialog that as been created by this builder class */    
    public static void setExtraView(AlertDialog dialog, View child, LayoutParams params) {
        
        View customLayoutView  = dialog.findViewById(R.id.alert_dialog_layout);
        if (customLayoutView != null && customLayoutView instanceof LinearLayout) {
            // Remove custom extra view if there is one already setup
            if (((LinearLayout)customLayoutView).getChildCount() > sExtraViewPostion) {
                ((LinearLayout)customLayoutView).removeViewAt(sExtraViewPostion);
            }
            if (child != null) {
                // Add custom extra view
                ((LinearLayout)customLayoutView).addView(child, sExtraViewPostion, params);
            }
        }
    }
    
    /** Set visibility of the buttons of the dialog */
    public Builder setButtonVisibility(int wichButton, int visibility) {        
        switch (wichButton) {
        case DialogInterface.BUTTON_POSITIVE:
            getDialogShowListener().mPositiveVisible = visibility;
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            getDialogShowListener().mNegativeVisible = visibility;
            break;
        case DialogInterface.BUTTON_NEUTRAL:
            getDialogShowListener().mNeutralVisible = visibility;
            break;
        }        
        return this;
    }
    
    /** Set visibility of a button of a dialog */
    public static void setButtonVisibility(AlertDialog dialog, int whichButton, int visibility) {
        Button button = dialog.getButton(whichButton);
        if (button != null) {            
            button.setVisibility(visibility);    
        }
    }  
            
    /** Sets a dialog button text */
    public static void setButtonText (AlertDialog dialog, int whichButton, int text) {
        
        Button button = dialog.getButton(whichButton);
        if (button != null) {
            button.setText(text);
        }
    }  
        
    /** Setups listener to be invoked when the positive button of the dialog is pressed.*/
    @Override
    public Builder setPositiveButton(int textId, final OnClickListener listener) {        
        // Sets the button with a dummy click listener
        super.setPositiveButton(textId, mDummyButtonClickListener);
        
        // Prepares the show listener to setup the button listener after dialog is shown
        getDialogShowListener().mPositiveListener = listener;
        return this;
    }
    
    /** Setups listener to be invoked when the positive button of the dialog is pressed.*/
    @Override
    public Builder setPositiveButton(CharSequence text, final OnClickListener listener) {        
        // Sets the button with a dummy click listener
        super.setPositiveButton(text, mDummyButtonClickListener);
        
        // Prepares the show listener to setup the button listener after dialog is shown
        getDialogShowListener().mPositiveListener = listener;
        return this;
    }
    
    /** Setups listener to be invoked when the negative button of the dialog is pressed.*/
    @Override
    public Builder setNegativeButton(int textId, final OnClickListener listener) {        
        // Sets a dummy click listener for the dialog button 
        super.setNegativeButton(textId, mDummyButtonClickListener);
        
        // Prepares the show listener to setup the button listener after dialog is shown
        getDialogShowListener().mNegativeListener = listener;
        return this;
    }
    
    /** Setups listener to be invoked when the negative button of the dialog is pressed.*/
    @Override
    public Builder setNegativeButton(CharSequence text, final OnClickListener listener) {
        // Sets a dummy click listener for the dialog button 
        super.setNegativeButton(text, mDummyButtonClickListener);
        
        // Prepares the show listener to setup the button listener after dialog is shown
        getDialogShowListener().mNegativeListener = listener;
        return this;
    }
    
    /** Setups listener to be invoked when the negative button of the dialog is pressed.*/
    @Override
    public Builder setNeutralButton(int textId, final OnClickListener listener) {        
        // Sets a dummy click listener for the dialog button 
        super.setNeutralButton(textId, mDummyButtonClickListener);
        
        // Prepares the show listener to setup the button listener after dialog is shown
        getDialogShowListener().mNeutralListener = listener;
        return this;
    }
    
    /** Setups listener to be invoked when the negative button of the dialog is pressed.*/
    @Override
    public Builder setNeutralButton(CharSequence text, final OnClickListener listener) {
        // Sets a dummy click listener for the dialog button 
        super.setNeutralButton(text, mDummyButtonClickListener);
        
        // Prepares the show listener to setup the button listener after dialog is shown
        getDialogShowListener().mNeutralListener = listener;
        return this;
    }
    
    /** Setups listener to be invoked when the right button of the dialog is pressed.
     * Right depends on the theme */
    public Builder setRightButton (CharSequence text, final OnClickListener listener) {
        
        Builder result = null;
        if (THEME_CLASSIC) {
            result = setNegativeButton (text, listener);
        }
        else {
            result = setPositiveButton (text, listener);
        }
        
        return result;        
    }
    
    /** Setups listener to be invoked when the right button of the dialog is pressed.
     * Right depends on the theme */
    public Builder setRightButton (int textId, final OnClickListener listener) {
        
        Builder result = null;
        if (THEME_CLASSIC) {
            result = setNegativeButton (textId, listener);
        }
        else {
            result = setPositiveButton (textId, listener);
        }
        
        return result;        
    }

    /** Setups listener to be invoked when the left button of the dialog is pressed.
     * Left depends on the theme */
    public Builder setLeftButton (CharSequence text, final OnClickListener listener) {
        Builder result = null;
        if (!THEME_CLASSIC) {
            result = setNegativeButton (text, listener);
        }
        else {
            result = setPositiveButton (text, listener);
        }
        
        return result;   
    }
    
    /** Setups listener to be invoked when the left button of the dialog is pressed.
     * Left depends on the theme */
    public Builder setLeftButton (int textId, final OnClickListener listener) {
        Builder result = null;
        if (!THEME_CLASSIC) {
            result = setNegativeButton (textId, listener);
        }
        else {
            result = setPositiveButton (textId, listener);
        }
        
        return result;   
    }
                
    @Override
    public AlertDialog create() {
        
        // Creates the alert dialog
        AlertDialog alert = super.create();       
        
        // Sets the dialog show listener if required
        if (mDialogShowListener != null) {
            alert.setOnShowListener(mDialogShowListener);
        }                
        return alert;      
    }    
}
