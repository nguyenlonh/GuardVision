# GuardVision - Application Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         GuardVision App                         │
│                    (Android Application)                        │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 │
                    ┌────────────▼────────────┐
                    │    MainActivity         │
                    │   (Home Screen)         │
                    │                         │
                    │  - Gesture Detection    │
                    │  - TTS Integration      │
                    │  - Navigation Hub       │
                    └────────────┬────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
         ┌──────▼─────┐   ┌─────▼──────┐  ┌─────▼──────┐
         │   Swipe    │   │   Swipe    │  │   Swipe    │
         │    Left    │   │   Right    │  │  Up/Down   │
         └──────┬─────┘   └─────┬──────┘  └─────┬──────┘
                │               │               │
    ┌───────────┼───────┐       │       ┌───────┼────────┐
    │           │       │       │       │       │        │
┌───▼───┐  ┌───▼───┐   │   ┌───▼───┐   │   ┌───▼───┐   │
│Single │  │Double │   │   │Object │   │   │ Swipe │   │
│Swipe  │  │Swipe  │   │   │Detect │   │   │  Up   │   │
└───┬───┘  └───┬───┘   │   └───┬───┘   │   └───┬───┘   │
    │          │       │       │       │       │       │
┌───▼────────┐ │       │   ┌───▼────────┐ ┌───▼────────┐│
│Ingredient  │ │       │   │Object      │ │Emergency   ││
│Reader      │ │       │   │Detection   │ │Activity    ││
│Activity    │ │       │   │Activity    │ └─────┬──────┘│
└────────────┘ │       │   └────────────┘       │       │
               │       │                        │       │
        ┌──────▼─────┐ │                   ┌────▼──┐    │
        │Text        │ │                   │Send   │    │
        │Recognition │ │                   │Signal │    │
        │Activity    │ │                   │Vibrate│    │
        └────────────┘ │                   └───────┘    │
                       │                                │
                   ┌───▼────┐                       ┌───▼────┐
                   │Swipe   │                       │Phone   │
                   │Down    │                       │Status  │
                   └───┬────┘                       │Activity│
                       │                            └────────┘
                  ┌────▼──────┐
                  │Phone      │
                  │Status     │
                  │Activity   │
                  └───────────┘
```

## Component Relationships

### 1. MainActivity (Central Hub)
```
MainActivity
├── GestureDetector (Captures swipe gestures)
├── TextToSpeech (Voice feedback)
└── Navigation Logic
    ├── Single Swipe Left → IngredientReaderActivity
    ├── Double Swipe Left → TextRecognitionActivity
    ├── Swipe Right → ObjectDetectionActivity
    ├── Swipe Up → EmergencyActivity
    └── Swipe Down → PhoneStatusActivity
```

### 2. Feature Activities (Camera-based)
```
IngredientReaderActivity
├── CameraX (Camera control)
├── ML Kit Text Recognition
├── TextToSpeech (Result announcement)
└── Auto-return to MainActivity

TextRecognitionActivity
├── CameraX (Camera control)
├── ML Kit Text Recognition
├── TextToSpeech (Result announcement)
└── Auto-return to MainActivity

ObjectDetectionActivity
├── CameraX (Camera control)
├── ML Kit Object Detection
├── TextToSpeech (Result announcement)
└── Auto-return to MainActivity
```

### 3. Feature Activities (Non-camera)
```
EmergencyActivity
├── SMS Manager (Emergency SMS)
├── Vibrator (Haptic feedback)
├── TextToSpeech (Confirmation)
└── Auto-return to MainActivity

PhoneStatusActivity
├── BatteryManager (Battery info)
├── TelephonyManager (Network info)
├── System Info (Time, device)
├── TextToSpeech (Status announcement)
└── Auto-return to MainActivity
```

## Data Flow

### Gesture-to-Action Flow
```
User Swipe
    ↓
GestureDetector.onFling()
    ↓
MainActivity.handleSwipe[Direction]()
    ↓
[Time Check for Double Swipe if Left]
    ↓
Show Feature Name on Screen
    ↓
Announce Feature via TTS
    ↓
Wait 1 second
    ↓
Start Feature Activity
    ↓
Feature Activity onCreate()
    ↓
Initialize Camera/Sensors
    ↓
Announce Instructions via TTS
    ↓
Wait for User Action
    ↓
Process User Action
    ↓
Display and Announce Results
    ↓
Wait 3-8 seconds
    ↓
finish() → Return to MainActivity
```

### Camera Processing Flow
```
User Opens Camera Feature
    ↓
Check Camera Permission
    ↓
[If Denied] → Request Permission → [If Still Denied] → Exit
    ↓
[If Granted] → Initialize CameraX
    ↓
Bind Preview to PreviewView
    ↓
Wait for User Tap
    ↓
Capture Image (ImageProxy)
    ↓
Convert to InputImage
    ↓
Process with ML Kit
    ├── Text Recognition → Extract Text
    └── Object Detection → Detect Objects
    ↓
Format Results (Vietnamese)
    ↓
Display on Screen
    ↓
Announce via TTS
    ↓
