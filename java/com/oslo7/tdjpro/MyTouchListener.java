/*
 * Copyright (C) 2019 Oslo7
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

package com.oslo7.tdjpro;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

import static java.lang.Math.abs;



public class MyTouchListener implements RecyclerView.OnItemTouchListener {

    public interface OnTouchActionListener {
        void onLeftSwipe(View view, int position);
        void onRightSwipe(View view, int position);
        void onClick(View view, int position);
        void onLongPress(View view, int position);
    }


    private static final String TAG = "Touch";

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static final int SWIPE_MAX_OFF_PATH = 250;

    public OnTouchActionListener mOnTouchActionListener;
    private GestureDetectorCompat mGestureDetector;

    float x1;

    public MyTouchListener(Context context, final RecyclerView recyclerView,
                           OnTouchActionListener onTouchActionListener){

        mOnTouchActionListener = onTouchActionListener;
        mGestureDetector = new GestureDetectorCompat(context,new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                int childPosition = recyclerView.getChildAdapterPosition(child);
                mOnTouchActionListener.onClick(child, childPosition);
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                int childPosition = recyclerView.getChildAdapterPosition(child);
                mOnTouchActionListener.onLongPress(child, childPosition);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {

                try {
                    if (abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                        return false;
                    }

                    View child = recyclerView.findChildViewUnder(e1.getX(), e1.getY());
                    int childPosition = recyclerView.getChildAdapterPosition(child);

                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

                        if (mOnTouchActionListener != null) {
                            if(child!=null)
                                mOnTouchActionListener.onLeftSwipe(child, childPosition);
                        }

                    } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        if (mOnTouchActionListener != null) {
                            if(child != null)
                                mOnTouchActionListener.onRightSwipe(child, childPosition);
                        }
                    }
                } catch (Exception e) {
                    // nothing
                }

                return false;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }


    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // nothing
    }

}
