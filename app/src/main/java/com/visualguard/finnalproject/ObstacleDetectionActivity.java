package com.visualguard.finnalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObstacleDetectionActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 2004;
    private PreviewView previewView;
    private TextToSpeech tts;
    private ExecutorService cameraExecutor;

    private ObjectDetector objectDetector;
    private final Map<String, Long> lastSpokenTime = new HashMap<>();
    private static final long COOLDOWN_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_detection);

        previewView = findViewById(R.id.previewView);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                speak("Obstacle detection started");
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Load model
        loadModel();

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    private void loadModel() {
        try {
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setMaxResults(3)
                    .setScoreThreshold(0.5f)
                    .build();

            objectDetector = ObjectDetector.createFromFileAndOptions(
                    this,
                    "efficientdet-lite1.tflite",
                    options
            );
            Log.d("Obstacle", "Model loaded successfully");
        } catch (IOException e) {
            Log.e("Obstacle", "Failed to load model", e);
            speak("Failed to load detection model");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("Obstacle", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        try {
            // Sử dụng ImageProxy.toBitmap() đơn giản
            android.graphics.Bitmap bitmap = imageProxy.toBitmap();

            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            // Tạo TensorImage từ bitmap
            org.tensorflow.lite.support.image.TensorImage tensorImage =
                    org.tensorflow.lite.support.image.TensorImage.fromBitmap(bitmap);

            // Chạy detection
            List<Detection> detections = objectDetector.detect(tensorImage);
            processDetections(detections, imageProxy.getWidth());

        } catch (Exception e) {
            Log.e("Obstacle", "Detection error", e);
        } finally {
            imageProxy.close();
        }
    }

    private void processDetections(List<Detection> detections, int imageWidth) {
        if (detections == null || detections.isEmpty()) return;

        long currentTime = System.currentTimeMillis();

        for (Detection detection : detections) {
            if (!detection.getCategories().isEmpty()) {
                org.tensorflow.lite.support.label.Category category = detection.getCategories().get(0);

                if (category.getScore() > 0.5f) {
                    String objectName = category.getLabel();
                    RectF boundingBox = detection.getBoundingBox(); // Sửa thành RectF

                    String position = getSimplePosition(boundingBox, imageWidth);

                    String key = objectName + position;
                    Long lastTime = lastSpokenTime.get(key);
                    if (lastTime == null || (currentTime - lastTime) > COOLDOWN_MS) {
                        speak(objectName + " " + position);
                        lastSpokenTime.put(key, currentTime);
                        break;
                    }
                }
            }
        }
    }

    private String getSimplePosition(RectF boundingBox, int imageWidth) {
        float centerX = boundingBox.centerX();
        if (centerX < imageWidth * 0.4f) {
            return "left";
        } else if (centerX > imageWidth * 0.6f) {
            return "right";
        } else {
            return "ahead";
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "obstacle");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }
}