package com.visualguard.finnalproject;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
    private Runnable scheduledBeepRunnable;

    private static final String TELEGRAM_BOT_TOKEN = BuildConfig.TELEGRAM_BOT_TOKEN;
    private static final String TELEGRAM_CHAT_ID = BuildConfig.TELEGRAM_CHAT_ID;
    private static final String IPINFO_TOKEN = BuildConfig.IPINFO_TOKEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_help);

        TextView tv = findViewById(R.id.helpStatus);
        if (tv != null) tv.setText("Sending help signal...");

        handler = new Handler(Looper.getMainLooper());

        // init TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(1.0f);
                // queue initial message and then start sending
                tts.speak("Sending help signal. Please wait.", TextToSpeech.QUEUE_ADD, null, "help-start");
                new SendHelpTask().execute();
            } else {
                // If TTS init fails, still run the send task
                new SendHelpTask().execute();
            }
        });
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "location-help-tts");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private class SendResult {
        final boolean success;
        final String message;
        SendResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }



    private class SendHelpTask extends AsyncTask<Void, Void, SendResult> {
        @Override
        protected SendResult doInBackground(Void... voids) {
            try {
                // Build ipinfo URL (if IPINFO_TOKEN empty, omit token)
                String ipinfoUrl = "https://ipinfo.io/json";
                if (IPINFO_TOKEN != null && !IPINFO_TOKEN.isEmpty()) {
                    ipinfoUrl += "?token=" + URLEncoder.encode(IPINFO_TOKEN, "UTF-8");
                }

                URL url = new URL(ipinfoUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                in.close();

                String message = "I am a blind person and I need help!\nLocation: " + sb.toString();

                String apiToken = TELEGRAM_BOT_TOKEN != null ? TELEGRAM_BOT_TOKEN : "";
                String chatId = TELEGRAM_CHAT_ID != null ? TELEGRAM_CHAT_ID : "";
                if (apiToken.isEmpty() || chatId.isEmpty()) {
                    return new SendResult(false, "Missing Telegram token or chat id");
                }

                String urlString = "https://api.telegram.org/bot" + URLEncoder.encode(apiToken, "UTF-8")
                        + "/sendMessage?chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                        + "&text=" + URLEncoder.encode(message, "UTF-8");
                URL telegramUrl = new URL(urlString);
                HttpURLConnection telegramConn = (HttpURLConnection) telegramUrl.openConnection();
                telegramConn.setRequestMethod("GET");
                int code = telegramConn.getResponseCode();
                telegramConn.getInputStream().close();

                if (code >= 200 && code < 300) {
                    return new SendResult(true, "Help message sent successfully");
                } else {
                    return new SendResult(false, "Telegram returned code: " + code);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return new SendResult(false, "Error sending help message");
            }
        }

        @Override
        protected void onPostExecute(SendResult result) {
            // speak result and start beep depending on success/failure
            String utteranceId = "send-help-result";
            // set listener to start beep after speech finishes
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String uttId) { /* no-op */ }

                @Override
                public void onDone(String uttId) {
                    // This runs on TTS thread; post to UI thread
                    runOnUiThread(() -> {
                        if (result.success) {
                            scheduleBeepDelay(20_000L);
                        } else {
                            startBeepLoop();
                        }
                        tts.setOnUtteranceProgressListener(null);
                    });
                }

                @Override
                public void onError(String uttId) {
                    // on error, decide fallback: start beep immediately for safety
                    runOnUiThread(() -> {
                        if (!result.success) startBeepLoop();
                        tts.setOnUtteranceProgressListener(null);
                    });
                }
            });

            // queue the result message (won't cut previous queued messages)
            tts.speak(result.message, TextToSpeech.QUEUE_ADD, null, utteranceId);
        }
    }

    private void scheduleBeepDelay(long delayMs) {
        cancelScheduledBeep();
        scheduledBeepRunnable = this::startBeepLoop;
        handler.postDelayed(scheduledBeepRunnable, delayMs);
    }

    private void cancelScheduledBeep() {
        if (handler != null && scheduledBeepRunnable != null) {
            handler.removeCallbacks(scheduledBeepRunnable);
            scheduledBeepRunnable = null;
        }
    }

    private void startBeepLoop() {
        // If already playing, do nothing
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep1);
            if (mediaPlayer == null) {
                // not found or failed
                tts.speak("Beep sound not found.", TextToSpeech.QUEUE_ADD, null, "beep-notfound");
                return;
            }
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            tts.speak("Alarm started.", TextToSpeech.QUEUE_ADD, null, "beep-started");
        } catch (Exception e) {
            e.printStackTrace();
            tts.speak("Failed to start alarm sound.", TextToSpeech.QUEUE_ADD, null, "beep-fail");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelScheduledBeep();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
