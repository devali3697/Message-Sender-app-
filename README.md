# Personal SMS & App Notification Synchronization System

A robust, private, multi-device personal synchronization system built with **Native Android, Kotlin, XML Layouts, Material Design 3, and Firebase**.

---

## 📱 System Architecture

This project consists of two native Android applications operating within a multi-module Gradle project:

1. **MessageSender (`com.personal.messagesender`)**:
   - Installed on **Phone 1 (SIM card device)**.
   - Captures incoming SIM SMS messages (via telephony broadcast receiver) and WhatsApp / Telegram / app notifications (via Notification Listener Service).
   - Synchronizes alerts securely with Firebase in real time.
   - Operates 24/7 in the background via a persistent Foreground Service (survives app swiping/closing from Recent Apps).

2. **MessageAdmin (`com.personal.messageadmin`)**:
   - Installed on **Phone 2 (Wi-Fi admin device)**.
   - Displays real-time alert history with full message text, sender name, and app source badges (WhatsApp, SMS, etc.).
   - **Multi-Device Support**: Filter alerts by specific sender devices (supports 4-5 different sender phones).
   - Supports marking alerts as read, deleting individual alerts, or clearing all history.
   - Receives high-priority FCM push notifications.

---

## ✨ Key Features

- **SIM SMS Fail-Safe**: Captures cellular SMS directly from the telephony hardware layer — works even when the phone is on **DND (Do Not Disturb)** mode or when SMS app notifications are disabled.
- **WhatsApp & App Notifications**: Unpacks `Notification.MessagingStyle` to extract sender names and full message body text.
- **Multi-Device Management**: Manage multiple sender phones simultaneously with horizontal device filter chips in the admin app.
- **24/7 Background Persistence**: Uses Android Foreground Services (`START_STICKY`), WorkManager for offline retry, and BootReceivers for device restart recovery.
- **Guided Onboarding**: Professional step-by-step permission setup flow on first installation.
- **Strict Security & Privacy**: Restrictive Firebase Realtime Database rules (`auth != null`) and exclusion of sensitive credentials.

---

## 🚀 Getting Started & Setup

### 1. Firebase Configuration
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Email/Password** authentication.
3. Create a **Realtime Database** and publish the rules from [`database.rules.json`](database.rules.json).
4. Place your project's `google-services.json` in:
   - `app/google-services.json` (for MessageSender)
   - `messageadmin/google-services.json` (for MessageAdmin)

### 2. Building APKs
Run the following Gradle command to build debug APKs for both apps:
```bash
./gradlew :app:assembleDebug :messageadmin:assembleDebug
```

- **User App APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Admin App APK**: `messageadmin/build/outputs/apk/debug/messageadmin-debug.apk`

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts, Material Design 3, ConstraintLayout, RecyclerView, Material Chips
- **Concurrency**: Kotlin Coroutines & Flow
- **Background Work**: WorkManager, Foreground Service, BroadcastReceiver
- **Backend**: Firebase Authentication, Firebase Realtime Database, Firebase Cloud Messaging (FCM), Cloud Functions
