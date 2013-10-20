/*
 * MyCheckBoxPreference.java
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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/** 
 * Trivial extension to standard CheckBoxPreference that retrieves the color 
 * Android gives to the text of the summary of the preferences. This normally depends on the
 * flavor of Android the application runs under. The color can be later retrieved
 * from other preferences to mimic the standard colors.
 */
public class MyCheckBoxPreference extends android.preference.CheckBoxPreference {

    /** Color of the text of the summary of the preferences */
    private static int sSummaryColor = Color.WHITE;
    
    /** Whether the color has been retrieved or not */
    private static boolean sInitialized = false;

    /** Whether this preference should be drawn in grey color */
    private boolean mGrey = false;    

    private int mColorStripe = 0;
    
    /** Constructor from a Context */
    public MyCheckBoxPreference(Context context) {
        super(context);
    }
    
    /** Constructor from a Context and AttributeSet */
    public MyCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference);
        mColorStripe= a.getColor(R.styleable.IconPreference_color_stripe, 0);
        if (mColorStripe != 0) {
            setLayoutResource(R.layout.preference);
        }
    }

    /** Constructor from a Context, AttributeSet and def style */
    public MyCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference, defStyle, 0);
        mColorStripe= a.getColor(R.styleable.IconPreference_color_stripe, 0);
        if (mColorStripe != 0) {
            setLayoutResource(R.layout.preference);
        }
    }
    
    public void setGrey (boolean grey) {
        mGrey = grey;
        notifyChanged();
    }

    /** 
     * Binds the created View to the data for this Preference. Extended from 
     * CheckBoxPreference to retrieve summary color first time its invoked 
     */
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        if (isEnabled()) {

            // Preference is being used to detect sSummaryColor for other preferences
            // Retrieves current summary color
            if (!sInitialized && !mGrey && mColorStripe == 0) {
                sSummaryColor = getSummaryColor(view);
                sInitialized = true;
            }

            // Sets texts color to grey of this preference as required
            else if (mGrey) {

                TextView titleView = (TextView) view.findViewById(android.R.id.title);
                if (titleView != null) {
                    titleView.setTextColor(Color.GRAY);
                }

                TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
                if (summaryView != null) {
                    summaryView.setTextColor(Color.GRAY);
                }
            }

            // Preference is being used as a normal check box preference with color stripe. 
            // Sets summary color 
            else if (mColorStripe != 0) {
                TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
                if (summaryView != null) {
                    summaryView.setTextColor(MyCheckBoxPreference.getSummaryColor());
                }
            }
        }

        // Set the color stripe if configured
        if (view instanceof MyListItemView) {
            ((MyListItemView) view).setColorStripe((this.mColorStripe != 0), this.mColorStripe);
        }
    }

    /** Retrieves summary text color given the view a preference is being drawn into */
    private int getSummaryColor(View view) {

        int color = Color.WHITE;

        // Gets the color android gave to the summary by default
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary); 
        if (summaryView != null) {
            ColorStateList list = summaryView.getTextColors();
            if (list != null) {
                color = list.getDefaultColor();
            }
        }
        return color;
    }
    
    /** Retrieves the color of the text of the summary of the check box preferences*/
    public static int getSummaryColor() {
        return sSummaryColor;
    }

    /** Set color stripe of this preference */
    public void setColorStripe (int color) {
        if (color != 0) {
            setLayoutResource(R.layout.preference);
        }
        mColorStripe = color;
        notifyChanged();
    }
    
    /** Returns color stripe of this preference */
    public int getColorStripe() {
        return mColorStripe;
    }    
}
