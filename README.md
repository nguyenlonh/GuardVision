# GuardVision

<p align="center">
  <a href="https://github.com/nguyenlonh/GuardVision" target="_blank">
    <img src="[logo-url]" width="400" alt="GuardVision Logo">
  </a>
</p>

<p align="center">
  <a href="https://github.com/nguyenlonh/GuardVision/actions"><img src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" alt="Build Status"></a>
  <a href="https://github.com/nguyenlonh/GuardVision/releases"><img src="https://img.shields.io/badge/version-v1.0-blue?style=flat-square" alt="Latest Version"></a>
  <a href="https://github.com/nguyenlonh/GuardVision"><img src="https://img.shields.io/github/stars/nguyenlonh/GuardVision?style=flat-square" alt="GitHub Stars"></a>
</p>

---

## About

**GuardVision** is an innovative Android application designed to assist visually impaired users by leveraging computer vision and gesture-based controls. The app transforms smartphone cameras into powerful tools for navigation, reading, and daily assistance, providing audio feedback through text-to-speech technology.

Built with Java and modern Android development practices, GuardVision integrates advanced machine learning models for real-time object detection, text recognition, and spatial awareness. This project demonstrates the potential of AI in enhancing accessibility and independence for users with visual impairments.

---

## Features

- **Obstacle Detection**: Real-time camera-based obstacle detection to help users navigate safely.
- **Text Recognition**: Optical Character Recognition (OCR) to read and vocalize text from images or surroundings.
- **Ingredient Scanner**: Specialized detection for identifying food ingredients and labels.
- **Emergency Assistance**: Location-based help signaling with integration to Telegram for notifications.
- **Gesture Controls**: Intuitive swipe gestures for navigation without visual interfaces:
  - Swipe right: Activate obstacle detection
  - Single swipe left: Open ingredient scanner
  - Double swipe left: Open text reader
  - Swipe up from bottom: Emergency help
  - Swipe down: Status update
- **Audio Feedback**: Text-to-speech integration for all interactions and results.
- **Location Services**: GPS-based location sharing for emergency situations.

---

## Tech Stack

- **Language**: Java 11
- **Platform**: Android (minSdk 29, targetSdk 35)
- **Build Tool**: Gradle with Kotlin DSL
- **Core Libraries**:
  - Google ML Kit (Text Recognition, Barcode Scanning)
  - TensorFlow Lite (Object Detection, Vision Tasks)
  - CameraX (Camera integration)
  - Google Maps & Location Services
  - OkHttp (Networking)
- **UI Framework**: Material Design Components
- **Testing**: JUnit, Espresso

---

## Installation

### Prerequisites

- **Android Studio**: Flamingo or later (recommended)
- **Java Development Kit (JDK)**: Version 11 or higher
- **Android Device/SDK**: API level 29 (Android 10) or higher
- **API Keys**: Configure the following in `local.properties`:
  - `TELEGRAM_BOT_TOKEN`: For emergency notifications
  - `TELEGRAM_CHAT_ID`: Chat ID for Telegram bot
  - `IPINFO_TOKEN`: For IP-based location services
  - `GOOGLE_MAPS_API_KEY`: For Google Maps integration

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/nguyenlonh/GuardVision.git
   cd GuardVision
   ```

2. **Configure API Keys**
   Create a `local.properties` file in the root directory and add your API keys:
   ```
   TELEGRAM_BOT_TOKEN=your_telegram_bot_token
   TELEGRAM_CHAT_ID=your_telegram_chat_id
   IPINFO_TOKEN=your_ipinfo_token
   GOOGLE_MAPS_API_KEY=your_google_maps_api_key
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

4. **Build and Run**
   - Sync the project with Gradle files
   - Connect an Android device or start an emulator
   - Run the app from Android Studio

### Permissions Required

The app requires the following permissions (automatically requested):
- Camera access for vision features
- Location access for emergency services
- Internet access for API calls and notifications

---

## Usage

Upon launching GuardVision, the app greets users with voice instructions. Use the following gestures to navigate:

- **Swipe Right**: Start obstacle detection mode
- **Swipe Left Once**: Access ingredient detection
- **Swipe Left Twice**: Activate text recognition
- **Swipe Up (from bottom)**: Send emergency help signal
- **Swipe Down**: Get status update

All detections and interactions provide immediate audio feedback.

---

## Architecture

The app follows a modular architecture with separate activities for each detection mode:

- `MainActivity`: Gesture handling and navigation hub
- `ObstacleDetectionActivity`: Real-time object detection using TensorFlow Lite
- `TextDetectionActivity`: OCR using Google ML Kit
- `IngredientDetectActivity`: Specialized ingredient recognition
- `LocationHelpActivity`: Emergency location sharing via Telegram
- `ContactActivity`: Contact management for emergency notifications
- `StatusManager`: Centralized status and TTS management

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- Google ML Kit for computer vision capabilities
- TensorFlow Lite for efficient on-device machine learning
- Android CameraX for camera integration
- Material Design for UI components

---

## Contact

For questions or support, please open an issue on GitHub or contact the maintainers.