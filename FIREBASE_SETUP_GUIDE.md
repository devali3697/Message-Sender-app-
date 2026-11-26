# Personal SMS Alert Synchronization System

This system consists of two native Android applications built with Kotlin, XML Layouts, Material Design 3, and Firebase:

1. **MessageSender (`com.personal.messagesender`)**: Installed on Phone 1 (Device with active SIM card). Captures incoming SMS arrival events and synchronizes non-sensitive metadata (sender phone number & arrival timestamp) with Firebase.
2. **MessageAdmin (`com.personal.messageadmin`)**: Installed on Phone 2 (Primary Wi-Fi device). Displays real-time SMS arrival history, sender online/offline status, read/unread states, and receives FCM push notifications.

---

## 🔒 Privacy & Security Architecture

- **Strict Non-Sensitive Data Sync**: Only sender phone number and arrival timestamp are transmitted.
- **Message Body Excluded**: SMS content, OTPs, 2FA codes, passwords, and banking text are **never** read or uploaded.
- **Restricted Database Rules**: Only authenticated owner sessions (`auth != null`) can read or write.
- **No Public Registration**: Pre-authorized owner login sessions only.

---

## 🛠️ Step-by-Step Setup Guide

### 1. Firebase Project Setup
1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project (e.g. `sms-alert-sync`).
2. **Authentication**: Enable **Email/Password** sign-in method under Firebase Authentication -> Sign-in method.
   - Create an owner account (e.g. `owner@myphone.com` with a strong password).
3. **Realtime Database**: Create a Realtime Database instance.
   - Go to **Rules** tab and paste the contents of `database.rules.json` included in this repository.
4. **Android App Registration**:
   - Add App 1: Package Name `com.personal.messagesender`
     - Download `google-services.json` and place it in the `app/` directory (`C:/Users/Dell/AndroidStudioProjects/MessageSender/app/google-services.json`).
   - Add App 2: Package Name `com.personal.messageadmin`
     - Download `google-services.json` and place it in the `messageadmin/` directory (`C:/Users/Dell/AndroidStudioProjects/MessageSender/messageadmin/google-services.json`).

---

## 🚀 Building & Installing APKs

### Command Line / Terminal:
- Build Debug APKs for both applications:
  ```bash
  ./gradlew :app:assembleDebug :messageadmin:assembleDebug
  ```

### Generated APK Locations:
- **MessageSender APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **MessageAdmin APK**: `messageadmin/build/outputs/apk/debug/messageadmin-debug.apk`

---

## 📱 Physical Device Testing Instructions

### Step 1: Phone 1 (Sender Device with SIM)
1. Install `app-debug.apk` on Phone 1.
2. Launch **MessageSender**.
3. Tap **Firebase Owner Sign In** and enter your pre-authorized owner email & password.
4. Tap **Grant SMS Permissions** and accept runtime `RECEIVE_SMS` & `READ_SMS` permissions.
5. Toggle **Enable SMS Arrival Sync** to `ON`.
6. Tap **Send Test SMS Alert Event** to verify Firebase Realtime Database connection.

### Step 2: Phone 2 (Receiver Device with Wi-Fi)
1. Install `messageadmin-debug.apk` on Phone 2.
2. Launch **MessageAdmin**.
3. Tap **Sign In** and log in with the same owner account.
4. Grant Notification permissions when prompted.
5. Observe:
   - Sender Device status (ONLINE / OFFLINE badge & last seen time).
   - Real-time SMS alert arrival history list.
   - Tap an alert to mark it as READ.
   - Pull down to refresh data at any time.

---

## ⚡ Background Functionality & Compliance

- **WorkManager Sync Retry**: When internet connection is temporarily lost, unsynced alerts are saved to local persistent storage and retried automatically via `PendingSyncWorker` as soon as connectivity resumes.
- **Device Reboot Resilience**: BootReceiver detects `ACTION_BOOT_COMPLETED` and restores sync service configuration.
- **Android System Compliance**: Fully compliant with modern Android background execution rules (Android 10 - 15, API 26-35).

### System Limitations:
- **Power Off**: If Phone 1 is powered off, hardware cannot receive cellular SMS.
- **Force Stop**: If Phone 1's user force-stops the app via System Settings, Android halts broadcast receivers until manual launch.
- **Revoked Permissions**: Revoking SMS permissions stops broadcast reception. The UI displays warning banners if permissions are missing.
