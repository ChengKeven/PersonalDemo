package com.example.cheng.personaldemo.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;

/**
 * handy class for thread dispatching
 */
public class ThreadHelper {

    private static MyHandlerThread mThread;

    static  {
        mThread = new MyHandlerThread("ThreadHelper-Thread");
        mThread.start();
    }

    //--------------------- background thread---------------------//

    public static void runOnBackground(Runnable task) {
        // not in main thread, run immediately
        if (Thread.currentThread() == mThread) {
            task.run();
            return;
        }

        mThread.post(task);
    }

    public static void removeCallbacks(Runnable task) {
        mThread.removeCallbacks(task);
    }

    private static class MyHandlerThread extends HandlerThread {
        CountDownLatch mLatch = new CountDownLatch(1);
        Handler handler = null;

        public MyHandlerThread(String name) {
            super(name);
        }

        protected void onLooperPrepared() {
            handler = new Handler();
            mLatch.countDown();
        }

        void post(Runnable task) {
            ensureHandler();
            handler.post(task);
        }

        void postDelay(Runnable task, long delayMillis) {
            ensureHandler();
            handler.postDelayed(task, delayMillis);
        }

        void removeCallbacks(Runnable task) {
            ensureHandler();
            handler.removeCallbacks(task);
        }

        private void ensureHandler() {
            if (handler == null) {
                try {
                    mLatch.await();
                } catch (InterruptedException e) {
                }
            }
        }

    }

    //--------------------- UI thread---------------------//

    private static Handler mUIHandler = new Handler(Looper.getMainLooper());

    public static void runOnUIThread(Runnable r) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            r.run();
        } else {
            mUIHandler.post(r);
        }
    }

    public static void postOnUIThread(Runnable task) {
        mUIHandler.post(task);
    }

    public static void postOnUIThread(Runnable task, long delayMillis) {
        mUIHandler.postDelayed(task, delayMillis);
    }

}
