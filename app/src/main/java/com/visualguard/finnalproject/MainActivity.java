package com.visualguard.finnalproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private GestureDetector gestureDetector;
    private StatusManager statusManager;

    // Double swipe detection
    private int swipeLeftCount = 0;
    private static final int DOUBLE_SWIPE_MAX_DELAY = 800; // ms
    private Handler doubleSwipeHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(1.2f);
                speakGreeting();
            }
        });

        statusManager = new StatusManager(this);
        gestureDetector = new GestureDetector(this, new GestureListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset swipe count when returning to main activity
        swipeLeftCount = 0;
        doubleSwipeHandler.removeCallbacksAndMessages(null);
        speakGreeting();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            // Horizontal swipe (left/right)
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX < 0) { // swipe left
                        handleSwipeLeft();
                        return true;
                    } else if (diffX > 0) { // swipe right - OBSTACLE DETECTION
                        handleSwipeRight();
                        return true;
                    }
                }
            } else {
                // Vertical swipe (up/down)
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) { // swipe up (finger moves up)
                        float startY = e1.getY();
                        int height = getWindow().getDecorView().getHeight();
                        if (startY > height * 0.7f) {
                            speak("Emergency help activated");
                            Intent intent = new Intent(MainActivity.this, LocationHelpActivity.class);
                            startActivity(intent);
                        } else {
                            speak("Swipe up from bottom for emergency");
                        }
                    } else { // swipe down
                        speak("Getting status update");
                        statusManager.speakStatus();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private void handleSwipeLeft() {
        swipeLeftCount++;

        if (swipeLeftCount == 1) {
            // First swipe left
            speak("Swipe left again for text reading");
            doubleSwipeHandler.postDelayed(() -> {
                // Timeout - chỉ có 1 swipe
                if (swipeLeftCount == 1) {
                    speak("Opening ingredient scanner");
                    Intent intent = new Intent(MainActivity.this, IngredientDetectActivity.class);
                    startActivity(intent);
                    swipeLeftCount = 0;
                }
            }, DOUBLE_SWIPE_MAX_DELAY);
        } else if (swipeLeftCount == 2) {
            // Double swipe left
            doubleSwipeHandler.removeCallbacksAndMessages(null);
            speak("Opening text reader");
            Intent intent = new Intent(MainActivity.this, TextDetectionActivity.class);
            startActivity(intent);
            swipeLeftCount = 0;
        }
    }

    private void handleSwipeRight() {
        speak("Opening obstacle detection");
        Intent intent = new Intent(MainActivity.this, ObstacleDetectionActivity.class);
        startActivity(intent);
    }

    private void speakGreeting() {
        String greeting = "Welcome. " +
                "Swipe left once for ingredient scanner, " +
                "twice for text reader. " +
                "Swipe right for obstacle detection. " +
                "Swipe up from bottom for emergency help. " +
                "Swipe down for status update.";
        speak(greeting);
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "main-tts");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (statusManager != null) {
            statusManager.shutdown();
            statusManager = null;
        }
        doubleSwipeHandler.removeCallbacksAndMessages(null);
    }
}