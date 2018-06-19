package com.cokus.wavelibrary.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 该类只是一个初始化surfaceview的封装
 *
 * @author cokus
 */
public class WaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    private int line_off = 32;//上下边距距离
    private int startX = 0;

    public int getLine_off() {
        return line_off;
    }


    public void setLine_off(int line_off) {
        this.line_off = line_off;
    }


    public WaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        this.holder = getHolder();
        holder.addCallback(this);

    }


    /**
     * @author cokus
     * init surfaceview
     */
    public void initSurfaceView(final SurfaceView sfv) {
        new Thread() {
            public void run() {
                Canvas canvas = sfv.getHolder().lockCanvas(
                        new Rect(0, 0, sfv.getWidth(), sfv.getHeight()));// 关键:获取画布
                if (canvas == null) {
                    return;
                }
                //canvas.drawColor(Color.rgb(241, 241, 241));// 清除背景
                canvas.drawARGB(255, 239, 239, 239);

                //int height = sfv.getHeight() - line_off;


                Paint circlePaint = new Paint();//指针画笔
                circlePaint.setColor(Color.rgb(246, 131, 126));
                circlePaint.setAntiAlias(true);
                canvas.drawCircle(startX, line_off / 4, line_off / 4, circlePaint);// 上面小圆
                canvas.drawCircle(startX, sfv.getHeight() - line_off / 4, line_off / 4, circlePaint);// 下面小圆
                canvas.drawLine(startX, 0, startX, sfv.getHeight(), circlePaint);//垂直的线

                Paint paintLine = new Paint();//上下界限画笔
                paintLine.setColor(Color.rgb(169, 169, 169));
                canvas.drawLine(startX, line_off / 2, sfv.getWidth(), line_off / 2, paintLine);//最上面的那根线
                canvas.drawLine(startX, sfv.getHeight() - line_off / 2 - 1, sfv.getWidth(), sfv.getHeight() - line_off / 2 - 1, paintLine);//最下面的那根线

                Paint centerLine = new Paint();//中线画笔
                centerLine.setColor(Color.rgb(39, 199, 175));
                canvas.drawLine(startX, sfv.getHeight() / 2, sfv.getWidth(), sfv.getHeight() / 2, centerLine);//中心线
                sfv.getHolder().unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像
            }

            ;
        }.start();

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        initSurfaceView(this);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


}
