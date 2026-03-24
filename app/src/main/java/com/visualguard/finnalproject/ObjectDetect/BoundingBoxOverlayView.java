package com.visualguard.finnalproject.ObjectDetect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlayView extends View {
    private List<DrawableDetection> drawableDetections;
    private Paint boxPaint, textPaint, textBackgroundPaint;

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
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);

        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setAntiAlias(true);

        drawableDetections = new ArrayList<>();
    }

    public void setDetections(List<?> detections) {
        drawableDetections.clear();

        if (detections != null) {
            for (Object obj : detections) {
                if (obj instanceof ScaledDetection) {
                    ScaledDetection sd = (ScaledDetection) obj;
                    drawableDetections.add(new DrawableDetection(
                            sd.getLabel(),
                            sd.getScore(),
                            sd.getBoundingBox()
                    ));
                } else if (obj instanceof Detection) {
                    Detection d = (Detection) obj;
                    if (d.getCategories() != null && !d.getCategories().isEmpty()) {
                        drawableDetections.add(new DrawableDetection(
                                d.getCategories().get(0).getLabel(),
                                d.getCategories().get(0).getScore(),
                                d.getBoundingBox()
                        ));
                    }
                }
            }
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawableDetections == null || drawableDetections.isEmpty()) {
            return;
        }

        for (int i = 0; i < drawableDetections.size(); i++) {
            DrawableDetection detection = drawableDetections.get(i);

            if (detection.confidence > 0.3f) {
                int color = colors[i % colors.length];
                RectF box = detection.boundingBox;

                // Draw bounding box
                boxPaint.setColor(color);
                canvas.drawRect(box, boxPaint);

                // Draw label background
                String text = String.format("%s %.0f%%", detection.label, detection.confidence * 100);
                float textWidth = textPaint.measureText(text);
                float textHeight = textPaint.getTextSize();

                textBackgroundPaint.setColor(Color.argb(180, 0, 0, 0));
                canvas.drawRect(
                        box.left,
                        Math.max(box.top - textHeight - 8, 0),
                        box.left + textWidth + 16,
                        Math.max(box.top, textHeight + 8),
                        textBackgroundPaint
                );

                // Draw label text
                canvas.drawText(text, box.left + 8, Math.max(box.top - 8, textHeight), textPaint);
            }
        }
    }

    // Internal class to store drawable detection data
    private static class DrawableDetection {
        final String label;
        final float confidence;
        final RectF boundingBox;

        DrawableDetection(String label, float confidence, RectF boundingBox) {
            this.label = label;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
        }
    }
}