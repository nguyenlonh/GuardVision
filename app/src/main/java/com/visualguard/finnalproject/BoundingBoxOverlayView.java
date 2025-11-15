package com.visualguard.finnalproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.List;

public class BoundingBoxOverlayView extends View {
    private List<Detection> detections;
    private Paint boxPaint, textPaint;
    private float scaleFactor = 1.0f;

    // Color palette for different objects
    private int[] colors = {
            Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA,
            Color.YELLOW, Color.WHITE, Color.rgb(255, 165, 0)
    };

    public BoundingBoxOverlayView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Box paint
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);

        // Text paint
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void setDetections(List<Detection> detections) {
        this.detections = detections;
        invalidate(); // Redraw view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detections == null || detections.isEmpty()) {
            return;
        }

        for (int i = 0; i < detections.size(); i++) {
            Detection detection = detections.get(i);
            if (detection.getCategories() != null && !detection.getCategories().isEmpty()) {
                String label = detection.getCategories().get(0).getLabel();
                float confidence = detection.getCategories().get(0).getScore();
                RectF boundingBox = detection.getBoundingBox();

                if (confidence > 0.3f) {
                    // Draw bounding box
                    boxPaint.setColor(colors[i % colors.length]);
                    canvas.drawRect(boundingBox, boxPaint);

                    // Draw label
                    String text = String.format("%s %.1f", label, confidence);
                    canvas.drawText(text, boundingBox.left, boundingBox.top - 10, textPaint);
                }
            }
        }
    }
}