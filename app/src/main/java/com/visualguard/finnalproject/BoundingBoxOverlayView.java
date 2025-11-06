package com.visualguard.finnalproject;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlayView extends View {
    private List<Detection> detections = new ArrayList<>();
    private Paint boxPaint = new Paint();
    private Paint textPaint = new Paint();

    // Simple colors array
    private int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN};

    public BoundingBoxOverlayView(Context context) {
        super(context);
        setupPaints();
    }

    public BoundingBoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    private void setupPaints() {
        // Box paint - simple setup
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);

        // Text paint - simple setup
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setFakeBoldText(true);
    }

    public void setDetections(List<Detection> detections) {
        this.detections = detections != null ? new ArrayList<>(detections) : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detections.isEmpty()) return;

        for (int i = 0; i < detections.size(); i++) {
            Detection detection = detections.get(i);
            if (detection.getCategories().isEmpty()) continue;

            drawSimpleDetection(canvas, detection, i);
        }
    }

    private void drawSimpleDetection(Canvas canvas, Detection detection, int index) {
        // Get detection info
        String label = detection.getCategories().get(0).getLabel();
        float confidence = detection.getCategories().get(0).getScore();
        RectF box = detection.getBoundingBox();

        // Only draw high confidence detections
        if (confidence < 0.4f) return;

        // Set random color for this detection
        boxPaint.setColor(colors[index % colors.length]);

        // Draw bounding box - DIRECT coordinates (no complex scaling)
        canvas.drawRect(box, boxPaint);

        // Draw simple label with confidence
        String text = String.format("%s %.1f", label, confidence);
        canvas.drawText(text, box.left, box.top - 10, textPaint);
    }

    public void clear() {
        detections.clear();
        invalidate();
    }
}