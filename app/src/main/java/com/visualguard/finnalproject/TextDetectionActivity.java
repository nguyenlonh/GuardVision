package com.visualguard.finnalproject;


import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextDetectionActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 2003;
    private PreviewView previewView;
    private TextToSpeech tts;
    private ExecutorService cameraExecutor;

    // Biến để quản lý việc đọc
    private String lastDetectedText = "";
    private long lastReadTime = 0;
    private static final long READ_COOLDOWN = 4000; // 8 giây chờ giữa các lần đọc

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textdetect);

        previewView = findViewById(R.id.previewView);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                speak("Text reading started. Point camera at text.");
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            startCamera();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
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

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @androidx.camera.core.ExperimentalGetImage
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        try {
                            Image mediaImage = imageProxy.getImage();
                            if (mediaImage == null) {
                                imageProxy.close();
                                return;
                            }

                            InputImage inputImage = InputImage.fromMediaImage(mediaImage,
                                    imageProxy.getImageInfo().getRotationDegrees());

                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                    .process(inputImage)
                                    .addOnSuccessListener(text -> {
                                        // Thu thập toàn bộ văn bản từ tất cả các dòng và đoạn
                                        processCompleteText(text);
                                        imageProxy.close();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TextDetection", "OCR failed", e);
                                        imageProxy.close();
                                    });

                        } catch (Exception e) {
                            Log.e("TextDetection", "Analyze exception", e);
                            imageProxy.close();
                        }
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("TextDetection", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processCompleteText(Text text) {
        // Xây dựng toàn bộ văn bản từ tất cả các khối (blocks), dòng (lines) và phần tử (elements)
        StringBuilder fullText = new StringBuilder();

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                if (lineText != null && !lineText.trim().isEmpty()) {
                    if (fullText.length() > 0) {
                        fullText.append(" "); // Thêm khoảng trắng giữa các dòng
                    }
                    fullText.append(lineText.trim());
                }
            }
        }

        String currentText = fullText.toString().trim();

        // Bỏ qua nếu không có văn bản hoặc quá ngắn
        if (currentText.isEmpty() || currentText.length() < 10) {
            return;
        }

        // Kiểm tra xem văn bản có đủ khác biệt so với lần trước không
        long currentTime = System.currentTimeMillis();
        boolean isDifferentEnough = isTextDifferentEnough(currentText, lastDetectedText);

        // Chỉ đọc nếu văn bản khác biệt đáng kể và đã đủ thời gian chờ
        if (isDifferentEnough && (currentTime - lastReadTime) > READ_COOLDOWN) {
            // Giới hạn độ dài văn bản để tránh quá dài
            String textToRead = currentText;
            if (textToRead.length() > 600) {
                textToRead = textToRead.substring(0, 600) + "...";
            }

            speak(textToRead);
            lastDetectedText = currentText;
            lastReadTime = currentTime;

            Log.d("TextDetection", "Reading text: " + textToRead);
        }
    }

    private boolean isTextDifferentEnough(String newText, String oldText) {
        if (oldText == null || oldText.isEmpty()) {
            return true;
        }

        // Tính toán độ tương đồng đơn giản
        int minLength = Math.min(newText.length(), oldText.length());
        if (minLength == 0) return true;

        int sameChars = 0;
        for (int i = 0; i < Math.min(newText.length(), oldText.length()); i++) {
            if (newText.charAt(i) == oldText.charAt(i)) {
                sameChars++;
            }
        }

        double similarity = (double) sameChars / minLength;

        // Coi là khác biệt nếu độ tương đồng dưới 70%
        return similarity < 0.7;
    }

    private void speak(String text) {
        if (tts != null) {
            // Sử dụng QUEUE_ADD thay vì QUEUE_FLUSH để không cắt ngang nếu đang nói
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "text-detection");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null) {
            tts.stop();
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}