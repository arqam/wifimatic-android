/*
 * IconPreference.java
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
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/** Extension to Preference that allows to place an Icon at the right of the preference title */
public class IconPreference extends Preference {

    /** The icon of the preference */
    private Drawable mIcon;
    
    /** Whether the preference is to be highlighted in bold */
    private boolean mBold = false;
    
    private int mColorStripe = 0;

    /** Constructor to create an IconPreference */
    public IconPreference (Context context) {
        super(context);
        setLayoutResource(R.layout.preference);
    }
    
    /** Constructor to create an IconPreference */
    public IconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference);
        mIcon = a.getDrawable(R.styleable.IconPreference_icon);       
        mBold = a.getBoolean(R.styleable.IconPreference_bold, false);
        mColorStripe= a.getColor(R.styleable.IconPreference_color_stripe, 0);
    }

    /** Constructor to create an IconPreference */
    public IconPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);        
        setLayoutResource(R.layout.preference);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference, defStyle, 0);
        mIcon = a.getDrawable(R.styleable.IconPreference_icon);       
        mBold = a.getBoolean(R.styleable.IconPreference_bold, false);
        mColorStripe= a.getColor(R.styleable.IconPreference_color_stripe, 0);
    }

    /** 
     * Binds the created View to the data for this IconPreference.
     * Draws the icon and the title in bold if defined for this preference.
     * Summary and Bold title text mimics the color of the CheckBoxPreferences.
     */
    @Override
    public void onBindView(View view) {
        super.onBindView(view);

        View widgetLayout = view.findViewById(android.R.id.widget_frame);
        if (widgetLayout != null && widgetLayout instanceof ViewGroup) {            
            ((ViewGroup) widgetLayout).removeAllViews();
            if (mIcon != null) {
                ImageView imageView = new ImageView(widgetLayout.getContext());
                imageView.setImageDrawable(mIcon);
                ((ViewGroup) widgetLayout).addView(imageView);
                widgetLayout.setVisibility(View.VISIBLE);
            }
        }

        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        if (titleView != null) {
            if (mBold) {
                if (isEnabled()) {
                    titleView.setTextColor(MyCheckBoxPreference.getSummaryColor());
                }
                titleView.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
            }
        }

        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        if (summaryView != null && isEnabled()) {
            summaryView.setTextColor(MyCheckBoxPreference.getSummaryColor());
        }
        
        // Set the color stripe
        if (view instanceof MyListItemView) {
            ((MyListItemView) view).setColorStripe((this.mColorStripe != 0), this.mColorStripe);
        }
    }
        
    /** Set an Icon for this preference */
    public void setMyIcon(Drawable icon) {
        if ((icon == null && mIcon != null) || (icon != null && !icon.equals(mIcon))) {
            mIcon = icon;
            notifyChanged();
        }
    }

    /** Returns the Icon of this preference or null if it has not been set */
    public Drawable getIcon() {
        return mIcon;
    }
    
    /** Set this preference to be highlighted in bold letters */
    public void setBold (boolean bold) {
        mBold = bold;
        notifyChanged();
    }
    
    /** Returns if this preference has been set to be highlighted in bold letters */
    public boolean getBold() {
        return mBold;
    }    

    /** Set color stripe of this preference */
    public void setColorStripe (int color) {
        mColorStripe = color;
        notifyChanged();
    }
    
    /** Returns color stripe of this preference */
    public int getColorStripe() {
        return mColorStripe;
    }    
}
