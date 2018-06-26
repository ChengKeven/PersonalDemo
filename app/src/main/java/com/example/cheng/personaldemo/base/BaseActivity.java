package com.example.cheng.personaldemo.base;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.cheng.personaldemo.R;

public abstract class BaseActivity extends AppCompatActivity /*implements MvpView*/ {
    private static final String TAG = BaseActivity.class.getSimpleName();

    protected View customActionBar;

    protected boolean isActivityRunning;

//    public SunlandLoadingDialog mLoadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (initContentLayoutId() != 0) {
//            setContentView(initContentLayoutId());
//        }
        if (!disableWhiteNotificationBar()) {
            initStatusBar();
        }
        /*updateStatusBar();*/
//        initToolbar();
        initSunlandLoadingDialog();
//        setupActionBarListener();
    }

    protected void initStatusBar(){
        /*StatusBarUtils.setStatusBarColor(this, Color.parseColor("#f0f2f4"));
        StatusBarUtils.StatusBarLightMode(this);*/
    }

    protected void initSunlandLoadingDialog() {
//        mLoadingDialog = new SunlandLoadingDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.fontScale != 1)//非默认值
            getResources();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        if (res.getConfiguration().fontScale != 1) {//非默认值
            Configuration newConfig = new Configuration();
            newConfig.setToDefaults();//设置默认
            try {
                res.updateConfiguration(newConfig, res.getDisplayMetrics());
            } catch (Throwable e) {

            }
        }
        return res;
    }

    @Override
    protected void onPause() {
        try { //fix bug:https://bugly.qq.com/v2/crash-reporting/crashes/161f94eb00/9323?pid=1
            super.onPause();
        } catch (Exception e) {

        }

        isActivityRunning = false;
    }

    private void updateStatusBar() {
//        StatusBarUtils.StatusBarLightMode(this);
    }

//    @Override
    public void showLoading() {
//        if (!Utils.isActivityAlive(this)) {
//            return;
//        }
//        if (isFinishing()) return;
//        if (mLoadingDialog != null && mLoadingDialog.isShowing()) return;
//        if (mLoadingDialog != null) {
//            mLoadingDialog.show();
//        }
    }

//    @Override
//    public void hideLoading() {
//        if (isFinishing()) return;
//        if (mLoadingDialog != null && mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
//    }
//
//    @Override
//    public void onError(String message) {
//        if (message != null) {
//            Log.d(TAG, message);
//        }
//    }

//    @Override
//    public void onError(@StringRes int resId) {
//        onError(getString(resId));
//    }
//
//    @Override
//    public boolean isNetworkConnected() {
//        return Utils.isNetworkAvailable(this);
//    }
//
//    @Override
//    public void hideKeyboard() {
//        View view = this.getCurrentFocus();
//        if (view != null) {
//            InputMethodManager imm = (InputMethodManager)
//                    getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        }
//    }

//    private void initToolbar() {
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        if (null != toolbar) {
//            setSupportActionBar(toolbar);
//            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//
//            LayoutInflater inflater = LayoutInflater.from(this);
//            customActionBar = inflater.inflate(initActionbarLayoutID(), null);
//            initActionbarView(customActionBar);
//            getSupportActionBar().setCustomView(customActionBar);
//        }
//    }

    /**
     * 默认actionBar布局，子类可以重写提供自定义布局
     *
     * @return 返回自定义的actionBar布局
     */
//    protected int initActionbarLayoutID() {
//        return R.layout.custom_actionbar_home_common;
//    }

//    protected int initContentLayoutId() {
//        return 0;
//    }

    protected boolean disableWhiteNotificationBar() {
        return false;
    }
    /**
     * 如果actionBar中有返回button，设置点击事件
     */
//    protected void setupActionBarListener() {
//        if (customActionBar == null) return;
//        View back = customActionBar.findViewById(R.id.actionbarButtonBack);
//        if (back != null) {
//            back.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    finish();
//                }
//            });
//        }
//    }

//    protected void setToolBarTitle(String title) {
//        TextView titleTv = customActionBar.findViewById(R.id.actionbarTitle);
//        if (titleTv != null) {
//            titleTv.setText(title);
//        }
//    }

    /**
     * actionBar初始化View
     *
     * @param view
     */
    protected void initActionbarView(View view) {
    }



    //记录move次数
    private int mCount;
    //通过move次数下限判定滑动
    private static int MOVE_COUNT_MIN = 3;



    //点击位置是否在屏幕左上角一定位置:返回键
//    public static boolean isPointInLeftUp(float x, float y) {
////        return (x > 0 && x < 100) && (y > offset && y < offset + 150);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mLoadingDialog = null;
    }

//    public Dialog getDialog() {
////        return mLoadingDialog;
//    }

//    public boolean isActivityAlive() {
////        return !this.isFinishing() && !this.isDestroyed();
//    }
}
