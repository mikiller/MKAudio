package com.cokus.wavelibrary.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cokus.wavelibrary.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


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
    MediaPlayer player;
    Visualizer visualizer;
    Visualizer.OnDataCaptureListener captureListener;
    Timer timer;

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
                if(btn_switch.isChecked()){
                    if(mode == MODE_PLAY){
                        player.start();
                        if (visualizer != null)
                            visualizer.setEnabled(true);
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateCTime(player.getCurrentPosition());
                                    }
                                });
                            }
                        }, 0l, 1000l);
                    }
                    if(controllorListener != null)
                        controllorListener.onStart();
//                    else{
//                        player.start();
//                        if (visualizer != null)
//                            visualizer.setEnabled(true);
//                        timer = new Timer();
//                        timer.schedule(new TimerTask() {
//                            @Override
//                            public void run() {
//                                post(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        updateCTime(player.getCurrentPosition());
//                                    }
//                                });
//                            }
//                        }, 0l, 1000l);
//                    }
                }else{
                    if(mode == MODE_PLAY){
                        player.pause();
                        if(timer != null)
                            timer.cancel();
                    }
                    if(controllorListener != null)
                        controllorListener.onPause();
                }
            }
        });
        btn_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode == MODE_PLAY){
                    player.stop();
                    if (visualizer != null)
                        visualizer.setEnabled(false);
                    if (timer != null)
                        timer.cancel();
                }
                if(controllorListener != null)
                    controllorListener.onStop();
            }
        });

        initPlayer();
    }

    private void initPlayer(){
        player = new MediaPlayer();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                setTv_duration(mp.getDuration());
                updateCTime(mp.getCurrentPosition());

                visualizer = new Visualizer(mp.getAudioSessionId());
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                visualizer.setDataCaptureListener(captureListener, Visualizer.getMaxCaptureRate() / 2, true, false);
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                player.reset();
                return false;
            }
        });
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (timer != null)
                    timer.cancel();
                updateCTime(player.getCurrentPosition());
                if (visualizer != null)
                    visualizer.setEnabled(false);
                //tv_dirPath.setText(dirPath);
                //pgs_audio.setVisibility(View.INVISIBLE);
                //player.reset();
            }
        });


//        captureListener = new Visualizer.OnDataCaptureListener() {
//            @Override
//            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
////                String wave = new String(waveform);
////                Log.e(TAG, "wave: " + wave);
//                //waveView.updateAudioBuf(waveform);
//            }
//
//            @Override
//            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//                //waveView.updateAudioBuf(fft);
//            }
//        };
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

    public void setVisualizerListener(Visualizer.OnDataCaptureListener listener){
        captureListener = listener;
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

    public void updateCTimeMillis(long currentTime){
        long passTime = currentTime;
        int tmp = (int) (passTime % 1000l) + 1000;
        String millis = String.valueOf(tmp).substring(1, 4);
        String time = DateUtils.formatElapsedTime(passTime/1000);
        tv_cTime.setText(String.format("%1$s:%2$s", time, millis));
    }

    public void togglePlay() {
        btn_switch.performClick();
    }

    public void resetPlayer(String dataSource){
        if (player != null) {
            setMode(AudioProgress.MODE_PLAY);
            if (player.isPlaying()) {
                player.stop();
            }
            try {
                player.reset();
                player.setDataSource(dataSource);
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public void release(){
        player.release();
        visualizer.release();
    }

    public interface OnControllorListener{
        void onStart();
        void onStop();
        void onPause();
    }
}
