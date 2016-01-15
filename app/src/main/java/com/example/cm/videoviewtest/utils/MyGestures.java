package com.example.cm.videoviewtest.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.IllegalFormatCodePointException;

/**
 * Created by cm on 2016/1/14.
 */
public class MyGestures {

    private String TAG = "MyGesture";

    private int mCurrentSlide = 0;

    private static final int LEFT_SLIDE = 1;
    private static final int RIGHT_SLIDE = 2;
    private static final int HORIZON_SLIDE = 3;
    private static final float STEP_SLIDE = 2f;
    private boolean firstScroll = false;

    private GestureDetector mTapGestureDetector;
    private Context context;

    public MyGestures(Context ctx){
        context = ctx;
        mTapGestureDetector = new GestureDetector(ctx, new TapGestureListener());
    }

    public boolean onTouchEvent(MotionEvent event){
        return mTapGestureDetector.onTouchEvent(event);
    }
    public void setTouchListener(TouchListener l){
        mListener = l;
    }

    private class TapGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            firstScroll = true;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float mOldX = e1.getX(), mOldY = e1.getY();
            int y = (int) e2.getRawY();
            int windowWidth = ((Activity)context).getWindowManager().getDefaultDisplay().getWidth();
            Log.i(TAG,"getWindowWidth = "+ windowWidth);
            int windowHeight = ((Activity)context).getWindowManager().getDefaultDisplay().getHeight();
            Log.i(TAG,"getWindowWidth = "+ windowHeight);
            if (firstScroll){
                mListener.onGestureBegin();
                if (Math.abs(distanceX) >= Math.abs(distanceY)){
                    mCurrentSlide = HORIZON_SLIDE;
                }else {
                    if (mOldX > windowWidth * 3.0 / 5){
                        mCurrentSlide = RIGHT_SLIDE;
                    }else if (mOldX < windowWidth * 2.0 / 5){
                        mCurrentSlide = LEFT_SLIDE;
                    }
                }
            }
            if (mCurrentSlide == HORIZON_SLIDE ){
                if (Math.abs(distanceX) > Math.abs(distanceY)){
                    Log.i(TAG,"HORIZON_SLIDE");
                    float percent = distanceX / windowWidth;
                    if (distanceX >= DensityUtil.dip2px(context, STEP_SLIDE)){
                        mListener.onHorizonSlide(percent, false);
                    }else if (distanceX < -DensityUtil.dip2px(context, STEP_SLIDE)){
                        mListener.onHorizonSlide(percent,true);
                    }
                }

            }else if (mCurrentSlide == RIGHT_SLIDE){
                if (Math.abs(distanceY) > Math.abs(distanceX)){
                    Log.i(TAG,"RIGHT_SLIDE");
                    float percent = distanceY / windowHeight;
                    if (distanceY >= DensityUtil.dip2px(context,STEP_SLIDE)){
                        mListener.onRightSlide(percent, true);
                    }else if (distanceY <= -DensityUtil.dip2px(context,STEP_SLIDE)){
                        mListener.onRightSlide(percent,false);
                    }
                }
            }else if (mCurrentSlide == LEFT_SLIDE){
                if (Math.abs(distanceY) > Math.abs(distanceX)){
                    Log.i(TAG,"LEFT_SLIDE");
                    float percent = (mOldY - y)/windowHeight;
                    mListener.onLeftSlide(percent);
                }
            }
            firstScroll = false;
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    private TouchListener mListener;

    public interface TouchListener extends View.OnTouchListener{
        public void onGestureBegin();
        public void onGestureEnd();
        public void onLeftSlide(float percent);
        public void onRightSlide(float percent, boolean isIncrease);
        public void onHorizonSlide(float percent, boolean isForward);
        public void onSingleTap();
        public void onDoubleTap();
    }
}
