package com.visualguard.finnalproject.ObjectDetect;

import android.graphics.RectF;

/**
 * Simple class to hold scaled detection data without using reflection
 */
public class ScaledDetection {
    private final String label;
    private final float score;
    private final RectF boundingBox;

    public ScaledDetection(String label, float score, RectF boundingBox) {
        this.label = label;
        this.score = score;
        this.boundingBox = boundingBox;
    }

    public String getLabel() {
        return label;
    }

    public float getScore() {
        return score;
    }

    public RectF getBoundingBox() {
        return boundingBox;
    }
}