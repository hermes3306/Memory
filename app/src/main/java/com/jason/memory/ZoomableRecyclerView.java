package com.jason.memory;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.recyclerview.widget.RecyclerView;

public class ZoomableRecyclerView extends RecyclerView {
    private static final String TAG = "ZoomableRecyclerView";
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 3.0f;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;
    private float mLastFocusX;
    private float mLastFocusY;

    public ZoomableRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "--m-- init: Initializing ZoomableRecyclerView");
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "--m-- onInterceptTouchEvent: " + ev.getAction());
        mScaleDetector.onTouchEvent(ev);
        return super.onInterceptTouchEvent(ev) || mScaleDetector.isInProgress();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d(TAG, "--m-- onTouchEvent: " + ev.getAction());
        mScaleDetector.onTouchEvent(ev);

        if (mScaleDetector.isInProgress()) {
            Log.d(TAG, "--m-- onTouchEvent: Scale in progress");
            return true;
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.d(TAG, "--m-- onDraw: Scale factor = " + mScaleFactor);
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor, mLastFocusX, mLastFocusY);
        super.onDraw(canvas);
        canvas.restore();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "--m-- onScaleBegin");
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d(TAG, "--m-- onScale: factor = " + detector.getScaleFactor());
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(MIN_SCALE, Math.min(mScaleFactor, MAX_SCALE));
            mLastFocusX = detector.getFocusX();
            mLastFocusY = detector.getFocusY();
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "--m-- onScaleEnd");
        }
    }
}