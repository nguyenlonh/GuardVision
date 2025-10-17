package com.guardvision.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ObjectDetectionActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private PreviewView previewView;
    private Button btnCapture;
    private TextView tvFeatureTitle;
    private TextView tvResult;
    private TextToSpeech textToSpeech;
    private ImageCapture imageCapture;
    private ObjectDetector objectDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);

        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        tvFeatureTitle = findViewById(R.id.tvFeatureTitle);
        tvResult = findViewById(R.id.tvResult);

        tvFeatureTitle.setText(R.string.object_detection);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                speak(getString(R.string.object_detection) + ". " + getString(R.string.tap_to_capture));
            }
        });

        // Initialize ML Kit Object Detector
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        objectDetector = ObjectDetection.getClient(options);

        btnCapture.setOnClickListener(v -> captureImage());

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                speak(getString(R.string.camera_permission_required));
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Toast.makeText(this, "Error binding camera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImage() {
        if (imageCapture == null) return;

        speak(getString(R.string.processing));
        btnCapture.setEnabled(false);

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        processImage(imageProxy);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(ObjectDetectionActivity.this,
                                "Capture failed: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnCapture.setEnabled(true);
                    }
                });
    }

    private void processImage(ImageProxy imageProxy) {
        @androidx.camera.core.ExperimentalGetImage
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        objectDetector.process(image)
                .addOnSuccessListener(detectedObjects -> {
                    StringBuilder result = new StringBuilder();
                    if (detectedObjects.isEmpty()) {
                        result.append("Không tìm thấy vật thể nào");
                    } else {
                        result.append("Tìm thấy ").append(detectedObjects.size()).append(" vật thể: ");
                        for (int i = 0; i < detectedObjects.size(); i++) {
                            if (detectedObjects.get(i).getLabels().isEmpty()) {
                                result.append("Vật thể không xác định");
                            } else {
                                String label = detectedObjects.get(i).getLabels().get(0).getText();
                                result.append(translateLabel(label));
                            }
                            if (i < detectedObjects.size() - 1) {
                                result.append(", ");
                            }
                        }
                    }
                    displayResult(result.toString());
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to detect objects: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnCapture.setEnabled(true);
                    imageProxy.close();
                });
    }

    private String translateLabel(String label) {
        // Basic translation mapping
        switch (label.toLowerCase()) {
            case "person":
                return "người";
            case "chair":
                return "ghế";
            case "table":
                return "bàn";
            case "bottle":
                return "chai";
            case "cup":
                return "cốc";
            case "book":
                return "sách";
            case "phone":
                return "điện thoại";
            case "laptop":
                return "máy tính xách tay";
            default:
                return label;
        }
    }

    private void displayResult(String text) {
        previewView.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(text);
        speak(text);

        // Return to home after speaking
        tvResult.postDelayed(() -> finish(), 5000);
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
        super.onDestroy();
    }
}
