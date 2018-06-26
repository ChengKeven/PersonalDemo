package com.example.cheng.personaldemo.Video.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.example.cheng.personaldemo.Video.MediaUtils;
import com.example.cheng.personaldemo.util.T;


/**
 * 拍摄页，带进度条的按钮控件
 */

public class MediaProgressButton extends RelativeLayout implements VideoProgressBar.OnProgressEndListener {

    private VideoProgressBar progressBar;
    private View centerCircle;

    private int mProgress;
    private MediaUtils mediaUtils;
    private MediaProgressBtnListener listener;

    private boolean canRecord;
    private boolean isTakingPhoto;

    public MediaProgressButton(Context context) {
        super(context);

        init(context, null);
    }

    public MediaProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final ViewGroup content = (ViewGroup) inflater.inflate(R.layout.module_bbs_media_progress_button, this, true);

        progressBar = content.findViewById(R.id.progress_bar);
        centerCircle = content.findViewById(R.id.center_circle);

        progressBar.setOnProgressEndListener(this);
        clearProgress();
    }

    public void clearProgress() {
        progressBar.clearProgress();
    }

    public void setMediaUtil(MediaUtils util) {
        mediaUtils = util;
    }

    public void setListener(MediaProgressBtnListener listener) {
        this.listener = listener;
    }

    public void setCanRecord(boolean canRecord) {
        this.canRecord = canRecord;
    }

    private boolean isProgressEnd; //15s录制结束
    @Override
    public void onProgressEndListener() {
        isProgressEnd = true;
        stopRecord(true);
    }

    /**
     * 录制按钮相关逻辑
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mediaUtils == null || isTakingPhoto) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startRecord();
                break;
            case MotionEvent.ACTION_UP:
                if (isProgressEnd) {
                    break;
                }

                if (mProgress == 0) {
                    //拍照
                    isTakingPhoto = true;
                    mediaUtils.takePhoto();
                    stopRecord(false);
                } else {
                    //停止录制
                    stopRecord(true);
                }
                break;
        }
        return true;
    }

    private boolean isCanRecord = true;
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (!mediaUtils.isRecording()) {
                        if (mediaUtils.record()){
                            isCanRecord = true;
                            startButtonAnim();
                            listener.onRecordStart();
                        }else {
                            isCanRecord = false;
                            mProgress = 1;
                            T.showShort(getContext(), "没有录音权限，请前往\"设置\"中开启");
                        }
                    }

                    if (mediaUtils.isRecording()) {
                        progressBar.setProgress(mProgress);
                        Log.e("mediautils", "progress: " + mProgress);
                        mProgress = mProgress + 1;
                        sendMessageDelayed(handler.obtainMessage(0), 50);
                    }
                    break;
            }
        }
    };

    private static final long START_DELAY = 300;
    private void startRecord() {
        mProgress = 0;
        isProgressEnd = false;
        handler.removeMessages(0);
        if (!canRecord){
            Log.d("startRecord", "can not record because already has pictures");
            return;
        }

        handler.sendMessageDelayed(handler.obtainMessage(0), START_DELAY);
    }

    private static final int MIN_DURATION = 40;
    private static final int MEDIA_RECORDER_PREPARED_TIME = 16; //mediaRecorder准备需要时间，大约800ms
    private void stopRecord(boolean isSave) {
        handler.removeMessages(0);
        clearProgress();

        if (!isCanRecord){
            return;
        }

        if (isZoomIn){
            stopButtonAnim();
        }

        //不足2s不保存
        if (mProgress != 0 && mProgress < MIN_DURATION + MEDIA_RECORDER_PREPARED_TIME){
            isSave = false;
            T.showShort(getContext(), getContext().getString(R.string.module_bbs_video_too_short));
        }
        mProgress = 0;

        //是否保存
        if (isSave) {
            mediaUtils.stopRecordSave();
        } else {
            mediaUtils.stopRecordUnSave();
        }

        listener.onRecordStop(isSave);
    }

    private static final float ZOOM_OUT_SIZE = 0.6f;
    private static final float ZOOM_IN_SIZE = 1.3f;
    private static final long ANIMATION_DURATION = 300;
    private boolean isZoomIn = false;
    private void startButtonAnim() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(centerCircle, "scaleX", 1, ZOOM_OUT_SIZE),
                ObjectAnimator.ofFloat(centerCircle, "scaleY", 1, ZOOM_OUT_SIZE),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1, ZOOM_IN_SIZE),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1, ZOOM_IN_SIZE)
        );
        set.setDuration(ANIMATION_DURATION).start();
        isZoomIn = true;
    }

    private void stopButtonAnim() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(centerCircle, "scaleX", ZOOM_OUT_SIZE, 1f),
                ObjectAnimator.ofFloat(centerCircle, "scaleY", ZOOM_OUT_SIZE, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", ZOOM_IN_SIZE, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", ZOOM_IN_SIZE, 1f)
        );
        set.setDuration(ANIMATION_DURATION).start();
        isZoomIn = false;
    }

    public void onPictureTaken() {
        isTakingPhoto = false;
    }

    //回调
    public interface MediaProgressBtnListener {
        void onRecordStart();

        void onRecordStop(boolean isSave);
    }

}
