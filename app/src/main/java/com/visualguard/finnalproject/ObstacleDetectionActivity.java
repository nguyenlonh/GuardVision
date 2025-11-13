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
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
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
    private BoundingBoxOverlayView boundingBoxOverlay;
    private List<String> labels;
    private int[] colors = {
            Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY,
            Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    };
    private Paint paint = new Paint();
    private ImageProcessor imageProcessor;
    private Bitmap bitmap;
    private ImageView imageView;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private Handler handler;
    private CameraManager cameraManager;

    // Của chúng ta
    private ObjectDetector objectDetector;
    private TextToSpeech tts;
    private ObstacleDetectorManager obstacleManager;
    private long lastSpokenTime = 0;
    private static final long SPEECH_COOLDOWN = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_detection);

        getPermission();
        initializeComponents();
    }

    private void initializeComponents() {
        // Khởi tạo TTS đơn giản
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(1.1f);
            }
        });

        obstacleManager = new ObstacleDetectorManager(this);

        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
            Log.d(TAG, "Loaded " + labels.size() + " labels");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load labels", e);
        }

        // Image processor giống Kotlin code
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        // Load model - đơn giản
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
            Log.d(TAG, "Model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model", e);
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Handler thread giống Kotlin code
        HandlerThread handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // Views
        imageView = findViewById(R.id.imageView);
        textureView = findViewById(R.id.textureView);
        boundingBoxOverlay = findViewById(R.id.boundingBoxOverlay);

        // TextureView listener
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // QUAN TRỌNG: Xử lý mỗi frame
                processFrame();
            }
        });

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    private void processFrame() {
        bitmap = textureView.getBitmap();
        if (bitmap == null) return;

        try {
            TensorImage image = TensorImage.fromBitmap(bitmap);
            image = imageProcessor.process(image);

            List<Detection> detections = objectDetector.detect(image);

            // Update overlay - SUPER SIMPLE
            boundingBoxOverlay.setDetections(detections);

            // Keep your existing drawing logic for ImageView
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            drawDetections(canvas, detections, mutableBitmap.getWidth(), mutableBitmap.getHeight());
            imageView.setImageBitmap(mutableBitmap);

        } catch (Exception e) {
            Log.e(TAG, "Frame processing error", e);
        }
    }

    private void drawDetections(Canvas canvas, List<Detection> detections, int width, int height) {
        if (detections == null || detections.isEmpty()) {
            return;
        }

        // Setup paint giống Kotlin code - GIỮ NGUYÊN
        float textSize = height / 15f;
        float strokeWidth = height / 85f;
        paint.setTextSize(textSize);
        paint.setStrokeWidth(strokeWidth);

        long currentTime = System.currentTimeMillis();

        ObstacleDetectorManager.DetectionResult bestResult = null;

        for (int i = 0; i < detections.size(); i++) {
            Detection detection = detections.get(i);
            if (detection.getCategories() != null && !detection.getCategories().isEmpty()) {
                org.tensorflow.lite.support.label.Category category = detection.getCategories().get(0);
                String objectName = category.getLabel();
                float confidence = category.getScore();
                RectF boundingBox = detection.getBoundingBox();

                // Vẽ bounding box - GIỮ NGUYÊN
                if (confidence > 0.5f) {
                    paint.setColor(colors[i % colors.length]);
                    paint.setStyle(Paint.Style.STROKE);

                    float left = boundingBox.left * width;
                    float top = boundingBox.top * height;
                    float right = boundingBox.right * width;
                    float bottom = boundingBox.bottom * height;

                    canvas.drawRect(left, top, right, bottom, paint);

                    paint.setStyle(Paint.Style.FILL);
                    String labelText = objectName + " " + String.format("%.1f", confidence);
                    canvas.drawText(labelText, left, top, paint);
                }

                ObstacleDetectorManager.DetectionResult result =
                        obstacleManager.analyzeDetection(objectName, confidence,
                                boundingBox.centerX(), width, currentTime);

                if (result != null) {
                    bestResult = result;
                    // Có thể break nếu chỉ muốn nói 1 object mỗi frame
                    // break;
                }
            }
        }

        // THAY ĐỔI: Sử dụng result từ Manager
        if (bestResult != null) {
            Log.d(TAG, "Speaking: " + bestResult.spokenMessage);
            speak(bestResult.spokenMessage);

            // Cập nhật trạng thái trong Manager
            obstacleManager.updateLastSpokenTime(currentTime);
            obstacleManager.updateLastSpokenObject(bestResult.objectName);
        }
    }

    private String getSimpleDirection(float centerX, int imageWidth) {
        float normalizedX = centerX / imageWidth;
        if (normalizedX < 0.4f) {
            return "left";
        } else if (normalizedX > 0.6f) {
            return "right";
        } else {
            return "straight";
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            cameraManager.openCamera(cameraManager.getCameraIdList()[0],
                    new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            cameraDevice = camera;

                            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                            if (surfaceTexture == null) return;

                            surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
                            Surface surface = new Surface(surfaceTexture);

                            try {
                                CaptureRequest.Builder captureRequest =
                                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                captureRequest.addTarget(surface);

                                cameraDevice.createCaptureSession(Arrays.asList(surface),
                                        new CameraCaptureSession.StateCallback() {
                                            @Override
                                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                                try {
                                                    session.setRepeatingRequest(captureRequest.build(), null, null);
                                                    Log.d(TAG, "Camera started - Real-time detection active");
                                                    speak("Obstacle detection started");
                                                } catch (CameraAccessException e) {
                                                    Log.e(TAG, "Camera session error", e);
                                                }
                                            }

                                            @Override
                                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                                Log.e(TAG, "Camera configuration failed");
                                            }
                                        }, handler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Camera access error", e);
                            }
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            cameraDevice.close();
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            Log.e(TAG, "Camera error: " + error);
                            cameraDevice.close();
                        }
                    }, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera open error", e);
        }
    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue setup
                if (textureView.getSurfaceTexture() != null) {
                    openCamera();
                }
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cleanup đơn giản
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (cameraDevice != null) {
            cameraDevice.close();
        }

        if (handler != null) {
            handler.getLooper().quitSafely();
        }

        if (objectDetector != null) {
            objectDetector.close();
        }

        // Cleanup obstacle manager
        if (obstacleManager != null) {
            obstacleManager.release();
        }


        Log.d(TAG, "Obstacle detection stopped");
    }
}