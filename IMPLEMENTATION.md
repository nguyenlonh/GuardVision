# GuardVision - Implementation Details

## Overview
GuardVision is an Android accessibility application designed specifically for visually impaired users. The app uses gesture-based navigation and Text-to-Speech (TTS) to provide a seamless, voice-guided experience.

## Architecture

### Main Components

#### 1. MainActivity (Home Screen)
- **Purpose**: Entry point and gesture navigation hub
- **Key Features**:
  - Gesture detection using Android's `GestureDetector`
  - Text-to-Speech initialization and voice guidance
  - Smart double-swipe detection with 500ms threshold
  - Automatic navigation to feature activities based on gestures

**Gesture Mapping**:
```
Swipe Left (Single)  → IngredientReaderActivity
Swipe Left (Double)  → TextRecognitionActivity
Swipe Right          → ObjectDetectionActivity
Swipe Up             → EmergencyActivity
Swipe Down           → PhoneStatusActivity
```

#### 2. IngredientReaderActivity
- **Purpose**: Read and announce product ingredient labels
- **Technology**: ML Kit Text Recognition
- **Flow**:
  1. Opens camera with CameraX
  2. User taps to capture image
  3. ML Kit processes image for text
  4. Announces "Thành phần: [detected text]" via TTS
  5. Auto-returns to home after 5 seconds

#### 3. TextRecognitionActivity
- **Purpose**: General text recognition for documents and signs
- **Technology**: ML Kit Text Recognition
- **Flow**:
  1. Opens camera with CameraX
  2. User taps to capture image
  3. ML Kit extracts text from image
  4. Announces "Văn bản: [detected text]" via TTS
  5. Auto-returns to home after 5 seconds

#### 4. ObjectDetectionActivity
- **Purpose**: Detect and identify objects in the environment
- **Technology**: ML Kit Object Detection
- **Features**:
  - Detects multiple objects simultaneously
  - Classifies objects using ML Kit's built-in categories
  - Vietnamese label translation for common objects
- **Flow**:
  1. Opens camera with CameraX
  2. User taps to capture image
  3. ML Kit detects and classifies objects
  4. Announces "Tìm thấy [N] vật thể: [list]" via TTS
  5. Auto-returns to home after 5 seconds

#### 5. EmergencyActivity
- **Purpose**: Send emergency distress signal
- **Features**:
  - Vibration pattern for tactile feedback
  - SMS capability (requires permission)
  - Visual and audio confirmation
- **Flow**:
  1. Vibrates phone with emergency pattern
  2. Announces emergency signal
  3. (Optional) Sends SMS to pre-configured contact
  4. Confirms action via TTS
  5. Returns to home after 3 seconds

#### 6. PhoneStatusActivity
- **Purpose**: Read current phone status information
- **Information Provided**:
  - Battery level and charging status
  - Current date and time
  - Device model
  - Network operator name
- **Flow**:
  1. Collects system information
  2. Formats information in Vietnamese
  3. Announces all status details via TTS
  4. Returns to home after 8 seconds

## Technical Implementation

### Gesture Detection
```java
private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, 
                          float velocityX, float velocityY) {
        // Detects direction and triggers appropriate handler
    }
}
```

### Double-Swipe Detection
- Uses timing-based approach
- 500ms threshold between swipes
- First swipe: Records timestamp, waits for potential second swipe
- Second swipe: If within threshold, triggers double-swipe action
- Timeout: If no second swipe, executes single-swipe action

### Text-to-Speech Integration
- Initializes with Vietnamese locale (`vi`, `VN`)
- Uses `QUEUE_FLUSH` mode to ensure latest message is heard
- Proper lifecycle management (stop/shutdown on destroy)

### CameraX Integration
- Modern camera API for reliable image capture
- `CAPTURE_MODE_MINIMIZE_LATENCY` for quick response
- Proper lifecycle binding to activities
- Back camera as default

### ML Kit Services
- **Text Recognition**: Latin script recognition with Vietnamese support
- **Object Detection**: 
  - Single image mode for on-demand detection
  - Multiple object detection enabled
  - Classification enabled for object labels

## Permissions

### Required Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.VIBRATE" />
```

### Runtime Permission Handling
- Camera: Requested before camera initialization
- SMS: Requested before sending emergency signal
- Phone State: Optional, gracefully degrades if denied

## UI/UX Design

### Accessibility-First Approach
- **Large Text**: 32sp for titles, 18-24sp for content
- **High Contrast**: Blue and green backgrounds with white text
- **Full-Screen**: Maximizes touch target areas
- **Audio Feedback**: Every action confirmed via TTS
- **Automatic Navigation**: Returns to home after completing actions

### Color Scheme
- Home Screen: Blue (#2196F3) - Calming, trustworthy
- Feature Screens: Green (#4CAF50) - Active, processing
- White Text: Maximum contrast for partially sighted users

### Layout Structure
- Minimalist design reduces visual clutter
- Large touch targets throughout
- Constraint-based layouts for consistency
- Portrait-only orientation for simplicity

## Dependencies

### Core Libraries
```gradle
// Android Support
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// ML Kit
implementation 'com.google.mlkit:text-recognition:16.0.0'
implementation 'com.google.mlkit:object-detection:17.0.1'

// CameraX
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'
implementation 'androidx.camera:camera-view:1.3.1'
```

## Build Configuration

### Minimum SDK: 24 (Android 7.0)
- Ensures wide device compatibility
- Supports essential accessibility features
- CameraX and ML Kit compatibility

### Target SDK: 34 (Android 14)
- Latest features and security updates
- Optimized for modern Android devices

### Compile SDK: 34
- Access to latest APIs and tools

## Future Enhancements

### Potential Improvements
1. **Configurable Emergency Contacts**: Allow users to set custom emergency numbers
2. **Voice Commands**: Add voice control as alternative to gestures
3. **Location Sharing**: Include GPS location in emergency signals
4. **Offline OCR**: Add offline text recognition capability
5. **Custom Object Training**: Allow training for user-specific objects
6. **Haptic Feedback Patterns**: Different vibrations for different features
7. **Multi-language Support**: Extend beyond Vietnamese
8. **Cloud Backup**: Sync settings and emergency contacts
9. **Medication Recognition**: Specialized mode for reading medicine labels
10. **Navigation Assistance**: Turn-by-turn audio navigation

## Testing Recommendations

### Unit Tests
- Gesture detection logic
- Double-swipe timing validation
- Text formatting functions
- Permission checking logic

### Integration Tests
- Camera initialization
- ML Kit processing pipeline
- TTS integration
- Activity navigation flow

### Accessibility Tests
- TalkBack compatibility
- Touch target sizes
- Audio feedback timing
- Screen reader support

### User Testing
- Test with actual visually impaired users
- Gather feedback on gesture intuitiveness
- Validate TTS voice and speed
- Assess emergency feature usability

## Code Quality

### Best Practices Implemented
- Proper lifecycle management (onCreate, onDestroy)
- Resource cleanup (TTS, ML Kit detectors)
- Permission handling with fallbacks
- Null safety checks
- Modular activity structure
- Consistent naming conventions
- Comprehensive comments
- Error handling with user feedback

### Android Guidelines Compliance
- Material Design principles
- Activity lifecycle handling
- Memory leak prevention
- Battery optimization considerations
- Permission best practices

## Conclusion

GuardVision demonstrates a thoughtful implementation of accessibility features for visually impaired users. The gesture-based navigation combined with comprehensive voice feedback creates an intuitive, independent user experience. The modular architecture allows for easy extension and maintenance while maintaining focus on core accessibility needs.
