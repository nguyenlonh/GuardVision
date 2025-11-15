package com.visualguard.finnalproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ObstacleDetectionActivity extends AppCompatActivity {
    private static final String TAG = "ObstacleDetection";
    private static final int REQ_CAMERA = 101;

    private TextureView textureView;
    private TextView statusText;
    private BoundingBoxOverlayView boundingBoxOverlay;

    private CameraDevice cameraDevice;
    private Handler backgroundHandler;
    private CameraManager cameraManager;

    private ObjectDetector objectDetector;
    private TextToSpeech tts;
    private ObstacleDetectorManager obstacleManager;
    private ImageProcessor imageProcessor;

    // Model configuration
    private static final int MODEL_INPUT_SIZE = 480;
    private long lastProcessingTime = 0;
    private static final long PROCESSING_INTERVAL = 150; // ms between processing frames
    private boolean isDetectionActive = false;

    // For bounding box scaling
    private float scaleFactorX = 1.0f;
    private float scaleFactorY = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_detection);

        initializeViews();
        getPermission();
    }

    private void initializeViews() {
        textureView = findViewById(R.id.textureView);
        statusText = findViewById(R.id.statusText);
        boundingBoxOverlay = findViewById(R.id.boundingBoxOverlay);

        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private void initializeComponents() {
        // Initialize TTS for English
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(0.9f);
                speak("Indoor object detection started. I will announce objects around you.");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });

        obstacleManager = new ObstacleDetectorManager(this);

        // Initialize image processor
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        // Load TensorFlow Lite model
        try {
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setMaxResults(5)
                    .setScoreThreshold(0.4f)
                    .build();

            objectDetector = ObjectDetector.createFromFileAndOptions(
                    this,
                    "efficientdet-lite1.tflite",
                    options
            );
            Log.d(TAG, "Object detection model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model", e);
            Toast.makeText(this, "Object detection model failed to load", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Start background handler thread
        HandlerThread handlerThread = new HandlerThread("CameraBackground");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        isDetectionActive = true;
        updateStatus("Detection active - scanning for objects");
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            initializeComponents();
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Update scale factors when texture size changes
            updateScaleFactors(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if (isDetectionActive && System.currentTimeMillis() - lastProcessingTime > PROCESSING_INTERVAL) {
                processFrame();
                lastProcessingTime = System.currentTimeMillis();
            }
        }
    };

    private void updateScaleFactors(int viewWidth, int viewHeight) {
        // Calculate scale factors to convert from model coordinates (480x480) to view coordinates
        scaleFactorX = (float) viewWidth / MODEL_INPUT_SIZE;
        scaleFactorY = (float) viewHeight / MODEL_INPUT_SIZE;

        Log.d(TAG, String.format("Scale factors - X: %.2f, Y: %.2f", scaleFactorX, scaleFactorY));
    }

    private void processFrame() {
        Bitmap bitmap = textureView.getBitmap();
        if (bitmap == null) return;

        try {
            // Store original dimensions for scaling
            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();

            // Update scale factors if needed (should be done in texture size changed, but just in case)
            if (scaleFactorX == 1.0f || scaleFactorY == 1.0f) {
                updateScaleFactors(originalWidth, originalHeight);
            }

            // Convert to TensorImage and process
            TensorImage image = TensorImage.fromBitmap(bitmap);
            image = imageProcessor.process(image);

            // Run object detection
            List<Detection> detections = objectDetector.detect(image);

            // Scale bounding boxes to match original image size
            List<Detection> scaledDetections = scaleBoundingBoxes(detections, originalWidth, originalHeight);

            // Update bounding box overlay with scaled detections
            boundingBoxOverlay.setDetections(scaledDetections);

            // Process for voice announcements (use original detections for analysis)
            processDetectionsForSpeech(detections, originalWidth, originalHeight);

        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }
    }

    private List<Detection> scaleBoundingBoxes(List<Detection> detections, int originalWidth, int originalHeight) {
        if (detections == null) return detections;

        for (Detection detection : detections) {
            RectF boundingBox = detection.getBoundingBox();

            // Scale bounding box from model coordinates (480x480) to original image coordinates
            float left = boundingBox.left * scaleFactorX;
            float top = boundingBox.top * scaleFactorY;
            float right = boundingBox.right * scaleFactorX;
            float bottom = boundingBox.bottom * scaleFactorY;

            // Create new scaled bounding box
            RectF scaledBoundingBox = new RectF(left, top, right, bottom);

            // Use reflection to set the scaled bounding box (since Detection class doesn't have setter)
            try {
                java.lang.reflect.Field field = detection.getClass().getDeclaredField("boundingBox");
                field.setAccessible(true);
                field.set(detection, scaledBoundingBox);
            } catch (Exception e) {
                Log.e(TAG, "Failed to scale bounding box", e);
            }
        }

        return detections;
    }

    private void processDetectionsForSpeech(List<Detection> detections, int width, int height) {
        if (detections == null || detections.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        ObstacleDetectorManager.DetectionResult bestResult = null;
        float maxPriority = 0;

        for (Detection detection : detections) {
            if (detection.getCategories() != null && !detection.getCategories().isEmpty()) {
                String objectName = detection.getCategories().get(0).getLabel();
                float confidence = detection.getCategories().get(0).getScore();
                RectF boundingBox = detection.getBoundingBox();

                // Use original coordinates for speech analysis (model coordinates)
                float originalCenterX = boundingBox.centerX() / scaleFactorX;
                float originalCenterY = boundingBox.centerY() / scaleFactorY;

                ObstacleDetectorManager.DetectionResult result =
                        obstacleManager.analyzeDetection(objectName, confidence,
                                originalCenterX, originalCenterY,
                                MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, currentTime);

                if (result != null && result.priority > maxPriority) {
                    maxPriority = result.priority;
                    bestResult = result;
                }
            }
        }

        if (bestResult != null) {
            Log.d(TAG, "Voice announcement: " + bestResult.spokenMessage);
            updateStatus("Detected: " + bestResult.objectName);
            speak(bestResult.spokenMessage);
            obstacleManager.updateLastSpokenTime(currentTime);
            obstacleManager.updateLastSpokenObject(bestResult.objectName);
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                    updateStatus("Camera disconnected");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    updateStatus("Camera error: " + error);
                    speak("Camera error occurred");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error", e);
            updateStatus("Camera access denied");
            speak("Cannot access camera");
        }
    }

    private void startCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;

            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);

            CaptureRequest.Builder captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(),
                                        null, backgroundHandler);
                                updateStatus("Real-time detection active");
                                speak("Camera ready. Object detection is now active.");
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Failed to start camera preview", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            updateStatus("Camera configuration failed");
                            speak("Camera setup failed");
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting camera preview", e);
        }
    }

    private void updateStatus(final String message) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(message);
            }
        });
    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            if (textureView.getSurfaceTexture() != null) {
                openCamera();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (textureView.getSurfaceTexture() != null) {
                    openCamera();
                }
            } else {
                Toast.makeText(this, "Camera permission required for object detection",
                        Toast.LENGTH_LONG).show();
                updateStatus("Camera permission denied");
                speak("Camera permission is required");
                finish();
            }
        }
    }

    private void speak(String text) {
        if (tts != null && !tts.isSpeaking()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "obstacle_detection");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isDetectionActive = false;
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDetectionActive = true;
        if (tts != null) {
            speak("Resuming object detection");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDetectionActive = false;

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (cameraDevice != null) {
            cameraDevice.close();
        }

        if (backgroundHandler != null) {
            backgroundHandler.getLooper().quitSafely();
        }

        if (objectDetector != null) {
            objectDetector.close();
        }

        if (obstacleManager != null) {
            obstacleManager.release();
        }

        Log.d(TAG, "ObstacleDetectionActivity destroyed");
    }
}