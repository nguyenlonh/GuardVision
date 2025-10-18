package com.visualguard.finnalproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IngredientDetectActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 2001;
    private PreviewView previewView;
    private OverlayView overlayView;
    private TextToSpeech tts;
    private ExecutorService cameraExecutor;
    private IngredientDbHelper dbHelper;

    private final java.util.Map<String, Integer> consecutiveCount = new java.util.HashMap<>();
    private final java.util.Map<String, Long> lastSpokenAt = new java.util.HashMap<>();
    private static final int REQUIRED_FRAMES = 3;
    private static final long COOLDOWN_MS = 10_000L;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textdetect);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        dbHelper = new IngredientDbHelper(this);

        gestureDetector = new GestureDetector(this, new IngredientGestureListener());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                speak("Please scan the ingredient label");
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
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class IngredientGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX < 0) { // Swipe left - chuyển sang TextDetection
                    speak("Opening text reading mode");
                    Intent intent = new Intent(IngredientDetectActivity.this, TextDetectionActivity.class);
                    startActivity(intent);
                    return true;
                } else if (diffX > 0) { // Swipe right - quay về Home
                    speak("Returning to main menu");
                    finish();
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
                                        List<Text.TextBlock> blocks = text.getTextBlocks();
                                        List<RectF> mapped = mapBlocksToView(blocks, imageProxy);
                                        runOnUiThread(() -> overlayView.setRects(mapped));

                                        int viewW = previewView.getWidth();
                                        int viewH = previewView.getHeight();
                                        if (viewW == 0 || viewH == 0) {
                                            imageProxy.close();
                                            return;
                                        }

                                        float centerLeft = viewW * 0.25f;
                                        float centerTop = viewH * 0.25f;
                                        float centerRight = viewW * 0.75f;
                                        float centerBottom = viewH * 0.75f;
                                        RectF centerArea = new RectF(centerLeft, centerTop, centerRight, centerBottom);

                                        java.util.Set<String> candidates = new java.util.LinkedHashSet<>();

                                        for (int i = 0; i < blocks.size(); i++) {
                                            Text.TextBlock block = blocks.get(i);
                                            RectF rf = null;
                                            try { rf = mapped.get(i); } catch (Exception ignored) {}
                                            if (rf == null) continue;

                                            // only consider blocks whose center is inside centerArea and reasonably large
                                            float cx = rf.centerX();
                                            float cy = rf.centerY();
                                            float area = rf.width() * rf.height();
                                            float viewArea = (float) viewW * (float) viewH;
                                            if (!centerArea.contains(cx, cy)) continue;
                                            if (area < viewArea * 0.002f) continue; // ignore tiny boxes (<0.2% of view)

                                            String blockText = block.getText();
                                            java.util.Map<String, String> matches = dbHelper.findMatchesWithEffects(blockText);
                                            if (matches != null && !matches.isEmpty()) {
                                                for (String name : matches.keySet()) {
                                                    candidates.add(name);
                                                }
                                            }
                                        }

                                        // update consecutive counts and check cooldowns
                                        java.util.List<String> toSpeakNow = new java.util.ArrayList<>();
                                        long now = System.currentTimeMillis();
                                        // increase counters for present candidates, reset for others
                                        java.util.Set<String> present = candidates;
                                        java.util.Set<String> keys = new java.util.HashSet<>(consecutiveCount.keySet());
                                        for (String k : keys) {
                                            if (!present.contains(k)) consecutiveCount.put(k, 0);
                                        }
                                        for (String name : present) {
                                            int cnt = consecutiveCount.containsKey(name) ? consecutiveCount.get(name) : 0;
                                            cnt++;
                                            consecutiveCount.put(name, cnt);
                                            long last = lastSpokenAt.containsKey(name) ? lastSpokenAt.get(name) : 0L;
                                            if (cnt >= REQUIRED_FRAMES && (now - last) > COOLDOWN_MS) {
                                                toSpeakNow.add(name);
                                                consecutiveCount.put(name, 0); // reset after triggered
                                                lastSpokenAt.put(name, now);
                                            }
                                        }

                                        if (!toSpeakNow.isEmpty()) {
                                            StringBuilder sb = new StringBuilder();
                                            boolean first = true;
                                            for (String s : toSpeakNow) {
                                                // get effects text if any
                                                java.util.Map<String, String> map = dbHelper.findMatchesWithEffects(s);
                                                String effects = "";
                                                if (map != null && map.containsKey(s)) effects = map.get(s);
                                                if (!first) sb.append(". ");
                                                sb.append(s);
                                                if (effects != null && !effects.isEmpty()) {
                                                    sb.append(": ").append(effects);
                                                }
                                                first = false;
                                            }
                                            speak(sb.toString());
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("TextDetect", "OCR failed", e))
                                    .addOnCompleteListener(task -> imageProxy.close());

                        } catch (Exception e) {
                            e.printStackTrace();
                            try { imageProxy.close(); } catch (Exception ex) { ex.printStackTrace(); }
                        }
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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
            Rect r = block.getBoundingBox();
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
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "tts-id");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (cameraExecutor != null) cameraExecutor.shutdown();
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
