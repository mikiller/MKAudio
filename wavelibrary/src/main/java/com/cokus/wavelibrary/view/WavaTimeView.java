package com.cokus.wavelibrary.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by chenzhuo on 2017/3/30.
 * 实时绘制波形视图  有之前surfaceview 改为 view
 * 原因很直接 surfaceview 解决不了刷新卡顿问题，最好的选择只有View了
 */

public class WavaTimeView extends SurfaceView implements SurfaceHolder.Callback {
    private final int STATE_IDEL = -1, STATE_STOP = 0, STATE_START = 1, STATE_PAUSE = 2;
    private ArrayList<Byte> audioBuf = new ArrayList<>();//绘制波形的音频数据
    private int lineColor;//波形线的颜色
    private int seekColor;//进度的颜色
    private static final int draw_time = 10;//绘制时间间隔 单位毫秒
    private int line_off = 32;//上下边距距离
    private float startX = 0, oldX = 0;
    private Canvas canvas;
    private Paint paint;
    private int drawState = STATE_PAUSE;
    private DrawThread drawThread;


    public WavaTimeView(Context context) {
        this(context, null, 0);
    }

    public WavaTimeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WavaTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        getHolder().addCallback(this);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        audioBuf.clear();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
        }
        canvas = holder.lockCanvas(new Rect(0, 0, getWidth(), getHeight()));
        drawBackGroud();
        drawSeekLine(Color.rgb(246, 131, 126), startX);
        holder.unlockCanvasAndPost(canvas);

        if(drawThread == null)
            drawThread = new DrawThread(holder);
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        resetWaveTimeView(false);
    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//
//        this.canvas = canvas;
//        if(paint == null){
//            paint = new Paint();
//            paint.setAntiAlias(true);
//        }
//        drawBackGroud();
//        drawSeekLine(Color.rgb(246, 131, 126), startX);
//        //drawAudioWave(Color.rgb(131, 246, 146), startX, getMeasuredHeight() / 2);
//    }

    /**
     * 开始绘制
     */
    public void startDrawWave() {
        drawState = STATE_START;
    }

    /**
     * 暂停绘制
     */
    public void pauseDrawWave() {
        drawState = STATE_PAUSE;
    }

    /**
     * 停止绘制
     */
    public void stopDrawWave() {
        drawState = STATE_STOP;
    }

    /**
     * 重置视图
     */
    public void resetWaveTimeView(boolean needRestart) {
        stopDrawWave();
        drawState = STATE_PAUSE;
        startX = 0;
        audioBuf.clear();
        canvas = getHolder().lockCanvas();
        if (canvas != null) {
            //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清除画布
            drawBackGroud();
            drawSeekLine(Color.rgb(246, 131, 126), startX);
            getHolder().unlockCanvasAndPost(canvas);
        }
        //invalidate();
        if(needRestart && drawThread != null)
            drawThread.start();
    }

    public boolean isStart() {
        return drawState == STATE_START;
    }

    public boolean isPause() {
        return drawState == STATE_PAUSE;
    }

    public void updateAudioBuf(byte[] buf) {
        synchronized (audioBuf) {
            for (int i = 0; i < buf.length; i += 100) {
                //tmp[i/100] = Byte.valueOf(buf[i]);
                audioBuf.add(Byte.valueOf(buf[i]));
            }
        }
//        audioBuf = new ArrayList<>(Arrays.asList(tmp));
        //audioBuf.addAll(Arrays.asList(tmp));
    }

    /**
     * 绘制背景
     */
    private void drawBackGroud() {
        canvas.drawARGB(255, 239, 239, 239);
        drawBaseLine(Color.rgb(169, 169, 169));
        drawCenterLine(Color.rgb(39, 199, 175));
    }


    /**
     * 绘制线程
     */
    private class DrawThread extends Thread {
        SurfaceHolder holder;

        public DrawThread(SurfaceHolder holder) {
            this.holder = holder;
        }

        @Override
        public void run() {
            while (drawState != STATE_STOP) {
                if (startX < getMeasuredWidth() && drawState == STATE_START) {
                    synchronized (audioBuf) {
                        startX = audioBuf.size() * 0.2f;
                    }
                    //Log.e("wave", "startx: " + startX + ", oldx: " + oldX);
                    canvas = holder.lockCanvas(new Rect((int) (startX - line_off), 0, (int) (startX + line_off )/*getWidth()*/, getHeight()));
                    drawBackGroud();
                    drawSeekLine(Color.rgb(246, 131, 126), startX);
                    drawAudioWave(Color.rgb(87, 146, 246), oldX - line_off, getHeight() / 2);
                    oldX = startX;
                    holder.unlockCanvasAndPost(canvas);
//                    WavaTimeView.this.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            invalidate();
//                        }
//                    });
                    try {
                        Thread.sleep(10l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void drawBaseLine(int color) {
        paint.setColor(color);
        canvas.drawLine(0, line_off / 2, getMeasuredWidth(), line_off / 2, paint);
        canvas.drawLine(0, getMeasuredHeight() - line_off / 2, getMeasuredWidth(), getMeasuredHeight() - line_off / 2, paint);
    }

    private void drawCenterLine(int color) {
        paint.setColor(color);
        canvas.drawLine(0, getMeasuredHeight() / 2, getMeasuredWidth(), getMeasuredHeight() / 2, paint);//中心线
    }

    private void drawSeekLine(int color, float x) {
        paint.setColor(color);
        canvas.drawCircle(x, line_off / 4, line_off / 4, paint);// 上面小圆
        canvas.drawCircle(x, getMeasuredHeight() - line_off / 4, line_off / 4, paint);// 下面小圆
        canvas.drawLine(x, 0, x, getMeasuredHeight(), paint);//垂直的线
    }

    private void drawAudioWave(int color, float x, float y) {
        float sy, sx;
        paint.setColor(color);
        for (int i = (int) (x <0 ? 0 : x); i < audioBuf.size(); i++) {
            sy = audioBuf.get(i) * (y / 127f / 2) + y - line_off;
            sx = i * 0.2f;
            //Log.e("wave", "buf: " + audioBuf.get(i) + ", sx: " + sx + ", sy: " + sy + ", y: " + y);
            canvas.drawLine(sx, sy, sx, y, paint);
        }
    }

}
