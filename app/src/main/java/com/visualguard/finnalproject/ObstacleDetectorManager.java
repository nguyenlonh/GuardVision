package com.visualguard.finnalproject;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ObstacleDetectorManager {
    private static final String TAG = "ObstacleDetectorManager";

    // Optimized thresholds for indoor small objects
    public static final float CONFIDENCE_THRESHOLD = 0.35f; // Lower threshold for small objects
    public static final long SPEECH_COOLDOWN_MS = 1500; // Reduced cooldown for responsive feedback
    public static final float MIN_OBJECT_SIZE = 0.02f; // Minimum relative size for small objects

    private Set<String> importantObjects;
    private Set<String> highPriorityObjects;
    private Set<String> mediumPriorityObjects;
    private Set<String> smallObjects;

    private long lastSpokenTime = 0;
    private String lastSpokenObject = "";
    private int consecutiveSameObjectCount = 0;
    private static final int MAX_CONSECUTIVE_SAME_OBJECT = 2;

    public ObstacleDetectorManager(Context context) {
        initializePriorityObjects();
        Log.d(TAG, "Initialized with " + importantObjects.size() + " indoor objects");
    }

    private void initializePriorityObjects() {
        // High priority objects - safety critical or important items
        highPriorityObjects = new HashSet<>(Arrays.asList(
                "person", "stairs", "knife", "scissors", "glass",
                "oven", "microwave", "toaster", "fire", "hot surface"
        ));

        // Medium priority objects - common household items
        mediumPriorityObjects = new HashSet<>(Arrays.asList(
                "chair", "dining table", "bed", "couch", "toilet", "refrigerator",
                "sink", "door", "window", "cabinet", "bookshelf"
        ));

        // Small objects - items that are typically small and might be missed
        smallObjects = new HashSet<>(Arrays.asList(
                "bottle", "cup", "bowl", "fork", "knife", "spoon",
                "banana", "apple", "orange", "broccoli", "carrot",
                "donut", "cake", "cell phone", "remote", "keyboard", "mouse",
                "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush",
                "umbrella", "handbag", "tie", "suitcase", "backpack"
        ));

        // All important objects combined
        importantObjects = new HashSet<>();
        importantObjects.addAll(highPriorityObjects);
        importantObjects.addAll(mediumPriorityObjects);
        importantObjects.addAll(smallObjects);

        // Additional common household objects from your label list
        importantObjects.addAll(Arrays.asList(
                "tv", "laptop", "potted plant", "wine glass", "sandwich", "pizza",
                "traffic light", "stop sign", "parking meter", "bench", "bird", "cat", "dog"
        ));
    }

    /**
     * Analyze detection and determine if it should be announced
     */
    public DetectionResult analyzeDetection(String objectName, float confidence,
                                            float centerX, float centerY,
                                            int imageWidth, int imageHeight,
                                            long currentTime) {

        if (objectName == null || objectName.isEmpty()) {
            return null;
        }

        String normalizedObjectName = objectName.toLowerCase();

        // 1. Apply confidence threshold (lower for small objects)
        if (confidence < CONFIDENCE_THRESHOLD) {
            return null;
        }

        // 2. Only announce important indoor objects
        if (!isImportantObject(normalizedObjectName)) {
            return null;
        }

        // 3. Apply speech cooldown to avoid overwhelming the user
        if (currentTime - lastSpokenTime < SPEECH_COOLDOWN_MS) {
            return null;
        }

        // 4. Handle consecutive same object detection
        if (normalizedObjectName.equals(lastSpokenObject)) {
            consecutiveSameObjectCount++;
            if (consecutiveSameObjectCount < MAX_CONSECUTIVE_SAME_OBJECT) {
                return null;
            }
        } else {
            consecutiveSameObjectCount = 1; // Reset counter for new object
        }

        // 5. Calculate detailed direction and distance
        String direction = getDetailedDirection(centerX, centerY, imageWidth, imageHeight);
        float distance = estimateDistance(confidence, centerX, centerY, imageWidth, imageHeight);
        int priority = calculatePriority(normalizedObjectName, confidence, distance);

        // 6. Generate appropriate speech message
        String message = generateIndoorMessage(normalizedObjectName, direction, distance, confidence);

        Log.d(TAG, String.format("Object: %s (conf: %.2f, dir: %s, dist: %.1f, priority: %d)",
                normalizedObjectName, confidence, direction, distance, priority));

        return new DetectionResult(normalizedObjectName, direction, message,
                currentTime, confidence, priority);
    }

    /**
     * Calculate detailed direction including both horizontal and vertical position
     */
    private String getDetailedDirection(float centerX, float centerY, int width, int height) {
        float normalizedX = centerX / width;
        float normalizedY = centerY / height;

        String horizontalDir;
        String verticalDir;

        // Horizontal direction (more precise zones)
        if (normalizedX < 0.25f) {
            horizontalDir = "far left";
        } else if (normalizedX < 0.4f) {
            horizontalDir = "left";
        } else if (normalizedX > 0.75f) {
            horizontalDir = "far right";
        } else if (normalizedX > 0.6f) {
            horizontalDir = "right";
        } else {
            horizontalDir = "ahead";
        }

        // Vertical direction (indicates if object is high or low)
        if (normalizedY < 0.3f) {
            verticalDir = " above";
        } else if (normalizedY > 0.7f) {
            verticalDir = " below";
        } else {
            verticalDir = "";
        }

        return horizontalDir + verticalDir;
    }

    /**
     * Estimate relative distance based on confidence and position
     */
    private float estimateDistance(float confidence, float centerX, float centerY, int width, int height) {
        // Objects in center with high confidence are likely closer
        float centerDistanceX = Math.abs(centerX - width/2f) / (width/2f);
        float centerDistanceY = Math.abs(centerY - height/2f) / (height/2f);
        float centerDistance = (centerDistanceX + centerDistanceY) / 2f;

        // Higher confidence + center position = closer object
        float estimatedDistance = (1.0f - confidence) * 0.7f + centerDistance * 0.3f;

        return Math.min(estimatedDistance, 1.0f);
    }

    /**
     * Calculate priority score for object detection
     */
    private int calculatePriority(String objectName, float confidence, float distance) {
        int priority = 0;

        // Priority based on object type
        if (highPriorityObjects.contains(objectName)) {
            priority += 40; // Highest priority for safety-critical objects
        } else if (mediumPriorityObjects.contains(objectName)) {
            priority += 25;
        } else if (smallObjects.contains(objectName)) {
            priority += 20; // Small objects get decent priority
        } else {
            priority += 15; // Other objects
        }

        // Priority based on confidence
        priority += (int)(confidence * 25);

        // Priority based on distance (closer objects = higher priority)
        priority += (int)((1.0f - distance) * 20);

        return priority;
    }

    /**
     * Generate natural English speech message for indoor objects
     */
    private String generateIndoorMessage(String objectName, String direction, float distance, float confidence) {
        StringBuilder message = new StringBuilder();

        // Add confidence level indication
        if (confidence > 0.75f) {
            message.append("Clear ");
        } else if (confidence > 0.5f) {
            // No prefix for medium confidence
        } else {
            message.append("Possible ");
        }

        // Add object name (use natural English names)
        String naturalName = getNaturalObjectName(objectName);
        message.append(naturalName).append(" ").append(direction);

        // Add distance information
        if (distance < 0.25f) {
            message.append(", very close");
        } else if (distance < 0.5f) {
            message.append(", close by");
        } else if (distance < 0.75f) {
            message.append(", nearby");
        } else {
            message.append(", in the distance");
        }

        // Add urgency for high-priority objects
        if (highPriorityObjects.contains(objectName) && distance < 0.4f) {
            message.append(", be careful");
        }

        return message.toString();
    }

    /**
     * Convert object names to more natural English terms
     */
    private String getNaturalObjectName(String objectName) {
        switch (objectName.toLowerCase()) {
            case "cell phone": return "phone";
            case "dining table": return "table";
            case "potted plant": return "plant";
            case "wine glass": return "glass";
            case "hot dog": return "food";
            case "sports ball": return "ball";
            case "baseball bat": return "bat";
            case "tennis racket": return "racket";
            case "hair drier": return "hairdryer";
            case "teddy bear": return "teddy bear";
            case "toothbrush": return "toothbrush";
            case "remote": return "remote control";
            case "mouse": return "computer mouse";
            default: return objectName;
        }
    }

    /**
     * Check if object is important for indoor detection
     */
    public boolean isImportantObject(String objectName) {
        return importantObjects.contains(objectName.toLowerCase());
    }

    /**
     * Update last spoken time to manage cooldown
     */
    public void updateLastSpokenTime(long time) {
        this.lastSpokenTime = time;
    }

    /**
     * Update last spoken object for consecutive detection tracking
     */
    public void updateLastSpokenObject(String objectName) {
        this.lastSpokenObject = objectName != null ? objectName.toLowerCase() : "";
    }

    /**
     * Get object priority category for external use
     */
    public String getObjectPriorityCategory(String objectName) {
        String name = objectName.toLowerCase();
        if (highPriorityObjects.contains(name)) {
            return "HIGH";
        } else if (mediumPriorityObjects.contains(name)) {
            return "MEDIUM";
        } else if (smallObjects.contains(name)) {
            return "SMALL";
        } else {
            return "LOW";
        }
    }

    /**
     * Reset detection state (useful when changing environments)
     */
    public void resetDetectionState() {
        lastSpokenObject = "";
        consecutiveSameObjectCount = 0;
        lastSpokenTime = System.currentTimeMillis() - SPEECH_COOLDOWN_MS; // Reset cooldown
        Log.d(TAG, "Detection state reset");
    }

    /**
     * Data class for detection results
     */
    public static class DetectionResult {
        public final String objectName;
        public final String direction;
        public final String spokenMessage;
        public final long detectionTime;
        public final float confidence;
        public final int priority;

        public DetectionResult(String objectName, String direction,
                               String spokenMessage, long detectionTime,
                               float confidence, int priority) {
            this.objectName = objectName;
            this.direction = direction;
            this.spokenMessage = spokenMessage;
            this.detectionTime = detectionTime;
            this.confidence = confidence;
            this.priority = priority;
        }

        @Override
        public String toString() {
            return String.format("DetectionResult{object='%s', dir='%s', priority=%d, conf=%.2f}",
                    objectName, direction, priority, confidence);
        }
    }

    /**
     * Clean up resources
     */
    public void release() {
        if (importantObjects != null) {
            importantObjects.clear();
        }
        if (highPriorityObjects != null) {
            highPriorityObjects.clear();
        }
        if (mediumPriorityObjects != null) {
            mediumPriorityObjects.clear();
        }
        if (smallObjects != null) {
            smallObjects.clear();
        }
        lastSpokenObject = "";
        consecutiveSameObjectCount = 0;
        Log.d(TAG, "ObstacleDetectorManager released");
    }

    /**
     * Debug method to list all tracked objects
     */
    public void listTrackedObjects() {
        Log.d(TAG, "=== Tracked Objects ===");
        Log.d(TAG, "High Priority: " + highPriorityObjects.size() + " objects");
        Log.d(TAG, "Medium Priority: " + mediumPriorityObjects.size() + " objects");
        Log.d(TAG, "Small Objects: " + smallObjects.size() + " objects");
        Log.d(TAG, "Total Important: " + importantObjects.size() + " objects");
    }
}