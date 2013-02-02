/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android2.calculator3.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import com.android2.calculator3.Calculator;
import com.android2.calculator3.R;

public class Cling extends FrameLayout {
    public static final int SHOW_CLING_DURATION = 550;
    public static final int DISMISS_CLING_DURATION = 250;

    public static final String SIMPLE_CLING_DISMISSED_KEY = "cling.simple.dismissed";
    public static final String MATRIX_CLING_DISMISSED_KEY = "cling.matrix.dismissed";
    public static final String HEX_CLING_DISMISSED_KEY = "cling.hex.dismissed";
    public static final String GRAPH_CLING_DISMISSED_KEY = "cling.graph.dismissed";

    private static String SIMPLE_PORTRAIT = "simple_portrait";
    private static String SIMPLE_LANDSCAPE = "simple_landscape";
    private static String MATRIX_PORTRAIT = "matrix_portrait";
    private static String MATRIX_LANDSCAPE = "matrix_landscape";
    private static String HEX_PORTRAIT = "hex_portrait";
    private static String HEX_LANDSCAPE = "hex_landscape";
    private static String GRAPH_PORTRAIT = "graph_portrait";
    private static String GRAPH_LANDSCAPE = "graph_landscape";

    private Calculator mCalculator;
    private boolean mIsInitialized;
    private String mDrawIdentifier;
    private Drawable mBackground;
    private Drawable mPunchThroughGraphic;
    private Drawable mHandTouchGraphic;
    private int mPunchThroughGraphicCenterRadius;
    private int mAppIconSize;
    private int mButtonBarHeight;
    private float mRevealRadius;
    private int[] mPositionData;
    private boolean mDismissed;

    private Paint mErasePaint;

    public Cling(Context context) {
        this(context, null, 0);
    }

    public Cling(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Cling(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Cling, defStyle, 0);
        mDrawIdentifier = a.getString(R.styleable.Cling_drawIdentifier);
        a.recycle();
    }

    public void init(Calculator c, int[] positionData) {
        if(!mIsInitialized) {
            mCalculator = c;
            mPositionData = positionData;
            mDismissed = false;

            Resources r = getContext().getResources();
            mPunchThroughGraphic = r.getDrawable(R.drawable.cling);
            mPunchThroughGraphicCenterRadius = r.getDimensionPixelSize(R.dimen.clingPunchThroughGraphicCenterRadius);
            mAppIconSize = 0;// r.getDimensionPixelSize(R.dimen.app_icon_size);
            mRevealRadius = mAppIconSize * 1f;
            mButtonBarHeight = 0;// r.getDimensionPixelSize(R.dimen.button_bar_height);

            mErasePaint = new Paint();
            mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
            mErasePaint.setColor(0xFFFFFF);
            mErasePaint.setAlpha(0);

            mIsInitialized = true;
        }
    }

    public void dismiss() {
        mDismissed = true;
    }

    boolean isDismissed() {
        return mDismissed;
    }

    public void cleanup() {
        mBackground = null;
        mPunchThroughGraphic = null;
        mHandTouchGraphic = null;
        mIsInitialized = false;
    }

    private int[] getPunchThroughPosition() {// TODO No idea what this is for
        if(mDrawIdentifier.equals(SIMPLE_PORTRAIT) || mDrawIdentifier.equals(HEX_PORTRAIT)) {
            return new int[] { getMeasuredWidth() / 2, getMeasuredHeight() - (mButtonBarHeight / 2) };
        }
        else if(mDrawIdentifier.equals(SIMPLE_LANDSCAPE)) {
            return new int[] { getMeasuredWidth() - (mButtonBarHeight / 2), getMeasuredHeight() / 2 };
        }
        else if(mDrawIdentifier.equals(MATRIX_PORTRAIT) || mDrawIdentifier.equals(MATRIX_LANDSCAPE)) {
            return new int[] { mButtonBarHeight / 2, mButtonBarHeight / 2 };
        }
        else if(mDrawIdentifier.equals(GRAPH_PORTRAIT)) {
            final float scale = getResources().getDisplayMetrics().density;
            final int cornerXOffset = (int) (scale * 15);
            final int cornerYOffset = (int) (scale * 10);
            return new int[] { getMeasuredWidth() - cornerXOffset, cornerYOffset };
        }
        else if(mDrawIdentifier.equals(HEX_LANDSCAPE) || mDrawIdentifier.equals(GRAPH_LANDSCAPE)) {
            return mPositionData;
        }
        return new int[] { -1, -1 };
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        int[] pos = getPunchThroughPosition();
        double diff = Math.sqrt(Math.pow(event.getX() - pos[0], 2) + Math.pow(event.getY() - pos[1], 2));
        if(diff < mRevealRadius) {
            return false;
        }
        return true;
    };

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if(mIsInitialized) {
            DisplayMetrics metrics = new DisplayMetrics();
            mCalculator.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // Initialize the draw buffer (to allow punching through)
            Bitmap b = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);

            // Draw the background
            if(mBackground == null) {
                mBackground = getResources().getDrawable(R.drawable.bg_cling);
            }
            if(mBackground != null) {
                mBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                mBackground.draw(c);
            }
            else {
                c.drawColor(0x99000000);
            }

            int cx = -1;
            int cy = -1;
            float scale = mRevealRadius / mPunchThroughGraphicCenterRadius;
            int dw = (int) (scale * mPunchThroughGraphic.getIntrinsicWidth());
            int dh = (int) (scale * mPunchThroughGraphic.getIntrinsicHeight());

            // Determine where to draw the punch through graphic
            int[] pos = getPunchThroughPosition();
            cx = pos[0];
            cy = pos[1];
            if(cx > -1 && cy > -1) {
                c.drawCircle(cx, cy, mRevealRadius, mErasePaint);
                mPunchThroughGraphic.setBounds(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2);
                mPunchThroughGraphic.draw(c);
            }

            // Draw the hand graphic in All Apps
            if(mDrawIdentifier.equals(SIMPLE_PORTRAIT) || mDrawIdentifier.equals(SIMPLE_LANDSCAPE)) {
                if(mHandTouchGraphic == null) {
                    mHandTouchGraphic = getResources().getDrawable(R.drawable.hand);
                }
                int offset = mAppIconSize / 4;
                mHandTouchGraphic.setBounds(cx + offset, cy + offset, cx + mHandTouchGraphic.getIntrinsicWidth() + offset,
                        cy + mHandTouchGraphic.getIntrinsicHeight() + offset);
                mHandTouchGraphic.draw(c);
            }

            canvas.drawBitmap(b, 0, 0, null);
            c.setBitmap(null);
            b = null;
        }

        // Draw the rest of the cling
        super.dispatchDraw(canvas);
    };
}
