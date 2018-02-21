package com.zemosolabs.mindhive.videomanipulation.custom_views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.io.Serializable;

/**
 * Created by adv on 20.10.2016.
 * Used to make a view render in a circle.
 * Supports gestures for dragging and scaling.
 * To get layout updates set a listener using setLayoutUpdatesListener().
 */

public class CircularFrameLayout extends FrameLayout implements Serializable {
    private static final long serialVersionUID = 6128016096756071380L;

    private static final String TAG = CircularFrameLayout.class.getSimpleName();

    private static final int DIFF = 10;
    private static final int STROKE_WIDTH = (int) (DIFF * 0.5f);
    private static final int ALPHA = 255;

    private Paint paint;
    private boolean isCircular;
    private Path clippingPath;

    private GestureDetector doubleTapListener;

    //region Constructors

    public CircularFrameLayout(Context context) {
        super(context);
        initializeView(context);
    }

    public CircularFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView(context);
    }

    public CircularFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView(context);
    }

    private void initializeView(Context context){
        Log.d(TAG, "CircularView() called with: context = [" + context + "]");
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(ALPHA);
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        isCircular = true;
    }

    //endregion

    //region FrameLayout Inherited methods

    @Override
    protected void onDraw(Canvas canvas) {
        // Create a circular path.
        if(clippingPath != null){
            if(isCircular){
                canvas.drawCircle(getWidth()/2, getHeight()/2, getWidth()/2-STROKE_WIDTH, paint);
            }else{
                canvas.drawRect(STROKE_WIDTH, STROKE_WIDTH, getWidth()-STROKE_WIDTH, getHeight()-STROKE_WIDTH, paint);
            }
        }
        super.onDraw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int count = canvas.save();
        if (clippingPath != null) {
            canvas.clipPath(clippingPath);
        }
        super.dispatchDraw(canvas);
        canvas.restoreToCount(count);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        Log.d(TAG, "onSizeChanged() called with: w = [" + w + "], h = [" + h + "], oldWidth = [" + oldWidth + "], oldHeight = [" + oldHeight + "]");
        if (w != oldWidth || h != oldHeight) {
            updateClippingPath(w, h);
        }
    }

    //endregion

    //region helper methods

    private synchronized void updateClippingPath(int w, int h) {
        Log.d(TAG, "updateClippingPath() called with: w = [" + w + "], h = [" + h + "]");
        final int radius = Math.min(w, h) / 2;
        clippingPath = new Path();
        if(isCircular) {
            clippingPath.addCircle(radius, radius, radius - STROKE_WIDTH, Path.Direction.CW);
        }else{
            clippingPath.addRect(STROKE_WIDTH, STROKE_WIDTH, w-STROKE_WIDTH, h-STROKE_WIDTH, Path.Direction.CW);
        }
    }

    //endregion

    //region View Controller Methods

    public void setMode(boolean isCircular) {
        this.isCircular = isCircular;
        updateClippingPath(getWidth(), getHeight());
        invalidate();
    }

    public void switchWindowMode(){
        setMode(!isCircular);
    }

    public void setDoubleTapListener(GestureDetector doubleTapListener){
        this.doubleTapListener = doubleTapListener;
    }

    //endregion

    //region gestures

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean doubleTapVal = false;
        if(doubleTapListener != null){
            doubleTapVal = doubleTapListener.onTouchEvent(event);
        }
        return doubleTapVal;
    }

    //endregion
}