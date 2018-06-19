package com.smg.audioeditor.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.smg.audioeditor.R;

/**
 * Created by Mikiller on 2018/5/31.
 */

public class AudioProgress extends LinearLayout {
    public final static int MODE_RECORD = 0, MODE_PLAY = 1;
    private TextView tv_cTime, tv_duration;
    private SeekBar pgs_time;
    private CheckBox btn_switch;
    private ImageButton btn_stop;
    private int mode;
    private OnControllorListener controllorListener;

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

    private void initView(final Context context, @Nullable AttributeSet attrs, int defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.layout_progress, this);
        tv_cTime = findViewById(R.id.tv_cTime);
        tv_duration = findViewById(R.id.tv_duration);
        pgs_time = findViewById(R.id.pgs_time);
        btn_switch = findViewById(R.id.btn_switch);
        btn_stop = findViewById(R.id.btn_stop);

        btn_switch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(controllorListener == null)
                    return;
                if(btn_switch.isChecked()){
                    controllorListener.onStart();
                }else{
                    controllorListener.onPause();
                }
            }
        });
        btn_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(controllorListener != null)
                    controllorListener.onStop();
            }
        });
    }

    public void setMode(int mode){
        this.mode = mode;
    }

    public boolean isRecordMode(){
        return mode == MODE_RECORD;
    }

    public boolean isPlayMode(){
        return mode == MODE_PLAY;
    }

    public void setControllorListener(OnControllorListener listener){
        controllorListener = listener;
    }

    public void setTv_duration(long duration){
        tv_duration.setText(DateUtils.formatElapsedTime(duration/1000));
        pgs_time.setMax(mode == MODE_PLAY ? (int) duration : 0);
    }

    public void updateCTime(long currentTime){
        tv_cTime.setText(DateUtils.formatElapsedTime(currentTime/1000));
        if(mode == MODE_PLAY)
            pgs_time.setProgress((int) currentTime);
    }

    public interface OnControllorListener{
        void onStart();
        void onStop();
        void onPause();
    }
}
