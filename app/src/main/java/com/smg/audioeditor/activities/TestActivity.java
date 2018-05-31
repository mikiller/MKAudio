package com.smg.audioeditor.activities;

import android.media.AudioFormat;
import android.media.MediaPlayer;
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

import com.smg.audioeditor.R;
import com.smg.audioeditor.adapters.FileRcvAdapter;
import com.smg.audioeditor.base.BaseActivity;
import com.smg.audioeditor.utils.AudioUtils;
import com.uilib.utils.ToastUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    @BindView(R.id.btn_switch)
    ImageButton btn_switch;
    @BindView(R.id.btn_stop)
    ImageButton btn_stop;
    @BindView(R.id.tv_dirPath)
    TextView tv_dirPath;
    @BindView(R.id.rcv_file)
    RecyclerView rcv_file;

    FileRcvAdapter adapter;
    AudioUtils audioUtils;
    String dirPath;

    MediaPlayer player;

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
                    try {
                        player.setDataSource(file.getAbsolutePath());
                        player.prepare();
                        tv_dirPath.setText(dirPath + file.getName());
                        //player.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

            }
        });
        final File dir = new File(dirPath);
        if (dir.exists()) {
            List<File> files = Arrays.asList(dir.listFiles());
            if (files != null) {
                adapter.setFileList(files);
            }
        } else {
            dir.mkdirs();
        }

        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioUtils.isRecordPrepared()){
                    audioUtils.startRecordThread(dirPath);
                }
            }
        });
        btn_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioUtils.isRecordPrepared()) {
                    ToastUtils.makeToast(TestActivity.this, "先点击录音");
                    return;
                }
                if (audioUtils.isRecordPause() || audioUtils.isRecordStarting()) {
                    audioUtils.start();
                    btn_switch.setImageResource(android.R.drawable.ic_media_pause);
                } else if(audioUtils.isRecordRecording()){
                    audioUtils.pause();
                    btn_switch.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioUtils.stopRecordThread();
            }
        });

        tv_dirPath.setText(dirPath);

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(player != null)
                    player.start();
            }
        });

    }

    @Override
    protected void initData() {
        audioUtils = AudioUtils.getInstance();
        audioUtils.init(AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioUtils.setRecordListener(new AudioUtils.onRecordStateListener() {
            @Override
            public void onRecordStop(final File rstFile) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(adapter != null)
                            adapter.setFileList(rstFile);
                    }
                });
            }
        });

        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                tv_dirPath.setText(dirPath);
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        audioUtils.release();
    }
}
