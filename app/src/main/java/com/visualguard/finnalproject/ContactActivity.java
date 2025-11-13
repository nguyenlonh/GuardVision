package com.visualguard.finnalproject;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;

public class ContactActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private View touchView;

    // Simple states
    private static final int STATE_NAME = 0;
    private static final int STATE_ACTION = 1;
    private static final int STATE_MESSAGE = 2;
    private static final int STATE_CONFIRM = 3;

    private int currentState = STATE_NAME;
    private boolean isListening = false;

    // Contact data
    private String contactName;
    private String phoneNumber;
    private String messageContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        initializeUI();
        initializeTTS();
    }

    private void initializeUI() {
        TextView tv = findViewById(R.id.contactStatus);
        if (tv != null) {
            tv.setText("Touch and hold to speak");
        }

        // Sử dụng toàn bộ layout làm touch area
        touchView = findViewById(android.R.id.content);
        setupTouchListener();
    }

    private void setupTouchListener() {
        touchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Bắt đầu nghe khi chạm và giữ
                        startListening();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Dừng nghe khi thả tay
                        stopListening();
                        return true;
                }
                return false;
            }
        });
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                speak("Touch and hold screen to speak a name");
            }
        });
    }

    private void startListening() {
        if (isListening) return;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }

        updateStatus("Listening...");
        isListening = true;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processVoiceCommand(matches.get(0).toLowerCase());
                }
                isListening = false;
                updateStatus("Touch and hold to speak");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                updateStatus("Touch and hold to try again");
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        isListening = false;
        updateStatus("Touch and hold to speak");
    }

    private void processVoiceCommand(String command) {
        switch (currentState) {
            case STATE_NAME:
                searchContact(command);
                break;
            case STATE_ACTION:
                handleAction(command);
                break;
            case STATE_MESSAGE:
                saveMessage(command);
                break;
            case STATE_CONFIRM:
                if (command.contains("ok")) {
                    sendMessage();
                } else {
                    speak("Say ok to send, or touch and hold to try again");
                }
                break;
        }
    }

    private void searchContact(String name) {
        contactName = name;
        speak("Searching for " + name);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_CONTACTS}, 2);
            return;
        }

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));
            cursor.close();

            updateStatus("Found: " + contactName);
            speak("Found " + contactName + ". Touch and hold, then say call or message");
            currentState = STATE_ACTION;
        } else {
            if (cursor != null) cursor.close();
            speak("Name not found. Touch and hold, then say another name");
        }
    }

    private void handleAction(String action) {
        if (action.contains("call")) {
            // Kiểm tra quyền CALL_PHONE trước khi gọi
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CALL_PHONE}, 3);
                return;
            }
            makePhoneCall();
        } else if (action.contains("message")) {
            speak("Touch and hold, then say your message");
            currentState = STATE_MESSAGE;
        } else {
            speak("Touch and hold, then say call or message");
        }
    }

    private void saveMessage(String message) {
        messageContent = message;
        speak("Your message: " + message + ". Touch and hold, then say ok to send");
        currentState = STATE_CONFIRM;
    }

    private void makePhoneCall() {
        if (phoneNumber != null) {
            try {
                boolean callStarted = false;

                // Ưu tiên sử dụng TelecomManager cho API 23+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    callStarted = makeDirectCallWithTelecomManager();
                }

                // Nếu TelecomManager không hoạt động, thử các phương pháp khác
                if (!callStarted) {
                    callStarted = makeCallWithSpecificDialer();
                }

                // Fallback cuối cùng: sử dụng Intent thông thường
                if (!callStarted) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                }

                speak("Calling " + contactName + " now");
                resetToStart();

            } catch (SecurityException e) {
                speak("Cannot make call. Permission denied");
                resetToStart();
            } catch (Exception e) {
                speak("Error making call");
                resetToStart();
            }
        } else {
            speak("No number found for " + contactName);
            resetToStart();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean makeDirectCallWithTelecomManager() {
        try {
            TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (telecomManager != null) {
                Uri uri = Uri.fromParts("tel", phoneNumber, null);
                Bundle extras = new Bundle();
                telecomManager.placeCall(uri, extras);
                return true;
            }
        } catch (Exception e) {
            // TelecomManager không khả dụng, tiếp tục với phương pháp khác
        }
        return false;
    }

    private boolean makeCallWithSpecificDialer() {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));

            // Thử các package dialer phổ biến
            String[] dialerPackages = {
                    "com.android.phone",
                    "com.android.dialer",
                    "com.google.android.dialer",
                    "com.samsung.android.dialer",
                    "com.huawei.dialer"
            };

            for (String pkg : dialerPackages) {
                try {
                    // Kiểm tra xem package có tồn tại không
                    getPackageManager().getPackageInfo(pkg, 0);
                    intent.setPackage(pkg);
                    startActivity(intent);
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                    // Package không tồn tại, tiếp tục thử package tiếp theo
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi và thử phương pháp tiếp theo
        }
        return false;
    }

    private void sendMessage() {
        if (phoneNumber != null && messageContent != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.SEND_SMS}, 4);
                return;
            }

            try {
                SmsManager.getDefault().sendTextMessage(phoneNumber, null, messageContent, null, null);
                speak("Message sent successfully to " + contactName);
                resetToStart();
            } catch (Exception e) {
                speak("Failed to send message. Please try again");
                resetToStart();
            }
        } else {
            speak("Cannot send message. Missing information");
            resetToStart();
        }
    }

    private void resetToStart() {
        new android.os.Handler().postDelayed(() -> {
            currentState = STATE_NAME;
            contactName = null;
            phoneNumber = null;
            messageContent = null;

            updateStatus("Touch and hold to speak");
            speak("Touch and hold screen to speak a name");
        }, 2000);
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> {
            TextView tv = findViewById(R.id.contactStatus);
            if (tv != null) tv.setText(message);
        });
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "contact");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, thử lại hành động
            if (requestCode == 3) { // CALL_PHONE permission
                makePhoneCall();
            } else if (requestCode == 4) { // SEND_SMS permission
                sendMessage();
            } else {
                updateStatus("Touch and hold to speak");
            }
        } else {
            speak("Permission needed to continue");
            resetToStart();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        speak("Returning to main screen");
        super.onBackPressed();
    }
}