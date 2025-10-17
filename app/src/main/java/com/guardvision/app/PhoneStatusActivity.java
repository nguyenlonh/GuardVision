package com.guardvision.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhoneStatusActivity extends AppCompatActivity {
    private static final int PHONE_STATE_PERMISSION_CODE = 102;
    private TextToSpeech textToSpeech;
    private TextView tvFeatureTitle;
    private TextView tvResult;

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

        tvFeatureTitle.setText(R.string.phone_status);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                speak(getString(R.string.phone_status));
                readPhoneStatus();
            }
        });
    }

    private void readPhoneStatus() {
        StringBuilder status = new StringBuilder();
        
        // Get battery status
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);
            
            int chargingStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                               chargingStatus == BatteryManager.BATTERY_STATUS_FULL;
            
            status.append("Pin: ").append(batteryPct).append(" phần trăm");
            if (isCharging) {
                status.append(", đang sạc");
            }
            status.append("\n\n");
        }
        
        // Get current time
        SimpleDateFormat sdf = new SimpleDateFormat("HH giờ mm phút, dd tháng MM năm yyyy", new Locale("vi", "VN"));
        String currentTime = sdf.format(new Date());
        status.append("Thời gian: ").append(currentTime);
        status.append("\n\n");
        
        // Get device model
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        status.append("Thiết bị: ").append(deviceModel);
        status.append("\n\n");
        
        // Get network status
        if (checkPhoneStatePermission()) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                String networkOperator = telephonyManager.getNetworkOperatorName();
                if (networkOperator != null && !networkOperator.isEmpty()) {
                    status.append("Mạng: ").append(networkOperator);
                } else {
                    status.append("Mạng: Không có tín hiệu");
                }
            }
        } else {
            status.append("Mạng: Cần quyền để kiểm tra");
        }
        
        String statusText = status.toString();
        tvResult.setText(statusText);
        speak(statusText);
        
        // Return to home after speaking
        tvResult.postDelayed(() -> finish(), 8000);
    }

    private boolean checkPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
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
