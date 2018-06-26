package com.example.cheng.personaldemo.Video.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.RelativeLayout;


/**
 * Created by wangchao on 2018/3/29.
 *
 */

public class DragPlayView extends RelativeLayout {


    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_MOVING = 1;
    public static final int STATUS_REBACK = 2;
    public static final String TAG = "ScaleViewPager";

    //最多可缩小比例
    public static final float MIN_SCALE_WEIGHT = 0.25f;
    public static final int REBACK_DURATION = 300;//ms
    public static final int DRAG_GAP_PX = 50;

    private int currentStatus = STATUS_NORMAL;

    float mDownX;
    float mDownY;
    private float screenHeight;


    public DragPlayView(Context context) {
        super(context);
        initView();
    }

    public DragPlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DragPlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initView(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
    }

    public interface IReleasePlayerView{
        void onClosePlayView();
    }

    IReleasePlayerView mListen;

    public void setCloseListen(IReleasePlayerView listen){
        mListen = listen;
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (currentStatus == STATUS_REBACK)
            return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onTouchEvent: -->> ACTION_DOWN");
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
                addIntoVelocity(ev);
                return true;

            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: -->> ACTION_MOVE");
                addIntoVelocity(ev);
                int deltaY = (int) (ev.getRawY() - mDownY);
                if (deltaY <= DRAG_GAP_PX && currentStatus!=STATUS_MOVING)
                    return super.onTouchEvent(ev);
                if ( (deltaY>DRAG_GAP_PX||currentStatus==STATUS_MOVING)){
                    setupMoving(ev.getRawX(),ev.getRawY());
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG, "onTouchEvent: -->> ACTION_UP");
                if (currentStatus!=STATUS_MOVING)
                    return super.onTouchEvent(ev);
                final float mUpX = ev.getRawX();//->mDownX
                final float mUpY = ev.getRawY();//->mDownY

                float vY = computeYVelocity();
                if (vY >= 600 || Math.abs(mUpY-mDownY) > screenHeight/5){//速度有一定快，或者移动位置超过屏幕一半，那么释放
                    mListen.onClosePlayView();
                }else {
                    setupReback(mUpX, mUpY);
                }
                break;
        }
        try {
            return super.onTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }

    protected float computeYVelocity() {
        float result = 0;
        if (mVelocityTracker != null) {
            mVelocityTracker.computeCurrentVelocity(1000);
            result = mVelocityTracker.getYVelocity();
            releaseVelocity();
        }
        return result;
    }

    protected void releaseVelocity() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private VelocityTracker mVelocityTracker;

    protected void addIntoVelocity(MotionEvent event) {
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onInterceptTouchEvent: -->>ACTION_DOWN");
                mDownX = event.getRawX();
                mDownY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onInterceptTouchEvent: -->>ACTION_MOVE");
                float moveY = event.getY();
                if (moveY - mDownY > DRAG_GAP_PX)
                    return true;
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onInterceptTouchEvent: -->>ACTION_UP");
                break;
        }
        try {
            return super.onInterceptTouchEvent(event);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private void setupReback(final float mUpX, final float mUpY){
        currentStatus = STATUS_REBACK;
        if (mUpY!=mDownY) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(mUpY, mDownY);
            valueAnimator.setDuration(REBACK_DURATION);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float mY = (float) animation.getAnimatedValue();
                    float percent = (mY - mDownY) / (mUpY - mDownY);
                    float mX = percent * (mUpX - mDownX) + mDownX;
                    setupMoving(mX, mY);
                    if (mY == mDownY) {
                        mDownY = 0;
                        mDownX = 0;
                        currentStatus = STATUS_NORMAL;
                    }
                }
            });
            valueAnimator.start();
        }else if (mUpX!=mDownX){
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(mUpX, mDownX);
            valueAnimator.setDuration(REBACK_DURATION);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float mX = (float) animation.getAnimatedValue();
                    float percent = (mX - mDownX) / (mUpX - mDownX);
                    float mY = percent * (mUpY - mDownY) + mDownY;
                    setupMoving(mX, mY);
                    if (mX == mDownX) {
                        mDownY = 0;
                        mDownX = 0;
                        currentStatus = STATUS_NORMAL;
                    }
                }
            });
            valueAnimator.start();
        }
    }


    private void setupMoving(float movingX ,float movingY) {
        currentStatus = STATUS_MOVING;
        float deltaX = movingX - mDownX;
        float deltaY = movingY - mDownY;

        float scale = 1f;
        float alphaPercent = 1f;
        if(deltaY>0) {
            scale = 1 - Math.abs(deltaY) / screenHeight;
            alphaPercent = 1- Math.abs(deltaY) / (screenHeight/2);
        }
        this.setTranslationX(deltaX);
        this.setTranslationY(deltaY);
        setupScale(scale);
        setBackgroundColor(convertPercentToBlackAlphaColor(alphaPercent));
    }

    protected int convertPercentToBlackAlphaColor(float percent) {
        percent = Math.min(1, Math.max(0, percent));
        int intAlpha = (int) (percent * 255);
        String stringAlpha = Integer.toHexString(intAlpha).toLowerCase();
        String color = "#" + (stringAlpha.length() < 2 ? "0" : "") + stringAlpha + "000000";
        return Color.parseColor(color);
    }


    private void setupScale(float scale) {
        scale = Math.min(Math.max(scale, MIN_SCALE_WEIGHT), 1);
        this.setScaleX(scale);
        this.setScaleY(scale);
    }
}
