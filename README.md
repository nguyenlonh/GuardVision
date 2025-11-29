# GuardVision

GuardVision is an Android application (Java) built with Gradle, aiming to provide an intelligent, vision-based security and monitoring platform. The project is currently in an early development stage, with the structure ready for future feature expansion.

> Note: The following content is a professional template. You can adjust the feature descriptions, goals, and screenshots to match the actual product.

---

## 🎯 Project Goals

GuardVision aims to:

- Provide a smart security monitoring solution on mobile devices.
- Leverage the power of Android and Java for image/video processing (computer vision).
- Make it easy to extend and integrate additional AI, alert, and logging modules.

---

## 🏗 Architecture & Technology

- **Platform:** Android
- **Language:** Java (100% of the current codebase)
- **Build system:** Gradle (KTS)
- **Main structure:**
  - `app/` – Android application source code (activities, fragments, services, view models, resources, etc.).
  - `build.gradle.kts` – Project-level Gradle configuration.
  - `settings.gradle.kts` – Module declarations and project setup.
  - `gradle/`, `gradlew`, `gradlew.bat` – Gradle wrapper for easy builds across environments.

You can open the project directly using **Android Studio**.

---

## 🚀 Getting Started

### 1. System Requirements

- **Android Studio** (Giraffe/Koala or newer).
- **JDK 17** (or the version defined in `gradle.properties`).
- Android SDK compatible with the `compileSdkVersion` / `targetSdkVersion` defined in the `app` module.

### 2. Clone and Open the Project

```bash
git clone https://github.com/nguyenlonh/GuardVision.git
cd GuardVision
```

Then:

1. Open **Android Studio**.
2. Choose **“Open an Existing Project”**.
3. Select the `GuardVision` directory.
4. Wait for Gradle sync to complete.

---

## 🧪 Build & Run

### Build with Android Studio

1. Select a build variant (e.g. `debug` or `release`).
2. Click **Run** (Shift + F10) to launch on a real device or emulator.
3. Ensure **USB debugging** is enabled if using a physical device.

### Build from the Command Line

```bash
./gradlew assembleDebug
# Or on Windows
gradlew.bat assembleDebug
```

The APK will be generated under `app/build/outputs/apk/`.

---

## 📂 Project Structure (Planned)

> You can update this section once the `app` source code is fully implemented.

```text
GuardVision/
├─ app/
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/com/yourpackage/guardvision/...
│  │  │  ├─ res/...
│  │  │  └─ AndroidManifest.xml
│  └─ build.gradle
├─ gradle/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
└─ README.md
```

---

## 🖼 Screenshots

> Replace the images and descriptions below with real screenshots once your UI is ready.

### Main Screen

![Main Screen](docs/images/main-screen.png)

_Description:_  
Displays the real-time camera preview, connection status, and quick actions such as start/stop monitoring, capture image, and record video.

### History / Log Screen

![History Screen](docs/images/history-screen.png)

_Description:_  
Shows a list of recorded events (motion, intrusion, alerts, etc.) and allows filtering by time and event type.

### Settings Screen

![Settings Screen](docs/images/settings-screen.png)

_Description:_  
Allows configuring detection sensitivity, monitoring zones, and alert channels (notifications, sounds, email, etc.).

> You can create a `docs/images/` directory in the repo and place the corresponding PNG/JPG files there.

---

## 📚 API Docs

> This section describes the main internal services/classes/APIs provided by the app. Update class/package names according to the actual codebase.

### 1. Camera & Vision

#### `CameraManager`

- **Purpose:** Manage camera lifecycle, open/close camera, and switch between front/back camera.
- **Key methods (example):**
  - `startCamera()` – Initialize and show the camera preview.
  - `stopCamera()` – Release camera resources.
  - `switchCamera()` – Switch between front and rear cameras (if needed).

#### `VisionProcessor`

- **Purpose:** Process frames from the camera to detect objects/events.
- **Key methods (example):**
  - `processFrame(Image frame)` – Take a frame as input and return analysis results.
  - `setSensitivity(level: Int)` – Configure detection sensitivity.
  - `enableMotionDetection(enabled: Boolean)` – Enable/disable motion detection.

### 2. Alerts & Logging

#### `AlertService`

- **Purpose:** Send alerts to the user when important events are detected.
- **Key methods (example):**
  - `sendNotification(title: String, message: String)` – Send a local notification.
  - `triggerAlarm()` – Play a local alarm sound.
  - `sendRemoteAlert(payload: AlertPayload)` – Send alerts to a remote server (if a backend exists).

