package com.visualguard.finnalproject;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ObstacleDetectorManager {
    private static final String TAG = "ObstacleDetectorManager";

    // GIẢM COOLDOWN để phản hồi liên tục hơn
    public static final float CONFIDENCE_THRESHOLD = 0.4f;
    public static final long SPEECH_COOLDOWN_MS = 2000; // Giảm từ 2000ms xuống 800ms

    private Set<String> importantObjects;
    private long lastSpokenTime = 0;
    private String lastSpokenObject = "";
    private int consecutiveSameObjectCount = 0;
    private static final int MAX_CONSECUTIVE_SAME_OBJECT = 3;

    public ObstacleDetectorManager(Context context) {
        initializeImportantObjects();
        Log.d(TAG, "Initialized with " + importantObjects.size() + " important objects");
    }

    private void initializeImportantObjects() {
        // Mở rộng danh sách vật thể quan trọng
        importantObjects = new HashSet<>(Arrays.asList(
                "person", "chair", "dining table", "bed", "couch", "toilet",
                "refrigerator", "oven", "sink", "stairs", "door",
                "bicycle", "car", "motorcycle", "bus", "truck", "train",
                "traffic light", "stop sign", "parking meter",
                "dog", "cat", "bird", "horse", "cow",
                "bench", "potted plant", "fire hydrant",
                "bookshelf", "cabinet", "table",
                "cell phone", "tv", "laptop"
        ));
    }

    /**
     * Phân tích detection - CHO PHÉP NHIỀU VẬT THỂ LIÊN TIẾP
     */
    public DetectionResult analyzeDetection(String objectName, float confidence,
                                            float centerX, int imageWidth,
                                            long currentTime) {

        if (objectName == null || objectName.isEmpty()) {
            return null;
        }

        String normalizedObjectName = objectName.toLowerCase();

        // 1. Confidence threshold thấp hơn để bắt nhiều vật thể hơn
        if (confidence < CONFIDENCE_THRESHOLD) {
            return null;
        }

        // 2. Chỉ thông báo vật thể quan trọng
        if (!isImportantObject(normalizedObjectName)) {
            return null;
        }

        // 3. Cooldown ngắn hơn để phản hồi nhanh
        if (currentTime - lastSpokenTime < SPEECH_COOLDOWN_MS) {
            return null;
        }

        // 4. Logic mới: Cho phép nói cùng object sau 3 lần phát hiện liên tiếp
        if (normalizedObjectName.equals(lastSpokenObject)) {
            consecutiveSameObjectCount++;
            if (consecutiveSameObjectCount < MAX_CONSECUTIVE_SAME_OBJECT) {
                return null;
            }
        } else {
            consecutiveSameObjectCount = 1; // Reset counter for new object
        }

        // 5. Tính hướng và tạo message
        String direction = getPreciseDirection(centerX, imageWidth);
        String message = generateMessage(normalizedObjectName, direction, confidence);

        Log.d(TAG, String.format("Detection: %s (conf: %.2f, dir: %s, count: %d)",
                normalizedObjectName, confidence, direction, consecutiveSameObjectCount));

        return new DetectionResult(normalizedObjectName, direction, message, currentTime, confidence);
    }

    public boolean isImportantObject(String objectName) {
        return importantObjects.contains(objectName.toLowerCase());
    }

    /**
     * Tính hướng chi tiết hơn
     */
    private String getPreciseDirection(float centerX, int imageWidth) {
        float normalizedX = centerX / imageWidth;

        if (normalizedX < 0.2f) {
            return "far left";
        } else if (normalizedX < 0.4f) {
            return "left";
        } else if (normalizedX > 0.8f) {
            return "far right";
        } else if (normalizedX > 0.6f) {
            return "right";
        } else {
            return "ahead";
        }
    }

    private String generateMessage(String objectName, String direction, float confidence) {
        StringBuilder message = new StringBuilder();

        // Thêm mức độ confidence vào message
        if (confidence > 0.8f) {
            message.append("Clear ");
        } else if (confidence > 0.6f) {
            message.append("");
        } else {
            message.append("Possible ");
        }

        message.append(objectName).append(" ").append(direction);

        // Thêm thông tin khoảng cách dựa trên confidence
        if (confidence > 0.8f) {
            message.append(", very close");
        } else if (confidence > 0.6f) {
            message.append(", close");
        } else {
            message.append(", in distance");
        }

        return message.toString();
    }

    public void updateLastSpokenTime(long time) {
        this.lastSpokenTime = time;
    }

    public void updateLastSpokenObject(String objectName) {
        this.lastSpokenObject = objectName != null ? objectName.toLowerCase() : "";
    }

    public static class DetectionResult {
        public final String objectName;
        public final String direction;
        public final String spokenMessage;
        public final long detectionTime;
        public final float confidence;

        public DetectionResult(String objectName, String direction,
                               String spokenMessage, long detectionTime, float confidence) {
            this.objectName = objectName;
            this.direction = direction;
            this.spokenMessage = spokenMessage;
            this.detectionTime = detectionTime;
            this.confidence = confidence;
        }
    }

    public void release() {
        if (importantObjects != null) {
            importantObjects.clear();
        }
        lastSpokenObject = "";
        consecutiveSameObjectCount = 0;
        Log.d(TAG, "ObstacleDetectorManager released");
    }
}