package com.example.cheng.personaldemo.base;

import android.view.View;

public abstract class NoMultiClickListener implements View.OnClickListener {
    public static final int MIN_CLICK_DELAY_TIME = 1000;
    private long endTime;

    @Override
    public void onClick(View v) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - endTime > MIN_CLICK_DELAY_TIME) {
            onNoMultiClick(v);
            endTime = currentTime;
        }
    }

    public abstract void onNoMultiClick(View v);
}