#### `EventLogger`

- **Purpose:** Log events (timestamp, event type, evidence images, etc.).
- **Key methods (example):**
  - `logEvent(type: EventType, timestamp: Long, meta: Map<String, Any>)`
  - `getEvents(filter: EventFilter): List<Event>`

### 3. Configuration & Storage

#### `SettingsRepository`

- **Purpose:** Persist and retrieve app settings (SharedPreferences or DataStore).
- **Key methods (example):**
  - `getSensitivity(): Int`
  - `setSensitivity(level: Int)`
  - `isNotificationEnabled(): Boolean`
  - `setNotificationEnabled(enabled: Boolean)`

> If you generate Javadoc/KDoc, you can export it and link it here, or reference a separate file, e.g.:  
> `docs/api/guardvision-api.md`

---

## 📖 Detailed User Guide

### 1. Installing the App

- Option 1: **From APK**

  1. Build the APK: `./gradlew assembleDebug`
  2. Copy the APK from `app/build/outputs/apk/debug/app-debug.apk` to your phone.
  3. Open the APK on the device and install it (enable “install from unknown sources” if required).

- Option 2: **From Android Studio**

  1. Connect your phone via USB or start an Android Emulator.
  2. Click **Run** in Android Studio.
  3. Select the target device and wait for the app to be installed and launched.

---

### 2. Basic Usage Flow

1. **Grant Permissions**

   - On the first launch, the app will request:
     - **Camera** permission.
     - (Optional) **Storage** permission to save images/videos.
   - Tap **Allow** to enable full functionality.

2. **Start Monitoring**

   - On the main screen, tap **Start Monitoring** (or the equivalent action).
   - The camera stream will start, and the detection algorithm will begin processing incoming frames.

3. **Receive Alerts**

   - When motion or a defined event is detected (depending on your logic), the app can:
     - Send a **notification**.
     - Optionally play an **alarm sound**.
     - Optionally save an **image/frame** or a **short video clip**.

4. **View History**

   - Open the **History/Logs** tab.
   - Select an event to view details:
     - Timestamp.
     - Event type.
     - Snapshot/preview image (if available).

5. **Adjust Settings**

   - Go to the **Settings** screen:
     - Adjust **detection sensitivity**.
     - Enable/disable **push notifications**.
     - Choose the **storage mode** (metadata only, metadata + images, etc.).

---

### 3. Example Use Cases

- **Home/Office Entrance Monitoring**  
  Place a phone near a door and enable GuardVision to detect unauthorized movements.

- **Pet Monitoring**  
  Track your pets at home and receive alerts when they enter restricted areas.

- **Temporary IP Camera Replacement**  
  Use an old phone as a temporary security camera at your desired location.

---

## 🔧 Configuration & Customization

Some important configuration points you may want to adjust:

- `gradle.properties`  
  - JVM, Gradle, and caching configuration.
- `build.gradle.kts` (root)  
  - Global plugins, Gradle versions, and common dependencies.
- `app/build.gradle` (when available)  
  - `minSdk`, `targetSdk`, and feature-specific dependencies (CameraX, OpenCV, ML Kit, etc. if used).

---

## 🗺 Roadmap (Suggested)

You can adjust this roadmap to match the real plan:

- [ ] Design main screen UI/UX.
- [ ] Integrate real-time camera preview.
- [ ] Implement basic image processing (motion detection, frame analysis).
- [ ] Integrate AI/ML models (face detection, object detection, intrusion detection, etc.).
- [ ] Implement alert system (notifications, logs, video capture).
- [ ] Implement history/events screen.
- [ ] Optimize performance and memory usage.
- [ ] Add automated tests (Unit tests, Instrumentation tests).
- [ ] Improve documentation and detailed API docs.

---

## 🤝 Contributing

Contributions are welcome to help improve GuardVision.

1. Fork the repository.
2. Create a new branch:  
   `git checkout -b feature/your-feature-name`
3. Commit your changes:  
   `git commit -m "Short description of your changes"`
4. Push to your branch:  
   `git push origin feature/your-feature-name`
5. Open a **Pull Request** on GitHub.

---

## 📝 License

(Replace the text below with the actual license you plan to use, e.g. MIT/Apache-2.0)

This project **does not have an explicit license published yet**. Please contact the author before using it in production or for commercial purposes.

---

## 📬 Contact

- Author: **Nguyễn nh**  
- GitHub: [nguyenlonh](https://github.com/nguyenlonh)
