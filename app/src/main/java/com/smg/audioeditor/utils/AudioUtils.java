package com.smg.audioeditor.utils;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Executors;

/**
 * Created by Mikiller on 2017/1/23.
 */

public class AudioUtils {
    private static final String TAG = AudioUtils.class.getSimpleName();
    private final int RECORD_STATE_NONE = -1, RECORD_STATE_PREPARED = 0, RECORD_STATE_STARTING = 1, RECORD_STATE_RECORDING = 2, RECORD_STATE_PAUSE = 3, RECORD_STATE_STOP = 4;
    final int AUDIOSOURCE = MediaRecorder.AudioSource.MIC;
    final int SAMPLERATE = 44100;
    AudioRecord audioRecord;
    int audioBufSize = 0;
    short[] tmp;
//    ByteBuffer audioBuf;
    ShortBuffer audioBuf;
    private RecordRunnable recordRunnable;
    private onRecordStateListener recordListener;
    private int recordState = RECORD_STATE_NONE;

    private AudioUtils() {
    }

    private static class AudioUtilsFactory {
        private static AudioUtils instance = new AudioUtils();
    }

    public static AudioUtils getInstance() {
        return AudioUtilsFactory.instance;
    }

    public int getAudioBufSize(){
        return audioBufSize;
    }

    public void init(int channelConfig, int audioFormat) {
        audioBufSize = AudioRecord.getMinBufferSize(SAMPLERATE, channelConfig, audioFormat);
        if (audioBufSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "audio param is wrong");
            return;
        }
        audioRecord = new AudioRecord(AUDIOSOURCE, SAMPLERATE, channelConfig, audioFormat, audioBufSize);
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "init audio failed");
            return;
        }
        recordState = RECORD_STATE_PREPARED;
    }

    public byte[] getAudioData() {
        int readSize = 0;
        if (audioBuf == null) {
//            audioBuf = ByteBuffer.allocate(audioBufSize * audioRecord.getChannelCount());
            audioBuf = ShortBuffer.allocate(audioBufSize * audioRecord.getChannelCount());
        } else {
            audioBuf.clear();
        }
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            readSize = audioRecord.read(audioBuf.array(), 0, audioBufSize * audioRecord.getChannelCount());
            if (readSize < 0) {
                Log.e(TAG, "get audio failed");
            }
        } else {
            if (tmp == null)
//                tmp = new byte[audioBufSize * audioRecord.getChannelCount()];
                tmp = new short[audioBufSize * audioRecord.getChannelCount()];
            audioBuf.put(tmp);
        }
        byte[] byts = new byte[readSize * 2];
        for(int i = 0; i < readSize; i++){
            byte[] b = getBytes(audioBuf.get(i));
            byts[i * 2] = b[0];
            byts[i * 2 + 1] = b[1];
        }
        return byts;
//        return audioBuf.array();
    }

    public byte[] getBytes(short s) {
        byte[] buf = new byte[2];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) (s & 0x00ff);
            s >>= 8;
        }
        return buf;
    }

    public void start() {
        if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.STATE_INITIALIZED)
            audioRecord.startRecording();
        if(recordRunnable != null)
            recordRunnable.setRecording(true);
        recordState = RECORD_STATE_RECORDING;
    }

    public void pause() {
        if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.stop();
        }
        if(recordRunnable != null)
            recordRunnable.setRecording(false);
        recordState = RECORD_STATE_PAUSE;
    }

    public void release() {
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
    }

    public void setRecordListener(onRecordStateListener listener){
        recordListener = listener;
    }

    public boolean isRecordPrepared(){
        return recordState == RECORD_STATE_PREPARED;
    }

    public boolean isRecordStarting(){
        return recordState == RECORD_STATE_STARTING;
    }

    public boolean isRecordRecording(){
        return recordState == RECORD_STATE_RECORDING;
    }

    public boolean isRecordPause(){
        return recordState == RECORD_STATE_PAUSE;
    }

    public void startRecordThread(String dirPath){
        if(recordRunnable == null)
            recordRunnable = new RecordRunnable(dirPath);
        File rawFile = new File(dirPath + "tmp" + System.currentTimeMillis() + ".raw");
        if (!rawFile.exists()) {
            try {
                rawFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        recordRunnable.setRawFile(rawFile);
        recordRunnable.setStarting(true);
        Executors.newCachedThreadPool().execute(recordRunnable);
        recordState = RECORD_STATE_STARTING;
    }

    public void stopRecordThread(){
        if(recordRunnable != null)
            recordRunnable.setStarting(false);
        recordState = RECORD_STATE_STOP;
    }

    public class RecordRunnable implements Runnable {
        private FileOutputStream fos;
        private boolean isStarting = false, isRecording = false;
        private File rawFile;
        private String dirPath;

        public RecordRunnable(String dirPath){
            this.dirPath = dirPath;
        }

        public void setRawFile(File rawFile){
            this.rawFile = rawFile;
        }

        public void setStarting(boolean isStarting){
            this.isStarting = isStarting;
        }

        public void setRecording(boolean isRecording){
            this.isRecording = isRecording;
        }

        @Override
        public void run() {
            while (isStarting) {
                if (fos == null)
                    try {
                        fos = new FileOutputStream(rawFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                if (isRecording) {
                    byte[] buffer = getAudioData();
                    try {
                        fos.write(buffer);
                        fos.flush();
                        if(recordListener != null)
                            recordListener.onRecording(audioBuf.array());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fos = null;
            File wavFile = new File(dirPath + "rst" + System.currentTimeMillis() + ".wav");
            try {
                wavFile.createNewFile();
                copyWaveFile(rawFile.getAbsolutePath(), wavFile.getAbsolutePath());
                if(recordListener != null)
                    recordListener.onRecordStop(wavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            recordState = RECORD_STATE_PREPARED;
        }
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, final String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        int channels = audioRecord.getChannelCount();
        long byteRate = 16 * channels / 8;
        byte[] data = new byte[audioBufSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                    long totalDataLen, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (SAMPLERATE & 0xff);
        header[25] = (byte) ((SAMPLERATE >> 8) & 0xff);
        header[26] = (byte) ((SAMPLERATE >> 16) & 0xff);
        header[27] = (byte) ((SAMPLERATE >> 24) & 0xff);
        header[28] = (byte) ((byteRate * SAMPLERATE) & 0xff);
        header[29] = (byte) (((byteRate * SAMPLERATE) >> 8) & 0xff);
        header[30] = (byte) (((byteRate * SAMPLERATE) >> 16) & 0xff);
        header[31] = (byte) (((byteRate * SAMPLERATE) >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    public interface onRecordStateListener{
        void onRecording(short[] audioBuf);
        void onRecordStop(File rstFile);
    }
}
