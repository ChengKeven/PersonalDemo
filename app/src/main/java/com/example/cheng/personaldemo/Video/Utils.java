package com.example.cheng.personaldemo.Video;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.DisplayMetrics;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by wanbo on 2017/1/20.
 */

public class Utils {

    private static Utils mInstance = null;
    private Context context;

    public static Utils getInstance(Context context) {
        if (mInstance == null) {
            synchronized (Utils.class) {
                if (mInstance == null) {
                    mInstance = new Utils(context);
                }
            }
        }
        return mInstance;
    }

    private Utils(Context context) {
        this.context = context;
    }

    public int getWidthPixels() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Configuration cf = context.getResources().getConfiguration();
        int ori = cf.orientation;
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {// 横屏
            return displayMetrics.heightPixels;
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {// 竖屏
            return displayMetrics.widthPixels;
        }
        return 0;
    }

    public int getHeightPixels() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Configuration cf = context.getResources().getConfiguration();
        int ori = cf.orientation;
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {// 横屏
            return displayMetrics.widthPixels;
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {// 竖屏
            return displayMetrics.heightPixels;
        }
        return 0;
    }

    public int dp2px(float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int px2dp(float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public File getFileFromSd(){
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (file != null){
            File[] files = file.listFiles();
            File videoFirst = null;
            if (files.length > 0){
                 videoFirst = files[files.length-1];
            }
            return videoFirst;
        }
        return null;
    }

    public static ByteArrayInputStream getByteArrayInputStream(File file){
        return new ByteArrayInputStream(getByetsFromFile(file));
    }
    /**
     *  ByteArrayInputStream ins = new ByteArrayInputStream(picBytes);
     * @param file
     * @return
     */
    public static byte[] getByetsFromFile(File file){
        FileInputStream is = null;
        // 获取文件大小
        long length = file.length();
        // 创建一个数据来保存文件数据
        byte[] fileData = new byte[(int)length];

        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int bytesRead=0;
        // 读取数据到byte数组中
        while(bytesRead != fileData.length) {
            try {
                bytesRead += is.read(fileData, bytesRead, fileData.length - bytesRead);
                if(is != null)
                    is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return fileData;
    }
}
