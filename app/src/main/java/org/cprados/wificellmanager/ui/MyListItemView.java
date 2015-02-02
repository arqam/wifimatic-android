/*
 * AuditTrailManager.java
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * A custom layout for each item in the Audit Trail list view.
 */
public class MyListItemView extends LinearLayout {

    private Paint mPaint = new Paint();
    private int mColorStripe;
    private boolean mDrawColorStripe = false;
    
    public MyListItemView(Context context) {
        super(context);
    }

    public MyListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setColorStripe (boolean draw, int color) {
        mDrawColorStripe = draw;
        mColorStripe = color;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        /* Draw vertical color stripe */
        if (mPaint != null && mDrawColorStripe) {
            mPaint.setColor(mColorStripe);
            canvas.drawRect(0, 0, 8, getHeight(), mPaint);
        }
    }
}
