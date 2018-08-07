package com.smg.audioeditor.activities;

import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cokus.wavelibrary.view.WavaTimeView;
import com.smg.audioeditor.R;
import com.smg.audioeditor.adapters.FileRcvAdapter;
import com.smg.audioeditor.base.BaseActivity;
import com.smg.audioeditor.utils.AudioUtils;
import com.cokus.wavelibrary.view.AudioProgress;
import com.smg.audioeditor.utils.StringUtils;
import com.uilib.utils.ToastUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    @BindView(R.id.tv_duration)
    TextView tv_duration;
    FileRcvAdapter adapter;
    AudioUtils audioUtils;
    String dirPath;

//    MediaPlayer player;
//    Visualizer visualizer;
//    Visualizer.OnDataCaptureListener captureListener;
//    Timer timer;
    int recordTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_main);
    }

    @Override
    protected void initView() {
        rcv_file.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rcv_file.setAdapter(adapter = new FileRcvAdapter());
        adapter.setItemClickListener(new FileRcvAdapter.onItemClickListener() {
            @Override
            public void onItemClick(File file) {
                tv_dirPath.setText(dirPath + file.getName());
            }
        });
        checkDirPath();

        waveView.setEndPos(0.5f);
        waveView.setDrawingListener(new WavaTimeView.OnDrawingWaveListener() {
            @Override
            public void onUpdateTime(long deltaTime) {
                tv_duration.setText(StringUtils.getMillisToStr(deltaTime));
            }
        });
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pgs_audio.setMode(AudioProgress.MODE_RECORD);
                if (audioUtils.isRecordPrepared()) {
                    audioUtils.startRecordThread(dirPath);
                }
            }
        });

        tv_dirPath.setText(dirPath);

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pgs_audio.resetPlayer(tv_dirPath.getText().toString());
                waveView.setEndPos(0.9f);
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
                    }
                }
                waveView.startDrawWave();
            }

            @Override
            public void onStop() {
                waveView.resetWaveTimeView();
                waveView.stopDrawWave();
                if (pgs_audio.isRecordMode()) {
                    audioUtils.stopRecordThread();
                }
                waveView.stopDrawWave();

            }

            @Override
            public void onPause() {
                if (pgs_audio.isRecordMode()) {
                    if (audioUtils.isRecordPrepared()) {
                        ToastUtils.makeToast(TestActivity.this, "先点击录音");
                        return;
                    }
                    if (audioUtils.isRecordRecording()) {
                        audioUtils.pause();
                    }
                }
                waveView.pauseDrawWave();
            }
        });

        pgs_audio.setVisualizerListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                //waveView.updateAudioBuf(fft);
                int count = fft.length >> 1;
                short[] dest = new short[count];
                for (int i = 0; i < count; i++) {
                    dest[i] = (short) (fft[i * 2] << 8 | fft[2 * i + 1] & 0xff);
                }
                waveView.updateAudioBuf(dest);
            }
        });

        audioUtils = AudioUtils.getInstance();
        audioUtils.init(AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioUtils.setRecordListener(new AudioUtils.onRecordStateListener() {

            @Override
            public void onRecording(short[] audioBuf) {
                if (waveView != null) {
                    waveView.updateAudioBuf(audioBuf);
                }
            }

            @Override
            public void onRecordStop(final File rstFile) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordTime = 0;
                        if (adapter != null)
                            adapter.setFileList(rstFile);
                    }
                });
            }
        });
    }

    private void checkDirPath() {
        final File dir = new File(dirPath = Environment.getExternalStorageDirectory() + "/aa/");
        if (dir.exists()) {
            List<File> files = Arrays.asList(dir.listFiles());
            if (files != null) {
                List<File> tmp = new ArrayList<>(files);
                for (File file : files) {
                    if (!file.getName().endsWith("wav"))
                        tmp.remove(file);
                }
                adapter.setFileList(tmp);
            }
        } else {
            dir.mkdirs();
        }
    }

    @Override
    protected void initData() {

    }

    @Override
    public void finish() {
        pgs_audio.release();
        audioUtils.release();
        super.finish();

    }
}
