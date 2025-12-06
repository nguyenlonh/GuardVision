package com.visualguard.finnalproject.TextDetect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.visualguard.finnalproject.IngredientDetect.IngredientDetectActivity;
import com.visualguard.finnalproject.IngredientDetect.OverlayView;
import com.visualguard.finnalproject.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextDetectionActivity extends AppCompatActivity {

    private static final String TAG = "TextDetection";
    private static final int REQ_CAMERA = 2002;
    private static final long READ_COOLDOWN = 8000; // 8 seconds between readings

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextToSpeech tts;
    private ExecutorService cameraExecutor;
    private GestureDetector gestureDetector;

    private long lastReadTime = 0;
    private String lastReadText = "";
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textdetect);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        gestureDetector = new GestureDetector(this, new TextGestureListener());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(1.0f);
                speak("Text reading mode. Point camera at text to read aloud. Double tap to repeat last read text. Press back to return.");
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            startCamera();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class TextGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Double tap to repeat last read text
            if (lastReadText != null && !lastReadText.isEmpty()) {
                speak("Repeating: " + lastReadText);
            } else {
                speak("No text has been read yet");
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Single tap to stop current speech
            if (tts != null && tts.isSpeaking()) {
                tts.stop();
                speak("Speech stopped");
            }
            return true;
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

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @androidx.camera.core.ExperimentalGetImage
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (isProcessing) {
                            imageProxy.close();
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastReadTime < READ_COOLDOWN) {
                            imageProxy.close();
                            return;
                        }

                        try {
                            Image mediaImage = imageProxy.getImage();
                            if (mediaImage == null) {
                                imageProxy.close();
                                return;
                            }

                            isProcessing = true;

                            InputImage inputImage = InputImage.fromMediaImage(mediaImage,
                                    imageProxy.getImageInfo().getRotationDegrees());

                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                    .process(inputImage)
                                    .addOnSuccessListener(text -> {
                                        processDetectedText(text, imageProxy);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "OCR failed", e);
                                        isProcessing = false;
                                        imageProxy.close();
                                    })
                                    .addOnCompleteListener(task -> {
                                        isProcessing = false;
                                        imageProxy.close();
                                    });

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing image", e);
                            isProcessing = false;
                            try {
                                imageProxy.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processDetectedText(Text text, ImageProxy imageProxy) {
        List<Text.TextBlock> blocks = text.getTextBlocks();

        // Update overlay with bounding boxes
        List<RectF> mappedRects = mapBlocksToView(blocks, imageProxy);
        runOnUiThread(() -> overlayView.setRects(mappedRects));

        if (blocks.isEmpty()) {
            return;
        }

        // Get view dimensions for center area filtering
        int viewW = previewView.getWidth();
        int viewH = previewView.getHeight();
        if (viewW == 0 || viewH == 0) {
            return;
        }

        // Define center area (50% of screen)
        float centerLeft = viewW * 0.25f;
        float centerTop = viewH * 0.25f;
        float centerRight = viewW * 0.75f;
        float centerBottom = viewH * 0.75f;
        RectF centerArea = new RectF(centerLeft, centerTop, centerRight, centerBottom);

        // Collect text from blocks in center area
        StringBuilder textBuilder = new StringBuilder();
        float viewArea = (float) viewW * (float) viewH;

        for (int i = 0; i < blocks.size(); i++) {
            Text.TextBlock block = blocks.get(i);
            RectF rect = null;
            try {
                rect = mappedRects.get(i);
            } catch (Exception ignored) {
            }

            if (rect == null) continue;

            float cx = rect.centerX();
            float cy = rect.centerY();
            float blockArea = rect.width() * rect.height();

            // Only consider blocks in center area and not too small
            if (!centerArea.contains(cx, cy)) continue;
            if (blockArea < viewArea * 0.003f) continue; // Ignore very small text

            String blockText = block.getText().trim();
            if (!blockText.isEmpty()) {
                if (textBuilder.length() > 0) {
                    textBuilder.append(". ");
                }
                textBuilder.append(blockText);
            }
        }

        String detectedText = textBuilder.toString().trim();

        // Only read if there's meaningful text (minimum 3 characters)
        if (detectedText.length() >= 3) {
            // Check if text is significantly different from last read
            if (!isSimilarText(detectedText, lastReadText)) {
                lastReadText = detectedText;
                lastReadTime = System.currentTimeMillis();

                // Limit text length for TTS
                String textToSpeak = detectedText;
                if (textToSpeak.length() > 500) {
                    textToSpeak = textToSpeak.substring(0, 500) + "... text continues";
                }

                speak(textToSpeak);
            }
        }
    }

    private boolean isSimilarText(String newText, String oldText) {
        if (oldText == null || oldText.isEmpty()) return false;
        if (newText == null || newText.isEmpty()) return true;

        // Simple similarity check - if more than 70% similar, consider same
        String newLower = newText.toLowerCase().trim();
        String oldLower = oldText.toLowerCase().trim();

        if (newLower.equals(oldLower)) return true;

        // Check if one contains the other
        if (newLower.contains(oldLower) || oldLower.contains(newLower)) {
            return true;
        }

        // Check word overlap
        String[] newWords = newLower.split("\\s+");
        String[] oldWords = oldLower.split("\\s+");

        int matchCount = 0;
        for (String newWord : newWords) {
            for (String oldWord : oldWords) {
                if (newWord.equals(oldWord)) {
                    matchCount++;
                    break;
                }
            }
        }

        int maxWords = Math.max(newWords.length, oldWords.length);
        if (maxWords == 0) return true;

        float similarity = (float) matchCount / maxWords;
        return similarity > 0.8f;
    }

    private List<RectF> mapBlocksToView(List<Text.TextBlock> blocks, ImageProxy imageProxy) {
        List<RectF> out = new ArrayList<>();
        if (blocks == null || blocks.isEmpty()) return out;

        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int rotation = imageProxy.getImageInfo().getRotationDegrees();

        int rotW = imageWidth;
        int rotH = imageHeight;
        if (rotation == 90 || rotation == 270) {
            rotW = imageHeight;
            rotH = imageWidth;
        }

        int viewW = previewView.getWidth();
        int viewH = previewView.getHeight();

        if (viewW == 0 || viewH == 0) return out;

        float scaleX = viewW / (float) rotW;
        float scaleY = viewH / (float) rotH;
        float scale = Math.max(scaleX, scaleY);

        float offsetX = (viewW - rotW * scale) / 2f;
        float offsetY = (viewH - rotH * scale) / 2f;

        for (Text.TextBlock block : blocks) {
            android.graphics.Rect r = block.getBoundingBox();
            if (r == null) continue;
            float left = r.left * scale + offsetX;
            float top = r.top * scale + offsetY;
            float right = r.right * scale + offsetX;
            float bottom = r.bottom * scale + offsetY;
            out.add(new RectF(left, top, right, bottom));
        }
        return out;
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "text-detection-tts");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset cooldown on resume to allow immediate reading
        lastReadTime = 0;
    }

    @Override
    public void onBackPressed() {
        // Stop TTS before going back
        if (tts != null) {
            tts.stop();
        }

        // Navigate back to MainActivity
        Intent intent = new Intent(this, com.visualguard.finnalproject.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
}