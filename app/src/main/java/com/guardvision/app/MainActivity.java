package com.guardvision.app;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private GestureDetector gestureDetector;
    private TextView tvCurrentFeature;
    private long lastSwipeTime = 0;
    private static final long DOUBLE_SWIPE_TIME_THRESHOLD = 500; // milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCurrentFeature = findViewById(R.id.tvCurrentFeature);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                speak(getString(R.string.home_welcome) + ". " + getString(R.string.home_instructions));
            }
        });

        // Initialize gesture detector
        gestureDetector = new GestureDetector(this, new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void handleSwipeLeft() {
        long currentTime = System.currentTimeMillis();
        
        // Check if this is a double swipe (two swipes within threshold)
        if (currentTime - lastSwipeTime < DOUBLE_SWIPE_TIME_THRESHOLD) {
            // Double swipe left - Text Recognition
            tvCurrentFeature.setText(R.string.text_recognition);
            speak(getString(R.string.text_recognition));
            
            // Delay to allow TTS to finish before navigating
            tvCurrentFeature.postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, TextRecognitionActivity.class);
                startActivity(intent);
            }, 1000);
            
            lastSwipeTime = 0; // Reset
        } else {
            // Single swipe left - Ingredient Reader
            lastSwipeTime = currentTime;
            tvCurrentFeature.setText(R.string.ingredient_reader);
            speak(getString(R.string.ingredient_reader));
            
            // Wait to see if there's a second swipe
            tvCurrentFeature.postDelayed(() -> {
                if (System.currentTimeMillis() - lastSwipeTime >= DOUBLE_SWIPE_TIME_THRESHOLD) {
                    Intent intent = new Intent(MainActivity.this, IngredientReaderActivity.class);
                    startActivity(intent);
                }
            }, DOUBLE_SWIPE_TIME_THRESHOLD + 100);
        }
    }

    private void handleSwipeRight() {
        tvCurrentFeature.setText(R.string.object_detection);
        speak(getString(R.string.object_detection));
        
        tvCurrentFeature.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, ObjectDetectionActivity.class);
            startActivity(intent);
        }, 1000);
    }

    private void handleSwipeUp() {
        tvCurrentFeature.setText(R.string.emergency);
        speak(getString(R.string.emergency));
        
        tvCurrentFeature.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, EmergencyActivity.class);
            startActivity(intent);
        }, 1000);
    }

    private void handleSwipeDown() {
        tvCurrentFeature.setText(R.string.phone_status);
        speak(getString(R.string.phone_status));
        
        tvCurrentFeature.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, PhoneStatusActivity.class);
            startActivity(intent);
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // Horizontal swipe
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right
                        handleSwipeRight();
                    } else {
                        // Swipe left
                        handleSwipeLeft();
                    }
                    return true;
                }
            } else {
                // Vertical swipe
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        // Swipe down
                        handleSwipeDown();
                    } else {
                        // Swipe up
                        handleSwipeUp();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
