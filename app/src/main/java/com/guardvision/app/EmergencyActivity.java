package com.guardvision.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Locale;

public class EmergencyActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private TextToSpeech textToSpeech;
    private TextView tvFeatureTitle;
    private TextView tvResult;
    private String emergencyNumber = "113"; // Default emergency number in Vietnam
    private String emergencyMessage = "Cần cấp cứu! Đây là tín hiệu khẩn cấp từ GuardVision.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);

        tvFeatureTitle = findViewById(R.id.tvFeatureTitle);
        tvResult = findViewById(R.id.tvResult);
        
        // Hide camera preview and capture button
        findViewById(R.id.previewView).setVisibility(android.view.View.GONE);
        findViewById(R.id.btnCapture).setVisibility(android.view.View.GONE);
        tvResult.setVisibility(android.view.View.VISIBLE);

        tvFeatureTitle.setText(R.string.emergency);
        tvResult.setText("Đang gửi tín hiệu cầu cứu...");

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                speak(getString(R.string.emergency) + ". Đang gửi tín hiệu cầu cứu.");
            }
        });

        // Vibrate for emergency alert
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(pattern, -1);
        }

        if (checkSmsPermission()) {
            sendEmergencySignal();
        } else {
            requestSmsPermission();
        }
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS},
                SMS_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencySignal();
            } else {
                String message = getString(R.string.sms_permission_required);
                tvResult.setText(message);
                speak(message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                tvResult.postDelayed(() -> finish(), 3000);
            }
        }
    }

    private void sendEmergencySignal() {
        try {
            // In a real app, you would send SMS to a pre-configured emergency contact
            // For demo purposes, we'll just simulate the action
            
            // Uncomment the following lines to actually send SMS:
            // SmsManager smsManager = SmsManager.getDefault();
            // smsManager.sendTextMessage(emergencyNumber, null, emergencyMessage, null, null);
            
            String message = getString(R.string.emergency_sent);
            tvResult.setText(message + "\n\nTín hiệu đã được ghi nhận.");
            speak(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            
            // Return to home after 3 seconds
            tvResult.postDelayed(() -> finish(), 3000);
        } catch (Exception e) {
            String errorMessage = "Lỗi khi gửi tín hiệu: " + e.getMessage();
            tvResult.setText(errorMessage);
            speak(errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            tvResult.postDelayed(() -> finish(), 3000);
        }
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
        super.onDestroy();
    }
}
