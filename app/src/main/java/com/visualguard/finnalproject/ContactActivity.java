package com.visualguard.finnalproject;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;

public class ContactActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;

    // App states
    private enum AppState { LISTENING_NAME, LISTENING_ACTION, LISTENING_MESSAGE }
    private AppState currentState = AppState.LISTENING_NAME;

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
            tv.setText("Say a contact name");
        }
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setSpeechRate(1.0f);

                    // Start by asking for contact name
                    speak("Say contact name");
                    startVoiceRecognition();
                }
            }
        });
    }

    private void startVoiceRecognition() {
        // Check audio permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                speak("Sorry, I didn't understand. Please try again.");
                startVoiceRecognition(); // Retry
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0).toLowerCase();
                    processVoiceCommand(spokenText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getPromptForCurrentState());

        speechRecognizer.startListening(intent);
    }

    private String getPromptForCurrentState() {
        switch (currentState) {
            case LISTENING_NAME:
                return "Say contact name";
            case LISTENING_ACTION:
                return "Say call or message";
            case LISTENING_MESSAGE:
                return "Say your message";
            default:
                return "Speak now";
        }
    }

    private void processVoiceCommand(String command) {
        switch (currentState) {
            case LISTENING_NAME:
                handleContactName(command);
                break;
            case LISTENING_ACTION:
                handleAction(command);
                break;
            case LISTENING_MESSAGE:
                handleMessage(command);
                break;
        }
    }

    private void handleContactName(String name) {
        contactName = name;
        speak("Searching for " + name);

        // Check contacts permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_CONTACTS}, 2);
            return;
        }

        findContactInAddressBook(name);
    }

    private void findContactInAddressBook(String name) {
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + name + "%"};

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, selectionArgs,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            String foundName = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));
            cursor.close();

            updateStatus("Found: " + foundName);
            speak("Found " + foundName + ". Say call to call or message to send text.");
            currentState = AppState.LISTENING_ACTION;
            startVoiceRecognition();

        } else {
            if (cursor != null) cursor.close();
            updateStatus("Contact not found");
            speak("Contact not found. Please say another name.");
            startVoiceRecognition(); // Retry
        }
    }

    private void handleAction(String action) {
        if (action.contains("call")) {
            makePhoneCall();
        } else if (action.contains("message") || action.contains("text")) {
            speak("What message do you want to send?");
            currentState = AppState.LISTENING_MESSAGE;
            startVoiceRecognition();
        } else {
            speak("Please say call or message.");
            startVoiceRecognition(); // Retry
        }
    }

    private void handleMessage(String message) {
        messageContent = message;
        speak("Message ready: " + message + ". Say send to confirm.");

        // Wait for send confirmation
        currentState = AppState.LISTENING_ACTION; // Reuse for send confirmation
        startVoiceRecognition();

        // Check if user says "send"
        if (message.toLowerCase().contains("send")) {
            sendSMS();
        }
    }

    private void makePhoneCall() {
        if (phoneNumber != null) {
            // Use ACTION_DIAL to open dialer (no CALL_PHONE permission needed)
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
            speak("Opening dialer for " + contactName);
        } else {
            speak("No phone number found for " + contactName);
        }
        finishAfterDelay();
    }

    private void sendSMS() {
        if (phoneNumber != null && messageContent != null) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", messageContent);
            startActivity(intent);
            speak("Opening message app for " + contactName);
        } else {
            speak("Cannot send message. Missing phone number or message content.");
        }
        finishAfterDelay();
    }

    private void finishAfterDelay() {
        // Wait a bit before finishing to let TTS complete
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 3000);
    }

    private void updateStatus(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.contactStatus);
                if (tv != null) {
                    tv.setText(message);
                }
            }
        });
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "contact-tts");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, retry the operation
            if (requestCode == 1) { // RECORD_AUDIO
                startVoiceRecognition();
            } else if (requestCode == 2) { // READ_CONTACTS
                findContactInAddressBook(contactName);
            }
        } else {
            speak("Permission denied. Please enable permissions in settings.");
            finish();
        }
    }
}