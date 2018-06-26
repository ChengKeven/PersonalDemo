package com.example.cheng.personaldemo.Video.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.cheng.personaldemo.R;
import com.example.cheng.personaldemo.Video.Utils;


/**
 * Created by wanbo on 2017/1/20.
 * 控制视频：是否保存图片或视频
 */

public class MediaControlView extends RelativeLayout {

    public ImageView backLayout,selectLayout;

    public MediaControlView(Context context) {
        super(context);
        init(context);
    }

    public MediaControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        LayoutParams params = new LayoutParams(Utils.getInstance(context).getWidthPixels(), Utils.getInstance(context).dp2px(180f));
        setLayoutParams(params);
        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.widget_view_send_btn,null,false);
        layout.setLayoutParams(params);
        backLayout =  layout.findViewById(R.id.return_layout);
        selectLayout = layout.findViewById(R.id.select_layout);
        addView(layout);
        setVisibility(GONE);
    }

    private static final float TRANSLATION_VALUE = 300;
    public void startAnim(){
        setVisibility(VISIBLE);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(backLayout,"translationX",0,-TRANSLATION_VALUE),
                ObjectAnimator.ofFloat(selectLayout,"translationX",0,TRANSLATION_VALUE)
        );
        set.setDuration(300).start();
    }

    public void stopAnim(){
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(backLayout,"translationX",-TRANSLATION_VALUE,0),
                ObjectAnimator.ofFloat(selectLayout,"translationX",TRANSLATION_VALUE,0)
        );
        set.setDuration(300).start();
        setVisibility(GONE);
    }

}
