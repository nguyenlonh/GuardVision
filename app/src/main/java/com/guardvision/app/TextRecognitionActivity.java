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
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class TextRecognitionActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private PreviewView previewView;
    private Button btnCapture;
    private TextView tvFeatureTitle;
    private TextView tvResult;
    private TextToSpeech textToSpeech;
    private ImageCapture imageCapture;
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);

        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        tvFeatureTitle = findViewById(R.id.tvFeatureTitle);
        tvResult = findViewById(R.id.tvResult);

        tvFeatureTitle.setText(R.string.text_recognition);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                speak(getString(R.string.text_recognition) + ". " + getString(R.string.tap_to_capture));
            }
        });

        // Initialize ML Kit Text Recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

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
                        Toast.makeText(TextRecognitionActivity.this,
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

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String text = visionText.getText();
                    if (text.isEmpty()) {
                        text = "Không tìm thấy văn bản nào";
                    } else {
                        text = "Văn bản: " + text;
                    }
                    displayResult(text);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to recognize text: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnCapture.setEnabled(true);
                    imageProxy.close();
                });
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
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        super.onDestroy();
    }
}
