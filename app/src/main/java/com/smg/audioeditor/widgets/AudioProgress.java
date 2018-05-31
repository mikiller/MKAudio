package com.smg.audioeditor.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.smg.audioeditor.R;

/**
 * Created by Mikiller on 2018/5/31.
 */

public class AudioProgress extends LinearLayout {
    private TextView tv_cTime, tv_duration;
    private SeekBar pgs_time;
    public AudioProgress(Context context) {
        this(context, null, 0);
    }

    public AudioProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.layout_progress, this);
        tv_cTime = findViewById(R.id.tv_cTime);
        tv_duration = findViewById(R.id.tv_duration);
        pgs_time = findViewById(R.id.pgs_time);
    }

    public void setTv_duration(long duration){
        tv_duration.setText(DateUtils.formatElapsedTime(duration/1000));
        pgs_time.setMax((int) duration);
    }

    public void updateCTime(long currentTime){
        tv_cTime.setText(DateUtils.formatElapsedTime(currentTime/1000));
        pgs_time.setProgress((int) currentTime);
    }
}
