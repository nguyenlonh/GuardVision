package com.visualguard.finnalproject.ObjectDetect;

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
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.visualguard.finnalproject.R;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObstacleDetectionActivity extends AppCompatActivity {
    private static final String TAG = "ObstacleDetection";
    private static final int REQ_CAMERA = 101;

    private TextureView textureView;
    private TextView statusText;
    private BoundingBoxOverlayView boundingBoxOverlay;

    private CameraDevice cameraDevice;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraManager cameraManager;

    private ObjectDetector objectDetector;
    private TextToSpeech tts;
    private ObstacleDetectorManager obstacleManager;
    private ImageProcessor imageProcessor;

    // Model configuration
    private static final int MODEL_INPUT_SIZE = 480;
    private long lastProcessingTime = 0;
    private static final long PROCESSING_INTERVAL = 150;

    // Thread-safe flags
    private AtomicBoolean isDetectionActive = new AtomicBoolean(false);
    private AtomicBoolean isProcessingFrame = new AtomicBoolean(false);

    // Reusable TensorImage to reduce allocations
    private TensorImage reusableTensorImage;

    // Scale factors
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

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    private void initializeComponents() {
        // Start background thread first
        startBackgroundThread();

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(0.9f);
                speak("Object detection started. I will announce objects around you.");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });

        obstacleManager = new ObstacleDetectorManager(this);

        // Initialize image processor
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        // Initialize reusable TensorImage
        reusableTensorImage = new TensorImage();

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

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        isDetectionActive.set(true);
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
            updateScaleFactors(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            long currentTime = System.currentTimeMillis();
            if (isDetectionActive.get() &&
                    !isProcessingFrame.get() &&
                    currentTime - lastProcessingTime > PROCESSING_INTERVAL) {

                lastProcessingTime = currentTime;
                // Process frame on background thread
                if (backgroundHandler != null) {
                    backgroundHandler.post(() -> processFrame());
                }
            }
        }
    };

    private void updateScaleFactors(int viewWidth, int viewHeight) {
        scaleFactorX = (float) viewWidth / MODEL_INPUT_SIZE;
        scaleFactorY = (float) viewHeight / MODEL_INPUT_SIZE;
        Log.d(TAG, String.format("Scale factors - X: %.2f, Y: %.2f", scaleFactorX, scaleFactorY));
    }

    private void processFrame() {
        // Prevent concurrent processing
        if (!isProcessingFrame.compareAndSet(false, true)) {
            return;
        }

        Bitmap bitmap = null;
        try {
            // Get bitmap from texture view (must be on UI thread context but getBitmap is safe)
            bitmap = textureView.getBitmap();
            if (bitmap == null || !isDetectionActive.get()) {
                return;
            }

            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();

            // Load bitmap into reusable TensorImage
            reusableTensorImage.load(bitmap);
            TensorImage processedImage = imageProcessor.process(reusableTensorImage);

            // Run object detection
            List<Detection> detections = objectDetector.detect(processedImage);

            if (detections != null && !detections.isEmpty()) {
                // Create scaled detections list (don't modify originals)
                List<Detection> scaledDetections = createScaledDetections(detections, originalWidth, originalHeight);

                // Update UI on main thread
                final List<Detection> finalDetections = scaledDetections;
                runOnUiThread(() -> {
                    if (boundingBoxOverlay != null) {
                        boundingBoxOverlay.setDetections(finalDetections);
                    }
                });

                // Process for speech (use original model coordinates)
                processDetectionsForSpeech(detections, originalWidth, originalHeight);
            } else {
                // Clear overlay if no detections
                runOnUiThread(() -> {
                    if (boundingBoxOverlay != null) {
                        boundingBoxOverlay.setDetections(null);
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        } finally {
            // Always recycle bitmap to prevent memory leak
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            isProcessingFrame.set(false);
        }
    }

    private List<Detection> createScaledDetections(List<Detection> detections, int bitmapWidth, int bitmapHeight) {
        // Scale from model size (480x480) to bitmap size
        float scaleX = (float) bitmapWidth / MODEL_INPUT_SIZE;
        float scaleY = (float) bitmapHeight / MODEL_INPUT_SIZE;

        // Create new list with scaled bounding boxes
        List<ScaledDetection> scaledList = new ArrayList<>();

        for (Detection detection : detections) {
            if (detection.getCategories() != null && !detection.getCategories().isEmpty()) {
                RectF originalBox = detection.getBoundingBox();
                RectF scaledBox = new RectF(
                        originalBox.left * scaleX,
                        originalBox.top * scaleY,
                        originalBox.right * scaleX,
                        originalBox.bottom * scaleY
                );

                scaledList.add(new ScaledDetection(
                        detection.getCategories().get(0).getLabel(),
                        detection.getCategories().get(0).getScore(),
                        scaledBox
                ));
            }
        }

        // Return as Detection list for compatibility with existing overlay
        // Note: This requires updating BoundingBoxOverlayView to use ScaledDetection
        return (List<Detection>)(List<?>) scaledList;
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

                ObstacleDetectorManager.DetectionResult result =
                        obstacleManager.analyzeDetection(objectName, confidence,
                                boundingBox.centerX(), boundingBox.centerY(),
                                MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, currentTime);

                if (result != null && result.priority > maxPriority) {
                    maxPriority = result.priority;
                    bestResult = result;
                }
            }
        }

        if (bestResult != null) {
            final ObstacleDetectorManager.DetectionResult finalResult = bestResult;
            Log.d(TAG, "Voice announcement: " + finalResult.spokenMessage);

            runOnUiThread(() -> {
                updateStatus("Detected: " + finalResult.objectName);
                speak(finalResult.spokenMessage);
            });

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
                    camera.close();
                    cameraDevice = null;
                    updateStatus("Camera disconnected");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
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
                initializeComponents();
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
                    initializeComponents();
                    openCamera();
                }
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
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
        isDetectionActive.set(false);
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        isDetectionActive.set(true);
        if (tts != null) {
            speak("Resuming object detection");
        }
    }

    @Override
    protected void onDestroy() {
        isDetectionActive.set(false);

        // Wait for any pending frame processing
        while (isProcessingFrame.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        stopBackgroundThread();

        if (objectDetector != null) {
            objectDetector.close();
            objectDetector = null;
        }

        if (obstacleManager != null) {
            obstacleManager.release();
            obstacleManager = null;
        }

        super.onDestroy();
        Log.d(TAG, "ObstacleDetectionActivity destroyed");
    }
}