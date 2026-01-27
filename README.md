# Mobile Print Android App

A native Android application that provides seamless access to the St. Louis County Library mobile print portal.

## Features

- **WebView Integration**: Full access to the mobile print portal website
- **Secure Credentials**: Login credentials stored securely using Android Keystore
- **Auto-Login**: Automatic login with saved credentials
- **Multi-File Upload**: Native Android file picker with support for selecting multiple files
- **Share Intent Support**: Share files from other apps (Files, Google Drive, Photos, etc.) directly to the print portal
- **Supported File Types**:
  - PDF documents
  - Images (JPEG, PNG)
  - Microsoft Office files (Word, Excel, PowerPoint)
  - Text files (TXT, CSV, RTF)

## Requirements

- **Minimum Android Version**: Android 8.0 (API 26)
- **Target Android Version**: Android 14 (API 34)
- **Permissions**:
  - Internet (required)
  - Read External Storage (for Android 8-12)

## Project Structure

```
app/
├── src/main/
│   ├── java/com/slcl/mobileprint/
│   │   ├── MainActivity.kt              # Main activity with WebView and share intent handling
│   │   ├── WebViewManager.kt            # WebView configuration and JavaScript interface
│   │   ├── CredentialManager.kt         # Secure credential storage with Android Keystore
│   │   └── FileUploadHandler.kt         # Multi-file upload handler
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml        # Main layout with WebView
│   │   ├── values/
│   │   │   ├── strings.xml              # App strings
│   │   │   └── themes.xml               # App theme
│   │   └── xml/
│   │       └── file_paths.xml           # FileProvider paths
│   └── AndroidManifest.xml              # App manifest with permissions and intents
└── build.gradle                         # App-level Gradle configuration
```

## Building the App

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to `c:\Users\reedb\Documents\Data\mobile_print`
4. Wait for Gradle sync to complete
5. Click "Run" (green play button) or use `Shift+F10`

### Option 2: Command Line
```bash
cd c:\Users\reedb\Documents\Data\mobile_print
./gradlew assembleDebug
# APK will be in: app/build/outputs/apk/debug/app-debug.apk
```

## Installation

### Install on Connected Device
```bash
./gradlew installDebug
```

### Manual Installation
1. Enable "Unknown Sources" in Android Settings
2. Transfer the APK to your device
3. Open and install the APK

## Usage

### First Time Setup
1. Launch the app
2. Log in with your library card and PIN
3. (Optional) Save credentials for automatic login

### Uploading Files from WebView
1. Open the app (auto-login if credentials saved)
2. Tap the "Upload" button in the web interface
3. Select one or more files using the native Android file picker
4. Files will upload to your print queue

### Sharing Files from Other Apps
1. Open any file manager, Google Drive, or Photos app
2. Select one or more files
3. Tap "Share"
4. Choose "Mobile Print" from the share menu
5. App opens, logs in automatically, and navigates to upload
6. Select files from the file picker to upload

## Security

- **Credential Storage**: All credentials are encrypted using Android Keystore with AES-GCM encryption
- **HTTPS Only**: App only communicates over secure HTTPS connections
- **No Third-Party Tracking**: No analytics or third-party SDKs

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Android Views with WebView
- **Encryption**: Android Keystore System with AES/GCM/NoPadding
- **Web Engine**: Android WebView with JavaScript enabled
- **File Handling**: Native Android file picker with EXTRA_ALLOW_MULTIPLE support

## Troubleshooting

### Login Issues
- Clear app data: Settings > Apps > Mobile Print > Storage > Clear Data
- Re-enter credentials

### File Upload Not Working
- Check internet connection
- Verify file type is supported
- Grant storage permissions in Settings > Apps > Mobile Print > Permissions

### Share Intent Not Appearing
- Verify file type is supported (PDF, images, Office docs)
- Check that app is installed correctly

## License

This app is for personal use with the St. Louis County Library print system.
