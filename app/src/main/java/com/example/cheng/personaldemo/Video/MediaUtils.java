package com.example.cheng.personaldemo.Video;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.cheng.personaldemo.R;
import com.example.cheng.personaldemo.util.T;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.hardware.Camera.CameraInfo;
import static android.hardware.Camera.PictureCallback;
import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.getNumberOfCameras;
import static android.hardware.Camera.open;


/**
 * 负责拍照，录视频，预览视频
 * 调用：Camera, MediaPlayer，MediaRecorder
 */

public class MediaUtils implements SurfaceHolder.Callback, PictureCallback, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private static final String TAG = "MediaUtils";
    private VideoRecorderActivity activity;
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private MediaPlayer mMediaPlayer;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    private File targetDir;
    private String targetName;
    private File mVideoFile; //视频文件
    private File mPictureFile;//照片文件

    private int previewWidth, previewHeight;
    private boolean isRecording;
    private int recordDegree = 90;   //录制视频角度
    private int mScreenRotation;  //屏幕初始状态，垂直角度是0
    private int cameraType = BACK;//0代表前置摄像头，1代表后置摄像头
    private static final int FRONT = 0;
    private static final int BACK = 1;

    private GestureDetector mDetector;
    private FlashMode mFlashMode = FlashMode.AUTO;//闪光灯模式
    private boolean isZoomIn = false;
    private List<Camera.Size> supportedVideoSizes;
    private int mCameraRotation;
    private CamcorderProfile recordProfile;
    private Bitmap bitmap;

    /**
     * 闪光灯类型
     */
    public enum FlashMode {
        ON("on"), OFF("off"), AUTO("auto"), TORCH("torch");
        private final String value;
        FlashMode(String value){
            this.value = value;
        }
        public String value(){
            return value;
        }
    }

    private double firstTime = 0;
    private double lastTime = 0;

    public MediaUtils(VideoRecorderActivity activity) {
        this.activity = activity;
    }

    public void setTargetDir(File file) {
        this.targetDir = file;
    }

    public void setTargetName(String name) {
        this.targetName = name;
    }

    public String getVideoFilePath() {
        if (mVideoFile == null) return null;
        return mVideoFile.getPath();
    }

    public String getPictureFilePath() {
        if (mPictureFile == null) return null;
        return mPictureFile.getPath();
    }

    public void setRecordDegree(int degree) {
        recordDegree = degree;
    }

    /**
     * 根据当前屏幕朝向修改保存图片的旋转角度
     * 摄像头的rotation参数为 0、90、180、270，水平方向为0
     * 屏幕的orientation垂直方向是0
     * @param screenOrientation 屏幕方向
     */
    protected void updateCameraOrientation(int screenOrientation) {
        mScreenRotation = screenOrientation;
        mCameraRotation = 90 + screenOrientation == 360 ? 0 : 90 + screenOrientation;
    }

    private FlashMode getFlashMode() {
        return mFlashMode;
    }

    public void setSurfaceView(SurfaceView view) {
        this.mSurfaceView = view;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setFixedSize(previewWidth, previewHeight);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        mDetector = new GestureDetector(activity, new ZoomGestureListener());
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    private boolean isPreviewStatus = false;

    public boolean isRecording() {
        return isRecording;
    }

    public void onDestroy() {
        releaseCamera();
        releaseMediaRecorder();
        releaseMediaPlayer();
    }

    /**
     * 摄像头预览
     */
    private void startPreView(SurfaceHolder holder) {
        if (mCamera == null) {
            mCamera = CameraHelper.getDefaultBackFacingCameraInstance();
            if (mCamera == null) {
                quitWithoutCameraPermission();
                return;
            }
            cameraType = BACK;
        }else {
            try {
                mCamera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setCameraParameters();

        try {
            mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    mSurfaceView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mSurfaceView.setScaleX(1f);
                    mSurfaceView.setKeepScreenOn(true);
                    mSurfaceView.setBackground(null);
                }
            });

            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            isPreviewStatus = true;
        } catch (Exception e) {
            isPreviewStatus = false;
            e.printStackTrace();
            quitWithoutCameraPermission();
        }
    }

    private void quitWithoutCameraPermission() {
        T.showShort(activity, "没有摄像头权限，请前往\"设置\"中开启");
        activity.finish();
    }

    public void resumePreview() {
        releaseMediaPlayer();
        startPreView(mSurfaceHolder);
    }

    private void setCameraParameters() {
        if (mCamera == null) {
            T.showShort(activity,"摄像头初始化失败，请检查相关权限");
            return;
        }

        try {
            mCamera.setDisplayOrientation(90);
            Camera.Parameters parameters = mCamera.getParameters();
            supportedVideoSizes = parameters.getSupportedVideoSizes();

            //预览
            previewWidth = ScreenUtil.getScreenHeightWithStatusBar(activity);
            previewHeight = ScreenUtil.getScreenWidth(activity);
            Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(supportedVideoSizes, parameters.getSupportedPreviewSizes(), previewWidth, previewHeight);
            if (!CameraHelper.isSupportedVideo(parameters.getSupportedPreviewSizes(), mCamera.new Size(previewWidth, previewHeight)) && optimalSize != null){
                previewWidth = optimalSize.width;
                previewHeight = optimalSize.height;
            }
            parameters.setPreviewSize(previewWidth, previewHeight);
            Log.e(TAG, "预览分辨率：" + "w: " + previewWidth + " h: " + previewHeight);

            //拍照
            Camera.Size pictureSize = CameraHelper.getOptimalPicSize(parameters.getSupportedPictureSizes(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
            if (null != pictureSize) {
                Log.e(TAG, "拍照分辨率:" + pictureSize.width + " : " + pictureSize.height);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);
            }
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setJpegQuality(100);
            parameters.setJpegThumbnailQuality(100);

            //设置闪光灯模式
            mSurfaceView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setFlashMode(mFlashMode);
                }
            },200);

            configMediaRecorderProfile(optimalSize);
            configAutoFocus(parameters);

            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("setParameters failed")){
                return;
            }
            quitWithoutCameraPermission();
        }
    }

    //配置自动对焦
    private void configAutoFocus(Camera.Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            for (String mode : focusModes) {
                mode.contains("continuous-video");
                parameters.setFocusMode("continuous-video");
            }
        }
    }

    /**
     * 录制视频
     */
    private boolean prepareRecord() {
        Log.e(TAG, "prepare start");
        try {
            if (mMediaRecorder == null)
                mMediaRecorder = new MediaRecorder();

            //闪光灯关闭
            if (mFlashMode == FlashMode.ON){
                setFlashMode(FlashMode.TORCH);
            }

            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            //配置视频码率
            mMediaRecorder.setProfile(recordProfile);

            //配置视频分辨率
            mMediaRecorder.setVideoSize(recordProfile.videoFrameWidth, recordProfile.videoFrameHeight);

            if (cameraType == FRONT && recordDegree == 90) {
                //前置摄像头做处理，不然会颠倒
                mMediaRecorder.setOrientationHint(270);
            } else {
                mMediaRecorder.setOrientationHint(recordDegree);
            }

            mVideoFile = new File(targetDir, targetName);
            mMediaRecorder.setOutputFile(mVideoFile.getPath());

        } catch (Exception e) {
            Log.e(TAG, "Exception prepareRecord: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            if(!TextUtils.isEmpty(e.getMessage())){
                Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            }
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        Log.e(TAG, "prepare end");
        return true;
    }

    /**
     * 配置录制视频码率、分辨率
     * 分辨率策略：
     * 1.分别处理16:9, 2:1的分辨率，如果摄像头满足等比例的较低分辨率（640*360, 800*450等等），优先使用
     * 2.使用默认640*360或640*480，大部分手机支持
     * 3.如果摄像头不支持较低分辨率，使用符合屏幕比例的较高分辨率
     */
    private void configMediaRecorderProfile(Camera.Size optimalSize) {
        if (mCamera == null || supportedVideoSizes == null) return;

        recordProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        recordProfile.videoBitRate = (int) (1.0 * previewWidth * previewHeight);

        Camera.Size videoSize = null;

        boolean isFullScreen = (float) previewWidth / (float) previewHeight == 2f / 1f;

        //16:9
        Camera.Size size960and540 = mCamera.new Size(960, 540);
        Camera.Size size1280and720 = mCamera.new Size(1280, 720);
        Camera.Size size800and450 = mCamera.new Size(800, 450);
        Camera.Size size640and360 = mCamera.new Size(640, 360);

        //2:1
        Camera.Size size1400and720 = mCamera.new Size(1440, 720);

        //4:3
        Camera.Size size640and480 = mCamera.new Size(640, 480);

        //分辨率优先级递减
        Camera.Size[] size16To9 = new Camera.Size[]{size960and540, size800and450, size1280and720, size640and360};
        Camera.Size[] size2To1 = new Camera.Size[]{size1400and720};

        Camera.Size[] sizes = size16To9;
        if (isFullScreen){
            sizes = size2To1;
        }

        //1.使用等比例且摄像头支持的较低分辨率
        for (Camera.Size size : sizes) {
            if (CameraHelper.isSupportedVideo(supportedVideoSizes, size)) {
                videoSize = size;
                break;
            }
        }

        if (videoSize == null){
            //2.使用默认960*540、1280*720、640*360或640*480，大部分手机支持
            if (CameraHelper.isSupportedVideo(supportedVideoSizes, size960and540)) {
                videoSize = size960and540;
            }else if (CameraHelper.isSupportedVideo(supportedVideoSizes, size1280and720)) {
                videoSize = size1280and720;
            } else if (CameraHelper.isSupportedVideo(supportedVideoSizes, size640and360)) {
                videoSize = size640and360;
            } else if (CameraHelper.isSupportedVideo(supportedVideoSizes, size640and480)) {
                videoSize = size640and480;
            } else if (optimalSize != null) {
                //3.使用符合屏幕比例的较高分辨率
                videoSize = mCamera.new Size(optimalSize.width, optimalSize.height);
            } else {
                videoSize = supportedVideoSizes.get(0);
            }
        }

        recordProfile.videoFrameWidth = videoSize.width;
        recordProfile.videoFrameHeight = videoSize.height;
        Log.e(TAG, "视频录制分辨率: " + "w:" + videoSize.width + " h:" + videoSize.height);
    }

    //开始录制
    public boolean record() {
        if (isRecording) {
            try {
                mMediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.d(TAG, "record exception: " + e.getMessage());
                deleteVideoFile();
            }
            releaseMediaRecorder();
            mCamera.lock();
            isRecording = false;
            return false;
        } else if (prepareRecord()) {
            try {
                mMediaRecorder.start();
                isRecording = true;
                Log.e(TAG, "开始录制");
                return true;
            } catch (Exception e) {
                releaseMediaRecorder();
                if(!TextUtils.isEmpty(e.getMessage())){
                    Log.e(TAG, e.getMessage());
                }
                return false;
            }
        }
        return false;
    }

    //停止录制且保存
    public void stopRecordSave() {
        resetFlashTorchStatus();

        Log.e(TAG, "停止录制");
        if (isRecording) {
            isRecording = false;
            try {
                mMediaRecorder.stop();
                Log.e(TAG, "视频保存在: " + mVideoFile.getPath());
            } catch (RuntimeException r) {
                Log.e(TAG, r.getMessage());
            } finally {
                releaseMediaRecorder();
                //准备短视频预览
                prepareMediaPlayer();
            }
        }
    }

    // 停止录制不保存
    public void stopRecordUnSave() {
        resetFlashTorchStatus();

        Log.e(TAG, "停止录制不保存");
        if (isRecording) {
            isRecording = false;
            try {
                mMediaRecorder.stop();
            } catch (RuntimeException r) {
                Log.e(TAG, r.getMessage());
                deleteVideoFile();
            } finally {
                releaseMediaRecorder();
            }
            deleteVideoFile();
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            Log.e(TAG, "release MediaRecorder");
        }
    }

    /**
     * 短视频预览，拍摄完循环播放
     */
    private void prepareMediaPlayer() {
        try {
            //step1，停止摄像头预览
            if (mCamera == null || activity.isFinishing()) return;

            mCamera.stopPreview();
            isPreviewStatus = false;
            mCamera.setPreviewDisplay(null);

            //2。初始化mediaPlayer
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            } else {
                mMediaPlayer.reset();
            }

            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setDataSource(mVideoFile.getPath());
            //step3，准备
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            resumePreview();
            T.showShort(activity, "没有录音权限，请前往\"设置\"中开启");
        }
    }

    //调整竖屏播放视频尺寸
    private void adjustVideoContainerSize(MediaPlayer mp) {
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();

        //计算视频缩放比例
        if (videoWidth > videoHeight){
            float ratio = (float)ScreenUtil.getScreenWidth(activity) / (float) videoWidth;

            //调整视频高度
            videoWidth = ScreenUtil.getScreenWidth(activity);
            videoHeight = (int) Math.ceil((float) videoHeight * ratio);
        }else {
            float ratio = (float)ScreenUtil.getScreenHeightWithStatusBar(activity) / (float) videoHeight;

            //调整视频高度
            videoWidth = (int) Math.ceil((float) videoWidth * ratio);
            videoHeight = ScreenUtil.getScreenHeightWithStatusBar(activity);
        }

        final int finalVideoHeight = videoHeight;
        final int finalVideoWidth = videoWidth;
        mSurfaceView.postDelayed(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(finalVideoWidth, finalVideoHeight);
                params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                mSurfaceView.setLayoutParams(params);

                mSurfaceView.setScaleX(finalVideoWidth/(float)ScreenUtil.getScreenWidth(activity));
            }
        },200);
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    //视频已准备好，可以播放
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setLooping(true);
        adjustVideoContainerSize(mp);

        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "mediaplayer onError: " + "what：" + what + " extra:" + extra);
        return false;
    }

    /**
     * 拍照
     */
    public void takePhoto() {
        if (mCamera != null) {
            try {
                Log.e(TAG, "----takePhoto----");
                mCamera.takePicture(null, null, this);
            } catch (Exception e) {
                e.printStackTrace();
                activity.onPictureTaken(null);
                Toast.makeText(activity, "拍照失败，请重试", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //拍照回调
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.e(TAG, "onPictureTaken");
        if (data == null) {
            Toast.makeText(activity, "拍照失败，请重试", Toast.LENGTH_SHORT).show();
        }

        if (mCamera != null) mCamera.stopPreview();
        isPreviewStatus = false;

        //解析生成相机返回的图片
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Log.e(TAG, "BitmapFactory.decodeByteArray");

        //前置摄像头做左右翻转处理
        if (cameraType == FRONT) {
            bitmap = CameraHelper.reversalBmp(bitmap);
            Log.e(TAG, "CameraHelper.reversalBmp() 前置摄像头左右翻转处理");
        }

        //根据屏幕角度调整照片角度
        bitmap = CameraHelper.rotateBitmapByDegree(bitmap, mCameraRotation);

        //surfaceView作为照片的黑色背景
        mSurfaceView.setBackgroundColor(activity.getResources().getColor(R.color.black));

        //生成保存照片的文件
        File toFile = CameraHelper.getOutputPictureFile();
        if (toFile == null) return;
        mPictureFile = toFile;

        activity.onPictureTaken(bitmap);
    }

    public void savePicture() {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mPictureFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

            bos.flush();
            bos.close();
        } catch (IOException e) {
            Toast.makeText(activity, "保存照片失败", Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, "----Bitmap存文件----");
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
            Log.e(TAG, "release Camera");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        Log.d(TAG, "surfaceCreated");
        startPreView(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        updateCameraOrientation(mScreenRotation);

        //恢复播放
        try {
            if (mMediaPlayer != null) {
                prepareMediaPlayer();
            }
        }catch (Exception e){
            Log.e(TAG, "mediaPlayer 恢复播放失败");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.d(TAG, "surfaceDestroyed");
            releaseCamera();
        }
        if (mMediaRecorder != null) {
            releaseMediaRecorder();
        }
//        releaseMediaPlayer();
        //暂停播放
        if (mMediaPlayer != null){
            mMediaPlayer.stop();
        }
    }

    private class ZoomGestureListener extends GestureDetector.SimpleOnGestureListener {
        //双击手势事件
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            super.onDoubleTap(e);
//            lastTime = System.currentTimeMillis();
//            if (lastTime - firstTime < 1000) {
//                firstTime = lastTime;
//                return false;
//            }
//            if (isPreviewStatus){
//                switchCamera();
//            }
//            firstTime = lastTime;
            return true;
        }
    }

    private void setZoom(int zoomValue) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                int maxZoom = parameters.getMaxZoom();
                if (maxZoom == 0) {
                    return;
                }
                if (zoomValue > maxZoom) {
                    zoomValue = maxZoom;
                }
                parameters.setZoom(zoomValue);
                mCamera.setParameters(parameters);
            }
        }
    }

    /**
     * 设置闪关灯模式
     *
     * @param flashMode
     */
    public void setFlashMode(FlashMode flashMode) {
        mFlashMode = flashMode;
        if (mCamera != null && null != mCamera.getParameters() && mCamera.getParameters().getSupportedFlashModes() != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            switch (flashMode) {
                case ON:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    break;
                case OFF:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    break;
                case AUTO:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
                case TORCH:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCamera.setParameters(parameters);
        }
    }

    /**
     * 切换闪关灯
     *
     * @return 返回当前闪关灯状态
     */
    public FlashMode switchFlashMode() {
        FlashMode mode = null;
        if (getFlashMode() == FlashMode.ON) {
            mode = FlashMode.OFF;
        } else if (getFlashMode() == FlashMode.OFF) {
            mode = FlashMode.AUTO;
        } else if (getFlashMode() == FlashMode.AUTO) {
            mode = FlashMode.ON;
        }
        if (mode == null) return null;
        setFlashMode(mode);
        return mode;
    }

    //如果录制时是常亮，预览时则是打开
    private void resetFlashTorchStatus() {
        if (mFlashMode == FlashMode.TORCH){
            setFlashMode(FlashMode.OFF);
            mFlashMode = FlashMode.ON;
        }
    }

    public void switchCamera() {
        int cameraCount = 0;
        CameraInfo cameraInfo = new CameraInfo();
        cameraCount = getNumberOfCameras();//得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (cameraType == BACK) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    cameraType = FRONT;
                    restartCamera(i);
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    cameraType = BACK;
                    restartCamera(i);
                    break;
                }
            }
        }
    }

    private void restartCamera(int i) {
        if (mCamera != null) {
            mCamera.stopPreview();//停掉原来摄像头的预览
            mCamera.release();//释放资源
            mCamera = null;//取消原来摄像头
        }
        try {
            mCamera = open(i);//打开当前选中的摄像头
        }catch (Exception e){
            e.printStackTrace();
            T.showShort(activity, "切换摄像头失败，请检查摄像头和录音权限");
            activity.finish();
        }
        startPreView(mSurfaceHolder);

        updateCameraOrientation(mScreenRotation);
    }

    public boolean deleteVideoFile() {
        return CameraHelper.deleteFile(mVideoFile);
    }

    public boolean deletePictureFile() {
        return CameraHelper.deleteFile(mPictureFile);
    }

}
