package com.cokus.wavelibrary.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

public class WavaTimeView extends SurfaceView implements SurfaceHolder.Callback {
    private final int STATE_IDEL = -1, STATE_STOP = 0, STATE_START = 1, STATE_PAUSE = 2;
    private ArrayList<Short> audioBuf = new ArrayList<>();//绘制波形的音频数据
    private int waveColor = Color.rgb(87, 146, 246);//波形线的颜色
    private int seekColor = Color.rgb(246, 131, 126);//进度的颜色
    private static final long draw_time = 40l;//绘制时间间隔 单位毫秒
    private static final int maxTime = 6;//最大秒数
    private static final int timeHeight = 60;//时间条高度
    private static int timeWidth;//一格时间宽度
    private static int tickCount = 15;//刻度数量
    private boolean needTime = true;
    private static float endPos;//指针固定位置
    private int line_off = 16;//上下边距距离
    private float cursorX = 0;//指针位置
    private float oldX = 0;//上一帧位置
    private float rateX = 0.25f;//绘制速率
    private Canvas canvas;
    private Paint paint;
    private int drawState = STATE_PAUSE;
    private DrawThread drawThread;
    private long startTime = 0, deltTime = 0, passTime = 0;
    private OnDrawingWaveListener drawingListener;

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
        endPos = getMeasuredWidth() * endPos;
        timeWidth = getMeasuredWidth() / maxTime;
        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
            paint.setTextAlign(Paint.Align.LEFT);
        }
        if(deltTime == 0)
            drawEmptyView();
        if (drawThread == null)
            drawThread = new DrawThread(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //resetWaveTimeView();
    }

    /**
     * 设置指针固定位置
     * 取值范围（0,1）
     */
    public void setEndPos(float x) {
        endPos = getMeasuredWidth() > 0 ? getMeasuredWidth() * x : x;
    }

    /**
     * 开始绘制
     */
    public void startDrawWave() {
        drawState = STATE_START;
        drawThread.start();
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
        if (drawThread != null) {
            synchronized (drawThread) {
                drawThread.notify();
            }
        }
    }

    /**
     * 重置视图
     */
    public void resetWaveTimeView() {
        cursorX = 0;
        oldX = 0;
        startTime = 0;
        passTime = 0;
        post(new Runnable() {
            @Override
            public void run() {
                if(drawingListener != null)
                    drawingListener.onUpdateTime(deltTime = 0);
            }
        });
        drawState = STATE_PAUSE;
        audioBuf.clear();
        drawEmptyView();
    }

    public boolean isStart() {
        return drawState == STATE_START;
    }

    public boolean isPause() {
        return drawState == STATE_PAUSE;
    }

    public void updateAudioBuf(short[] buf) {
        synchronized (audioBuf) {
            for (int i = 0; i < buf.length; i += 100) {
                audioBuf.add(Short.valueOf(buf[i]));
            }
        }
        if (drawThread != null) {
            synchronized (drawThread) {
                drawThread.notify();
            }
        }
    }

    public void setDrawingListener(OnDrawingWaveListener listener){
        drawingListener = listener;
    }

    /**
     * 绘制线程
     */
    private class DrawThread extends Thread {
        SurfaceHolder holder;
        long duration = 0;

        public DrawThread(SurfaceHolder holder) {
            this.holder = holder;
        }

        @Override
        public void run() {
            int waveStartPos = 0, sx = 0;
            while (drawState != STATE_STOP) {
                if (cursorX < getMeasuredWidth() && drawState == STATE_START) {
                    if (startTime == 0)
                        startTime = System.currentTimeMillis();
                    deltTime = (System.currentTimeMillis() - startTime) + passTime;
                    synchronized (audioBuf) {
                        if (cursorX <= endPos)
                            cursorX = audioBuf.size() * rateX;
                        if (cursorX > oldX || audioBuf.size() * rateX > oldX) {
                            canvas = holder.lockCanvas(new Rect(0, 0, getWidth(), getHeight()));
                            drawBackGroud();
                            if (cursorX <= endPos) {
                                oldX = cursorX;
                                drawTick(0, waveStartPos/*=0*/);
                            } else {
                                waveStartPos += audioBuf.size() - oldX / rateX;
                                sx += audioBuf.size() * rateX - oldX;
                                if (needTime ) {
                                    if(sx > getWidth() / maxTime) {
                                        sx = (int) (audioBuf.size() * rateX - oldX);
                                        duration++;
                                    }
                                } else if (sx >= getWidth() / tickCount) {
                                    sx = (int) (audioBuf.size() * rateX - oldX);
                                }
                                drawTick(duration, sx);
                                oldX = audioBuf.size() * rateX;
                            }
                            drawSeekLine(seekColor, cursorX);
                            drawAudioWave(waveColor, waveStartPos, (getHeight() + timeHeight) / 2);
                            holder.unlockCanvasAndPost(canvas);
                            canvas = null;

                        }
                    }
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if(drawingListener != null)
                                drawingListener.onUpdateTime(deltTime);
                        }
                    });
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (startTime > 0) {
                        passTime = System.currentTimeMillis() - startTime;
                        startTime = 0;
                    }
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            resetWaveTimeView();
            try {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                    canvas = null;
                }
            } catch (IllegalStateException se) {
                se.printStackTrace();
            } catch (IllegalArgumentException ae) {
                ae.printStackTrace();
            } catch (IllegalMonitorStateException me) {
                me.printStackTrace();
            }

        }
    }

    /**
     * 绘制空场景
     */
    private void drawEmptyView() {
        canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawBackGroud();
            drawTick(startTime, cursorX);
            drawSeekLine(seekColor, cursorX);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * 绘制背景
     */
    private void drawBackGroud() {
        canvas.drawARGB(255, 255, 255, 255);
        drawBaseLine(Color.rgb(169, 169, 169));
        drawCenterLine(Color.rgb(39, 199, 175));
    }

    private void drawTick(long duration, float dx) {
        paint.setColor(Color.BLACK);
        long max = duration + maxTime + 1;
        int i = 0;
        if (needTime) {
            for (long d = duration; d < max; d++) {
                String time = DateUtils.formatElapsedTime(d);
                float px = (i++ * timeWidth - dx);
                canvas.drawText(time, px + 5, 50, paint);
                canvas.drawLine(px, 0, px, line_off + timeHeight, paint);
            }
        } else {
            drawTick(dx);
        }
    }

    private void drawTick(float dx) {
        for (int i = 0; i < tickCount + 1; i++) {
            float x = i * getWidth() / tickCount - dx;
            canvas.drawLine(x, timeHeight, x, timeHeight + line_off, paint);
        }
    }

    /**
     * 绘制基准线
     */
    private void drawBaseLine(int color) {
        paint.setColor(color);
        canvas.drawLine(0, line_off + timeHeight, getMeasuredWidth(), line_off + timeHeight, paint);
        canvas.drawLine(0, getMeasuredHeight() - line_off, getMeasuredWidth(), getMeasuredHeight() - line_off, paint);
    }

    /**
     * 绘制中线
     */
    private void drawCenterLine(int color) {
        paint.setColor(color);
        canvas.drawLine(0, (getMeasuredHeight() + timeHeight) / 2, getMeasuredWidth(), (getMeasuredHeight() + timeHeight) / 2, paint);//中心线
    }

    /**
     * 绘制指针
     */
    private void drawSeekLine(int color, float x) {
        paint.setColor(color);
        canvas.drawCircle(x, line_off / 2 + timeHeight, line_off / 2, paint);// 上面小圆
        canvas.drawCircle(x, getMeasuredHeight() - line_off / 2, line_off / 2, paint);// 下面小圆
        canvas.drawLine(x, timeHeight, x, getMeasuredHeight(), paint);//垂直的线
    }

    /**
     * 绘制波形
     */
    private void drawAudioWave(int color, float x, float maxY) {
        float sy, sx;
        paint.setColor(color);
        ArrayList<Short> tmp;
        tmp = new ArrayList<>(audioBuf.subList((int) x, audioBuf.size()));

        for (int i = 0; i < tmp.size(); i++) {
            sy = maxY - tmp.get(i) * ((maxY - timeHeight - line_off) / Short.MAX_VALUE);
            sx = i * rateX;
            canvas.drawLine(sx + line_off/2, sy, sx + line_off/2, getMeasuredHeight() + timeHeight - sy, paint);
        }
    }

    public interface OnDrawingWaveListener {
        void onUpdateTime(long deltaTime);
    }
}
