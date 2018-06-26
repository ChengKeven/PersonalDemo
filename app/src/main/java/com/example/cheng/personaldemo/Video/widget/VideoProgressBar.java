package com.example.cheng.personaldemo.Video.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.cheng.personaldemo.R;


/**
 * Created by Wanbo on 2016/12/7.
 */

public class VideoProgressBar extends View {

    private boolean isClear = true;  //清除进度
    private Paint mProgressPaint, mBgPaint;
    private RectF mRectF;
    private int progress;
    private OnProgressEndListener mOnProgressEndListener;
    private int width, height;
    private int mProgressCircleWidth;
    private int mMaxProgress = 320; //最大时间15s

    public VideoProgressBar(Context context) {
        super(context, null);
        init();
    }

    public VideoProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mProgressPaint = new Paint();
        mBgPaint = new Paint();
        mRectF = new RectF();


        //进度圆画笔
        mProgressCircleWidth = 12;
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStrokeWidth(mProgressCircleWidth);
        mProgressPaint.setStyle(Paint.Style.STROKE);

        //实心圆画笔
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStrokeWidth(mProgressCircleWidth);
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setAlpha(80);
        mBgPaint.setColor(getResources().getColor(R.color.btn_bg));
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public void clearProgress() {
        this.isClear = true;
        invalidate();
    }

    public void setOnProgressEndListener(OnProgressEndListener onProgressEndListener) {
        mOnProgressEndListener = onProgressEndListener;
    }

    public interface OnProgressEndListener {
        void onProgressEndListener();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = getMeasuredWidth();
        height = getMeasuredHeight();

        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }

        //位置
        mRectF.left = mProgressCircleWidth / 2 + .8f;
        mRectF.top = mProgressCircleWidth / 2 + .8f;
        mRectF.right = width - mProgressCircleWidth / 2 - 1.5f;
        mRectF.bottom = height - mProgressCircleWidth / 2 - 1.5f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //绘制大圆
        canvas.drawCircle(width / 2, width / 2, width / 2 - .5f, mBgPaint);

        // 绘制圆圈，进度条背景
        if (isClear) {
            progress = 0;
            cleanProgressBar(canvas);
            isClear = false;
            return;
        }

        if (progress == 0) {
            cleanProgressBar(canvas);
        } else if (progress > 0 && progress <= mMaxProgress) {
            mProgressPaint.setColor(getResources().getColor(R.color.record_progress));
            canvas.drawArc(mRectF, -90, ((float) progress / mMaxProgress) * 360, false, mProgressPaint);

            if (progress == mMaxProgress && mOnProgressEndListener != null) mOnProgressEndListener.onProgressEndListener();
        }
    }

    //清理进度条
    private void cleanProgressBar(Canvas canvas) {
        mProgressPaint.setColor(Color.TRANSPARENT);
        canvas.drawArc(mRectF, -90, 360, false, mProgressPaint);
    }

}
