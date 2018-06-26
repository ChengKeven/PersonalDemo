package com.example.cheng.personaldemo.Video;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.example.cheng.personaldemo.R;
import com.example.cheng.personaldemo.Video.widget.MediaControlView;
import com.example.cheng.personaldemo.Video.widget.MediaProgressButton;

import java.util.ArrayList;
import java.util.UUID;


/**
 *发帖拍摄页面，可拍照和录制短视频
 */


public class VideoRecorderActivity extends Activity{

    public static final String HAS_IMG = "has_image";
    private MediaUtils mediaUtils;

    private TextView btnInfo,flashStatus;
    private ImageView switchBtn;
    private MediaControlView controlView;
    private RelativeLayout recordLayout;
    private ImageView flashIcon, photoPreview;
    private View closeBtn;
    private MediaProgressButton mediaProgressBtn;
    private boolean isRecordStatus = false; //是否录制状态
    private boolean hasImg = false;
    private ArrayList<PhotoInfo> mSelectList;
    private boolean isAutoBrightness; //是否自动亮度

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_video);
        SurfaceView surfaceView = findViewById(R.id.main_surface_view);
        hasImg = getIntent().getBooleanExtra(HAS_IMG, false);
//        mSelectList = (ArrayList<PhotoInfo>) getIntent().getParcelableArrayListExtra("alreadyimg");
        mediaUtils = new MediaUtils(this);
        mediaUtils.setTargetDir(CameraHelper.getOutputVideoFile());
        mediaUtils.setTargetName(UUID.randomUUID() + ".mp4");
        mediaUtils.setSurfaceView(surfaceView);

        btnInfo = findViewById(R.id.tv_info);
        if (hasImg){
            btnInfo.setText(getString(R.string.module_bbs_camera_picture));
        }else {
            btnInfo.setText(getString(R.string.module_bbs_camera_hint));
        }
        closeBtn = findViewById(R.id.btn_close);
        controlView = findViewById(R.id.view_send);
        recordLayout = findViewById(R.id.record_layout);
        switchBtn = findViewById(R.id.btn_switch);
        flashIcon = findViewById(R.id.iv_flash_light);
        flashStatus = findViewById(R.id.tv_flash_status_desc);
        mediaProgressBtn = findViewById(R.id.media_progress_button);
        photoPreview = findViewById(R.id.iv_photo_preview);

        controlView.backLayout.setOnClickListener(returnClick);
        controlView.selectLayout.setOnClickListener(confirmClick);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        flashIcon.setOnClickListener(flashListener);
        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaUtils.switchCamera();
            }
        });

        mediaProgressBtn.setMediaUtil(mediaUtils);
        mediaProgressBtn.setListener(mediaProgressBtnListener);
        mediaProgressBtn.setCanRecord(!hasImg);

        //恢复闪光灯状态
        recoverFlashStatus();

        //调整屏幕亮度
        adjustScreenBrightness();

        initOrientationListener();
    }



    @Override
    protected void onResume() {
        super.onResume();
        mediaProgressBtn.clearProgress();
//        showRecordLayout();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!shouldSave && mediaUtils != null) {
            clearShouldNotSavedFile();
        }
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //退出，恢复自动亮度.
        if (isAutoBrightness){
            ScreenUtil.startAutoBrightness(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (!shouldSave && mediaUtils != null) {
            clearShouldNotSavedFile();
        }
        mediaUtils.onDestroy();

        super.onDestroy();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_bottom_out);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_bottom_out);
    }

    MediaProgressButton.MediaProgressBtnListener mediaProgressBtnListener = new MediaProgressButton.MediaProgressBtnListener() {
        @Override
        public void onRecordStart() {
            //开始录制，隐藏摄像头切换和闪光灯
            showSwitchAndFlash(false);
        }

        @Override
        public void onRecordStop(boolean isSave) {
            if (hasImg){
                btnInfo.setText(getString(R.string.module_bbs_camera_picture));
            }else {
                btnInfo.setText(getString(R.string.module_bbs_camera_hint));
            }
            isRecordStatus = true;
            if (isSave) {
                showControlView();
//                updateGallery(mediaUtils.getVideoFilePath());
            } else {
                //停止录制，显示摄像头切换和闪光灯
                showSwitchAndFlash(true);
            }
        }
    };

    public void onPictureTaken(Bitmap rotatedBitmap) {
        if (rotatedBitmap != null) {
            isRecordStatus = false;
            showControlView();

            photoPreview.setVisibility(View.VISIBLE);
            photoPreview.setImageBitmap(rotatedBitmap);
        }
        mediaProgressBtn.onPictureTaken();
    }

    private boolean shouldSave = false;
    //返回预览
    private View.OnClickListener returnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaUtils.resumePreview();

            showRecordLayout();
            shouldSave = false;

            clearShouldNotSavedFile();
            showSwitchAndFlash(true);

        }
    };

    //确认使用照片、视频
    private View.OnClickListener confirmClick = new NoMultiClickListener() {
        @Override
        public void onNoMultiClick(View v) {
            shouldSave = true;
            String path;
            if (isRecordStatus) {
                path = mediaUtils.getVideoFilePath();
                gotoPostPager(path, true);

            } else {
                mediaUtils.savePicture();
                path = mediaUtils.getPictureFilePath();
//                updateGallery(path);
                gotoPostPager(path, false);
            }

        }
    };

    private void gotoPostPager(String path, boolean isVideo){
//        PhotoInfo info = new PhotoInfo();
//        info.setPhotoPath(path);
//        info.setVideo(isVideo);
//        mSelectList.add(info);
//        Intent i = new Intent();
//        i.putParcelableArrayListExtra("selectVideo", mSelectList);
//        setResult(0, i);
//        finish();
    }

    private void gotoSelectPager(String path, boolean isVideo) {
//        PhotoInfo info = new PhotoInfo();
//        info.setPhotoPath(path);
//        info.setVideo(isVideo);
//        mSelectList.add(info);
//
//        FunctionConfig.Builder functionConfigBuilder = new FunctionConfig.Builder();
//        PauseOnScrollListener pauseOnScrollListener = null;
//        functionConfigBuilder.setEnableRotate(true);
//        functionConfigBuilder.setRotateReplaceSource(true);
//        functionConfigBuilder.setEnableCamera(true);
//        functionConfigBuilder.setMutiSelectMaxSize(9);
//        functionConfigBuilder.setSelectArrayList(mSelectList);
//        ImageLoader imageLoader  = new GalleryImageLoader(this);
//        FunctionConfig functionConfig = functionConfigBuilder.build();
//        CoreConfig coreConfig = new CoreConfig.Builder(this, imageLoader, SunlandThemeConfig.getGalleryTheme())
//                .setFunctionConfig(functionConfig)
//                .setPauseOnScrollListener(pauseOnScrollListener)
//                .setNoAnimcation(true)
//                .build();
//
//        GalleryFinal.init(coreConfig);

//        Intent intent = new Intent(this, PhotoSelectActivity.class);
//        intent.putParcelableArrayListExtra("select", mSelectList);
//        intent.putExtra("from", "video");
//        startActivity(intent);
//        GalleryFinal.openGalleryForCamera(functionConfig);
    }

    //显示录制按钮、闪光灯、切换摄像头
    private void showRecordLayout() {
        controlView.stopAnim();
        showSwitchAndFlash(true);
        recordLayout.setVisibility(View.VISIBLE);
        photoPreview.setVisibility(View.INVISIBLE);
    }

    //显示 是否确认使用照片、视频页面
    private void showControlView() {
        recordLayout.setVisibility(View.GONE);
        showSwitchAndFlash(false);
        controlView.startAnim();
    }

    private void showSwitchAndFlash(boolean isShow) {
        switchBtn.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        flashIcon.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        btnInfo.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * 闪光灯
     */

    private View.OnClickListener flashListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final MediaUtils.FlashMode flashMode = mediaUtils.switchFlashMode();
//            PreferenceUtil.getInstance(VideoRecorderActivity.this).saveString(KeyConstant.FLASH_STATUS, flashMode.value());
            switch (flashMode) {
                case ON:
                    flashIcon.setImageResource(R.drawable.icon_flash_on);
                    flashStatus.setText(R.string.module_bbs_flash_on);
                    dismissFlashStatus();
                    break;
                case OFF:
                    flashIcon.setImageResource(R.drawable.icon_flash_off);
                    flashStatus.setText(R.string.module_bbs_flash_off);
                    dismissFlashStatus();
                    break;
                case AUTO:
                    flashIcon.setImageResource(R.drawable.icon_flash_auto);
                    flashStatus.setText(R.string.module_bbs_flash_auto);
                    dismissFlashStatus();
                    break;
            }
        }
    };

    private boolean shouldDismiss;
    private void dismissFlashStatus() {
        if (flashStatus.getVisibility() == View.INVISIBLE){
            shouldDismiss = true;
            flashStatus.setVisibility(View.VISIBLE);
        }else {
            shouldDismiss = false;
        }
        flashStatus.postDelayed(flashStatusDismissRunnable, 3000);
    }

    private Runnable flashStatusDismissRunnable = new Runnable() {
        @Override
        public void run() {
            if (shouldDismiss){
                flashStatus.setVisibility(View.INVISIBLE);
            }else {
                shouldDismiss = true;
            }
        }
    };

    private void recoverFlashStatus() {
////        String flashStatus = PreferenceUtil.getInstance(this).getString(KeyConstant.FLASH_STATUS, "auto");
//        switch (flashStatus) {
//            case "on":
//                flashIcon.setImageResource(R.drawable.icon_flash_on);
//                flashIcon.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mediaUtils.setFlashMode(MediaUtils.FlashMode.ON);
//                    }
//                }, 200);
//                break;
//            case "off":
//                flashIcon.setImageResource(R.drawable.icon_flash_off);
//                flashIcon.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mediaUtils.setFlashMode(MediaUtils.FlashMode.OFF);
//                    }
//                }, 200);
//                break;
//            default:
//                flashIcon.setImageResource(R.drawable.icon_flash_auto);
//                flashIcon.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mediaUtils.setFlashMode(MediaUtils.FlashMode.AUTO);
//                    }
//                }, 200);
//                break;
//        }
    }

    /**
     * 检测屏幕方向
     */
    private void initOrientationListener() {
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            int screenOrientation;

            @Override
            public void onOrientationChanged(int rotation) {
                if (mediaUtils.isRecording()) return;

                if ((rotation >= 0 && rotation <= 55) || rotation > 305) {
                    rotationAnimation(0);
                    mediaUtils.setRecordDegree(90);
                    rotation = 0;
                } else if (rotation > 55 && rotation <= 135) {
                    rotationAnimation(-90);
                    mediaUtils.setRecordDegree(180);
                    rotation = 90;
                } else if (rotation > 135 && rotation <= 225) {
                    rotation = 180;
                } else if (rotation > 225 && rotation <= 305) {
                    rotationAnimation(90);
                    mediaUtils.setRecordDegree(0);
                    rotation = 270;
                } else {
                    rotation = 0;
                }
                if (rotation == screenOrientation) return;
                screenOrientation = rotation;
                mediaUtils.updateCameraOrientation(screenOrientation);
            }
        };
        orientationEventListener.enable();
    }

    private void rotationAnimation(int afterDegree) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(switchBtn, "rotation", afterDegree),
                ObjectAnimator.ofFloat(flashIcon, "rotation", afterDegree),
                ObjectAnimator.ofFloat(flashStatus, "rotation", afterDegree)
        );
        set.setInterpolator(new OvershootInterpolator(1f));
        set.setDuration(200).start();
    }

    /**
     * 调整屏幕亮度
     * */
    private void adjustScreenBrightness() {
        isAutoBrightness = ScreenUtil.isAutoBrightness(getContentResolver());
        if (isAutoBrightness){
            ScreenUtil.stopAutoBrightness(this);
        }
        ScreenUtil.setBrightness(this, 160);
    }

    //清除不应该保存的图片、视频
    private void clearShouldNotSavedFile() {
        if (isRecordStatus) {
            mediaUtils.deleteVideoFile();
//            updateGallery(mediaUtils.getVideoFilePath());
        } else {
            mediaUtils.deletePictureFile();
//            updateGallery(mediaUtils.getPictureFilePath());
        }
    }

    public void scanFile(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        retriever.release();
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DATA, filePath);
        values.put(MediaStore.Video.Media.HEIGHT, height);
        resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
