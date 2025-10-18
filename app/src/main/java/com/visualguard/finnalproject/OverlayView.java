package com.visualguard.finnalproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private List<RectF> rects = new ArrayList<>();
    private final Paint rectPaint;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(6f);
    }

    public void setRects(List<RectF> r) {
        if (r == null) {
            this.rects = new ArrayList<>();
        } else {
            this.rects = r;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rects == null || rects.isEmpty()) return;
        for (RectF rf : rects) {
            if (rf != null) {
                canvas.drawRect(rf, rectPaint);
            }
        }
    }
}

