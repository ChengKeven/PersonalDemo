package com.example.cheng.personaldemo.util;

import android.content.Context;
import android.widget.Toast;



/**
 * Toast统一管理类
 *
 * @author way
 */
public class T {
    // Toast
    private static Toast toast;

    /**
     * 短时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showShort(final Context context, final CharSequence message) {
        if (context == null || message == null) {
            return;
        }
        ThreadHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (null == toast) {
                    toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                } else {
                    try {
                        toast.setText(message);
                    } catch (RuntimeException e) {
                        toast.cancel();
                        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                    }
                }
                toast.show();
            }
        });
    }

    /**
     * 短时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showShort(Context context, int message) {
        if (null == toast) {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        } else {
            try {
                toast.setText(message);
            } catch (RuntimeException e) {
                toast.cancel();
                toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            }
        }
        toast.show();
    }

    /**
     * 长时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showLong(Context context, CharSequence message) {
        if (null == toast) {
            toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        } else {
            toast.setText(message);
        }
        toast.show();
    }

    /**
     * 长时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showLong(Context context, int message) {
        if (null == toast) {
            toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        } else {
            toast.setText(message);
        }
        toast.show();
    }

    /**
     * 自定义显示Toast时间
     *
     * @param context
     * @param message
     * @param duration
     */
    public static void show(Context context, CharSequence message, int duration) {
        if (null == toast) {
            toast = Toast.makeText(context, message, duration);
        } else {
            toast.setText(message);
        }
        toast.show();
    }

    /**
     * 自定义显示Toast时间
     *
     * @param context
     * @param message
     * @param duration
     */
    public static void show(Context context, int message, int duration) {
        if (null == toast) {
            toast = Toast.makeText(context, message, duration);
        } else {
            toast.setText(message);
        }
        toast.show();
    }

    /**
     * Hide the toast, if any.
     */
    public static void hideToast() {
        if (null != toast) {
            toast.cancel();
        }
    }





}
