package com.smg.audioeditor.activities;

import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cokus.wavelibrary.view.WavaTimeView;
import com.smg.audioeditor.R;
import com.smg.audioeditor.adapters.FileRcvAdapter;
import com.smg.audioeditor.base.BaseActivity;
import com.smg.audioeditor.utils.AudioUtils;
import com.smg.audioeditor.widgets.AudioProgress;
import com.uilib.utils.ToastUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import butterknife.BindView;

/**
 * Created by Mikiller on 2018/5/18.
 */

public class TestActivity extends BaseActivity {

    @BindView(R.id.btn_record)
    Button btn_record;
    @BindView(R.id.btn_play)
    Button btn_play;
//    @BindView(R.id.btn_switch)
//    ImageButton btn_switch;
//    @BindView(R.id.btn_stop)
//    ImageButton btn_stop;
    @BindView(R.id.tv_dirPath)
    TextView tv_dirPath;
    @BindView(R.id.rcv_file)
    RecyclerView rcv_file;
    @BindView(R.id.pgs_audio)
    AudioProgress pgs_audio;
    @BindView(R.id.waveView)
    WavaTimeView waveView;

    FileRcvAdapter adapter;
    AudioUtils audioUtils;
    String dirPath;

    MediaPlayer player;
    Visualizer visualizer;
    Visualizer.OnDataCaptureListener captureListener;
    Timer timer;
    int recordTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_main);
    }

    Runnable tmp = new Runnable() {
        @Override
        public void run() {
            while (true) {
                Log.e(TAG, "run");
//                if (!isRecording)
//                    break;
            }
        }
    };

    @Override
    protected void initView() {
        dirPath = Environment.getExternalStorageDirectory() + "/aa/";
        rcv_file.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rcv_file.setAdapter(adapter = new FileRcvAdapter());
        adapter.setItemClickListener(new FileRcvAdapter.onItemClickListener() {
            @Override
            public void onItemClick(File file) {

                if(player != null && !player.isPlaying())

                        //pgs_audio.setMode(AudioProgress.MODE_PLAY);
                        //player.setDataSource(file.getAbsolutePath());
                        //player.prepare();
                        tv_dirPath.setText(dirPath + file.getName());


            }
        });
        final File dir = new File(dirPath);
        if (dir.exists()) {
            List<File> files = Arrays.asList(dir.listFiles());
            if (files != null) {
                List<File> tmp = new ArrayList<>(files);
                for(File file : files){
                    if(!file.getName().endsWith("wav"))
                        tmp.remove(file);
                }
                adapter.setFileList(tmp);
            }
        } else {
            dir.mkdirs();
        }

        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pgs_audio.setMode(AudioProgress.MODE_RECORD);
                if(audioUtils.isRecordPrepared()){
                    audioUtils.startRecordThread(dirPath);
                }
            }
        });

        tv_dirPath.setText(dirPath);

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(player != null) {
                    pgs_audio.setMode(AudioProgress.MODE_PLAY);
                    if(player.isPlaying()){
                        player.stop();
                    }
                    try {
                        player.reset();
                        player.setDataSource(tv_dirPath.getText().toString());
                        player.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                }
            }
        });

        pgs_audio.setControllorListener(new AudioProgress.OnControllorListener() {
            @Override
            public void onStart() {
                if (pgs_audio.isRecordMode()) {
                    if (audioUtils.isRecordPrepared()) {
                        ToastUtils.makeToast(TestActivity.this, "先点击录音");
                        return;
                    }
                    if (audioUtils.isRecordPause() || audioUtils.isRecordStarting()) {
                        audioUtils.start();
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                pgs_audio.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        pgs_audio.updateCTime(recordTime += 1000);
                                    }
                                });
                            }
                        }, 0l, 1000l);
                        //btn_switch.setImageResource(android.R.drawable.ic_media_pause);
                    }
                }else{
                    player.start();
                    if(visualizer != null)
                        visualizer.setEnabled(true);
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            pgs_audio.post(new Runnable() {
                                @Override
                                public void run() {
                                    pgs_audio.updateCTime(player.getCurrentPosition());
                                }
                            });
                        }
                    }, 0l, 1000l);
                }
                waveView.startDrawWave();
            }

            @Override
            public void onStop() {
                if(pgs_audio.isRecordMode()) {
                    audioUtils.stopRecordThread();

                }else{
                    player.stop();
                    if(visualizer != null)
                        visualizer.setEnabled(false);
                }
                if (timer != null)
                    timer.cancel();
                //waveView.stopDrawWave();
                waveView.resetWaveTimeView(true);
            }

            @Override
            public void onPause() {
                if(pgs_audio.isRecordMode()) {
                    if (audioUtils.isRecordPrepared()) {
                        ToastUtils.makeToast(TestActivity.this, "先点击录音");
                        return;
                    }
                    if (audioUtils.isRecordRecording()) {
                        audioUtils.pause();
                        //btn_switch.setImageResource(android.R.drawable.ic_media_play);
                    }
                }else{
                    player.pause();
                }
                if(timer != null)
                    timer.cancel();
                waveView.pauseDrawWave();
            }
        });
    }

    @Override
    protected void initData() {
        audioUtils = AudioUtils.getInstance();
        audioUtils.init(AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioUtils.setRecordListener(new AudioUtils.onRecordStateListener() {
            @Override
            public void onRecording(byte[] audioBuf) {
                //Log.e(TAG, Arrays.toString(audioBuf));
                waveView.updateAudioBuf(audioBuf);
            }

            @Override
            public void onRecordStop(final File rstFile) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordTime = 0;
                        if(adapter != null)
                            adapter.setFileList(rstFile);
                    }
                });
            }
        });

        player = new MediaPlayer();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                pgs_audio.setTv_duration(mp.getDuration());
                pgs_audio.updateCTime(mp.getCurrentPosition());

                visualizer = new Visualizer(mp.getAudioSessionId());
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                visualizer.setDataCaptureListener(captureListener, Visualizer.getMaxCaptureRate()/2, true, true);
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
                if(timer != null)
                    timer.cancel();
                pgs_audio.updateCTime(player.getCurrentPosition());
                if(visualizer != null)
                    visualizer.setEnabled(false);
                tv_dirPath.setText(dirPath);
                //pgs_audio.setVisibility(View.INVISIBLE);
                //player.reset();
            }
        });

        captureListener = new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
//                String wave = new String(waveform);
//                Log.e(TAG, "wave: " + wave);
                //waveView.updateAudioBuf(waveform);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                waveView.updateAudioBuf(fft);
            }
        };
    }

    @Override
    public void finish() {
        super.finish();
        audioUtils.release();
    }
}
