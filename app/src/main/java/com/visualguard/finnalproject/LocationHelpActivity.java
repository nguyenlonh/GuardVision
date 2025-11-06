package com.visualguard.finnalproject;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class LocationHelpActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private GestureDetector gestureDetector;

    // State management
    private boolean isWaitingForConfirmation = true;
    private boolean helpSignalSent = false;
    private boolean isBeepPlaying = false;

    // Constants
    private static final long AUTO_CANCEL_DELAY = 15000; // 15 seconds
    private static final String TELEGRAM_BOT_TOKEN = BuildConfig.TELEGRAM_BOT_TOKEN;
    private static final String TELEGRAM_CHAT_ID = BuildConfig.TELEGRAM_CHAT_ID;
    private static final String IPINFO_TOKEN = BuildConfig.IPINFO_TOKEN;

    // Runnable for auto-cancel
    private Runnable autoCancelRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_help);

        initializeUI();
        initializeHandlers();
        initializeTTS();
    }

    private void initializeUI() {
        TextView tv = findViewById(R.id.helpStatus);
        if (tv != null) {
            tv.setText("Emergency Help - Waiting for confirmation...");
        }
    }

    private void initializeHandlers() {
        handler = new Handler(Looper.getMainLooper());
        gestureDetector = new GestureDetector(this, new GestureListener());

        // Auto-cancel after 15 seconds - WILL PLAY BEEP SOUND
        autoCancelRunnable = new Runnable() {
            @Override
            public void run() {
                if (isWaitingForConfirmation && !helpSignalSent) {
                    speak("15 seconds passed. Alarm activated.");
                    startBeepLoop(); // Play beep sound after 15 seconds
                }
            }
        };
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setSpeechRate(1.0f);

                    // Immediate TTS announcement
                    speak("Help signal ready. Swipe up to confirm sending emergency alert in 15 seconds." +
                            " Swipe down to contact or message.");

                    // Start 15-second countdown
                    handler.postDelayed(autoCancelRunnable, AUTO_CANCEL_DELAY);
                } else {
                    // If TTS fails, still start countdown
                    handler.postDelayed(autoCancelRunnable, AUTO_CANCEL_DELAY);
                }
            }
        });
    }

    /**
     * Gesture Listener for swipe detection
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            // Prioritize vertical swipes
            if (Math.abs(diffX) < Math.abs(diffY)) {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        // SWIPE UP - Confirm emergency signal
                        handleSwipeUpConfirmation();
                        return true;
                    } else {
                        // SWIPE DOWN - Switch to contact mode
                        handleSwipeDownContact();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Handle swipe up - confirm and send emergency signal
     */
    private void handleSwipeUpConfirmation() {
        if (isWaitingForConfirmation && !helpSignalSent) {
            isWaitingForConfirmation = false;
            helpSignalSent = true;

            // Cancel the auto-cancel countdown
            handler.removeCallbacks(autoCancelRunnable);

            // Update UI
            updateStatusText("Sending emergency signal...");

            // Announce and send
            speak("Confirmed. Sending emergency help signal now.");
            startBeepLoop(); // Play beep sound immediately on confirmation
            new SendHelpTask().execute();
        }
    }

    /**
     * Handle swipe down - switch to contact activity
     */
    private void handleSwipeDownContact() {
        if (isWaitingForConfirmation) {
            // Cancel auto-cancel countdown
            handler.removeCallbacks(autoCancelRunnable);

            speak("Opening contact helper.");

            // Stop any beep sound that might be playing
            stopBeep();

            // Start ContactActivity
            Intent intent = new Intent(LocationHelpActivity.this, ContactActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Update status text on UI
     */
    private void updateStatusText(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.helpStatus);
                if (tv != null) {
                    tv.setText(message);
                }
            }
        });
    }

    /**
     * Speak text using TTS
     */
    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "location-help");
        }
    }

    /**
     * Start beep sound loop
     */
    private void startBeepLoop() {
        if (isBeepPlaying) return;

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep1);
            if (mediaPlayer == null) {
                speak("Alert sound activated");
                return;
            }

            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            isBeepPlaying = true;

        } catch (Exception e) {
            e.printStackTrace();
            speak("Alert activated");
        }
    }

    /**
     * Stop beep sound
     */
    private void stopBeep() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
            isBeepPlaying = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        // Cancel countdown and stop beep when back pressed
        handler.removeCallbacks(autoCancelRunnable);
        stopBeep();
        speak("Returning to main screen.");
        super.onBackPressed();
    }

    /**
     * AsyncTask to send help signal via Telegram
     */
    private class SendHelpTask extends AsyncTask<Void, Void, SendResult> {
        @Override
        protected SendResult doInBackground(Void... voids) {
            try {
                // Get location from ipinfo
                String locationInfo = getLocationInfo();

                // Create emergency message
                String message = "🚨 EMERGENCY HELP REQUEST 🚨\n" +
                        "I am a visually impaired person and I need immediate assistance!\n\n" +
                        "📍 Location Details:\n" + locationInfo + "\n\n" +
                        "⚠️ Please send help to this location immediately!";

                // Send via Telegram
                return sendTelegramMessage(message);

            } catch (Exception e) {
                e.printStackTrace();
                return new SendResult(false, "Error sending help message: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(SendResult result) {
            // Update UI
            updateStatusText(result.success ? "Help sent successfully!" : "Failed to send help");

            // Announce result
            speak(result.message);

            if (result.success) {
                // Continue beep sound and inform user
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speak("Emergency signal sent. Help is on the way.");
                    }
                }, 2000);
            } else {
                // Stop beep after delay if failed
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopBeep();
                        speak("Returning to main screen.");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 3000);
                    }
                }, 5000);
            }
        }

        private String getLocationInfo() throws Exception {
            String ipinfoUrl = "https://ipinfo.io/json";
            if (IPINFO_TOKEN != null && !IPINFO_TOKEN.isEmpty()) {
                ipinfoUrl += "?token=" + URLEncoder.encode(IPINFO_TOKEN, "UTF-8");
            }

            URL url = new URL(ipinfoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();

            return sb.toString();
        }

        private SendResult sendTelegramMessage(String message) throws Exception {
            String apiToken = TELEGRAM_BOT_TOKEN != null ? TELEGRAM_BOT_TOKEN : "";
            String chatId = TELEGRAM_CHAT_ID != null ? TELEGRAM_CHAT_ID : "";

            if (apiToken.isEmpty() || chatId.isEmpty()) {
                return new SendResult(false, "Missing Telegram configuration");
            }

            String urlString = "https://api.telegram.org/bot" + URLEncoder.encode(apiToken, "UTF-8")
                    + "/sendMessage?chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                    + "&text=" + URLEncoder.encode(message, "UTF-8");

            URL telegramUrl = new URL(urlString);
            HttpURLConnection telegramConn = (HttpURLConnection) telegramUrl.openConnection();
            telegramConn.setRequestMethod("GET");
            telegramConn.setConnectTimeout(10000);
            telegramConn.setReadTimeout(10000);

            int code = telegramConn.getResponseCode();
            telegramConn.getInputStream().close();

            if (code >= 200 && code < 300) {
                return new SendResult(true, "Help message sent successfully");
            } else {
                return new SendResult(false, "Telegram API error: " + code);
            }
        }
    }

    /**
     * Result class for async task
     */
    private class SendResult {
        final boolean success;
        final String message;

        SendResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (handler != null && autoCancelRunnable != null) {
            handler.removeCallbacks(autoCancelRunnable);
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        stopBeep();
    }
}