Auto-return to Home
```

## Technology Stack

### Android Framework
```
┌──────────────────────────────────┐
│   Android SDK 24-34              │
│   (Android 7.0 - 14)             │
└──────────────────────────────────┘
         ↓
┌──────────────────────────────────┐
│   AndroidX Libraries             │
│   - AppCompat                    │
│   - ConstraintLayout             │
│   - Material Components          │
└──────────────────────────────────┘
```

### Machine Learning
```
┌──────────────────────────────────┐
│   Google ML Kit                  │
│   - Text Recognition             │
│   - Object Detection             │
└──────────────────────────────────┘
```

### Camera
```
┌──────────────────────────────────┐
│   AndroidX CameraX               │
│   - camera-core                  │
│   - camera-camera2               │
│   - camera-lifecycle             │
│   - camera-view                  │
└──────────────────────────────────┘
```

### Accessibility
```
┌──────────────────────────────────┐
│   Android TTS API                │
│   - Vietnamese Language Support  │
│   - QUEUE_FLUSH mode             │
└──────────────────────────────────┘
```

## Permission Architecture

```
┌─────────────────────────────────────────────────┐
│          AndroidManifest.xml                    │
├─────────────────────────────────────────────────┤
│  <uses-permission>                              │
│    - CAMERA (Runtime)                           │
│    - SEND_SMS (Runtime)                         │
│    - READ_PHONE_STATE (Runtime)                 │
│    - VIBRATE (Install-time)                     │
│  </uses-permission>                             │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│        Runtime Permission Checks                │
├─────────────────────────────────────────────────┤
│  Camera Features:                               │
│    checkCameraPermission() → Request → Start    │
│                                                 │
│  Emergency Feature:                             │
│    checkSmsPermission() → Request → Send        │
│                                                 │
│  Phone Status:                                  │
│    checkPhoneStatePermission() → (Optional)     │
└─────────────────────────────────────────────────┘
```

## Lifecycle Management

```
MainActivity Lifecycle:
├── onCreate()
│   ├── Initialize TTS
│   ├── Setup GestureDetector
│   └── Speak Welcome Message
├── onTouchEvent()
│   └── Delegate to GestureDetector
└── onDestroy()
    └── Shutdown TTS

Feature Activity Lifecycle:
├── onCreate()
│   ├── Initialize TTS
│   ├── Initialize Camera/Sensors
│   ├── Check Permissions
│   └── Speak Instructions
├── onUserAction()
│   ├── Process Action
│   ├── Display Result
│   └── Announce Result
└── onDestroy()
    ├── Shutdown TTS
    ├── Close ML Kit Detectors
    └── Release Camera
```

## State Management

### MainActivity States
- **IDLE**: Waiting for gesture
- **DETECTING**: Processing gesture
- **NAVIGATING**: Transitioning to feature
- **WAITING_DOUBLE_SWIPE**: Awaiting potential second swipe

### Feature Activity States
- **INITIALIZING**: Setting up camera/sensors
- **READY**: Waiting for user action
- **CAPTURING**: Taking photo
- **PROCESSING**: ML Kit processing
- **DISPLAYING**: Showing results
- **FINISHING**: Returning to home

## Error Handling

```
Error Scenarios:
├── Permission Denied
│   ├── Show Toast Message
│   ├── Speak Error via TTS
│   └── Return to MainActivity
├── Camera Initialization Failed
│   ├── Show Error Message
│   └── Retry or Exit
├── ML Kit Processing Failed
│   ├── Display Error
│   ├── Announce via TTS
│   └── Allow Retry
└── TTS Initialization Failed
    └── Continue with visual feedback only
```

## Performance Considerations

### Memory Management
- Activities properly release resources in onDestroy()
- ML Kit detectors closed when not needed
- Camera released after use
- TTS stopped before shutdown

### Battery Optimization
- Camera only active when needed
- ML Kit uses on-device processing
- Auto-return prevents idle battery drain
- Minimal background processing

### Response Time
- Gesture detection: < 100ms
- Camera preview: < 500ms
- ML Kit processing: 1-3 seconds
- TTS response: Immediate

## Security Considerations

### Data Privacy
- All ML Kit processing on-device
- No data sent to external servers
- Camera images not stored
- No user tracking or analytics

### Permission Usage
- Camera: Only during active feature use
- SMS: Only for emergency feature
- Phone State: Read-only, optional
- Vibrate: No sensitive data

## Scalability

### Adding New Features
1. Create new Activity class
2. Add to AndroidManifest.xml
3. Implement gesture handler in MainActivity
4. Add strings to strings.xml
5. Follow existing pattern for consistency

### Multi-language Support
1. Create values-[locale] directories
2. Translate strings.xml
3. Update TTS locale in activities
4. Test with target language

### Custom ML Models
1. Add model to assets/
2. Update ML Kit configuration
3. Test accuracy with target use case
4. Optimize for on-device performance

## Conclusion

The GuardVision architecture emphasizes:
- **Simplicity**: Clear, linear user flows
- **Accessibility**: Voice-first design
- **Privacy**: On-device processing
- **Reliability**: Robust error handling
- **Extensibility**: Modular structure for growth

This architecture ensures the app remains maintainable, performant, and focused on serving visually impaired users effectively.
