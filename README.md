# GuardVision

GuardVision is an Android application written in Java and built with Gradle. It aims to provide an intelligent, vision-based security and monitoring platform on mobile devices. The project is currently in an early development stage and is designed to be easily extendable for future features.

---

## Project Goals

GuardVision aims to:

- Provide a smart security monitoring solution on Android devices.
- Leverage Android and Java for image and video processing (computer vision).
- Make it easy to extend and integrate additional AI, alerting, and logging modules.

---

## Architecture & Technology

- **Platform:** Android
- **Language:** Java
- **Build system:** Gradle (Kotlin DSL – `build.gradle.kts`)

Main structure:

- `app/` – Android application source code (activities, services, view models, resources, etc.).
- `build.gradle.kts` – Project-level Gradle configuration.
- `settings.gradle.kts` – Module declarations and project setup.
- `gradle/`, `gradlew`, `gradlew.bat` – Gradle wrapper scripts and configuration.
- `gradle.properties` – Gradle and JVM configuration.

You can open the project directly with **Android Studio**.

---

## Getting Started

### 1. System Requirements

- Android Studio (Giraffe/Koala or newer)
- JDK 17 (or the version defined in `gradle.properties`)
- Android SDK compatible with the `compileSdkVersion` / `targetSdkVersion` used in the `app` module

### 2. Clone and Open the Project

```bash
git clone https://github.com/nguyenlonh/GuardVision.git
cd GuardVision
```

Then:

1. Open Android Studio.
2. Select “Open an Existing Project”.
3. Choose the `GuardVision` directory.
4. Wait for Gradle sync to finish.

---

## Build & Run

### Build with Android Studio

1. Select a build variant (e.g. `debug` or `release`).
2. Click **Run** (Shift + F10) to launch on a device or emulator.
3. If using a physical device, make sure USB debugging is enabled.

### Build from the Command Line

```bash
./gradlew assembleDebug
# Or on Windows
gradlew.bat assembleDebug
```

The debug APK will be generated under:

```text
app/build/outputs/apk/debug/
```

---

## Project Structure (Planned)

This is the expected structure of the project; you can update it when the `app` module is fully implemented.

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

## Basic Usage

1. Install the app (debug build):

    - Build the APK with `./gradlew assembleDebug`.
    - Install the APK on your Android device.

2. On first launch, grant the required permissions:

    - Camera permission.
    - (Optional) Storage permission if the app needs to save images or videos.

3. Start monitoring from the main screen:

    - The app starts the camera preview and runs its detection logic.
    - When motion or a configured event is detected, the app can send notifications or log the event (depending on implementation).

4. Open the history/log screen (when implemented) to review past events.

---

## Configuration & Customization

Some important files for configuration:

- `gradle.properties`  
  Configure Gradle and JVM options.

- `build.gradle.kts` (root)  
  Global plugins, Gradle version, and shared dependencies.

- `app/build.gradle` (when available)  
  Android-specific configuration such as `minSdk`, `targetSdk`, and libraries (e.g. CameraX, ML Kit, OpenCV).

You can adjust these files to fit your target devices and features.

---

## Contributing

Contributions are welcome.

1. Fork this repository.
2. Create a feature branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Commit your changes:

   ```bash
   git commit -m "Short description of your changes"
   ```

4. Push the branch:

   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a Pull Request on GitHub.

---

## License

This project does not have an explicit license published yet.  
Please contact the author before using it in production or for commercial purposes.

---

## Contact

- Author: Nguyễn Lonh
- GitHub: [nguyenlonh](https://github.com/nguyenlonh)
