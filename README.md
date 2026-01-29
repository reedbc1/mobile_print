# Upload Helper - Android Browser App

A native Android web browser application with enhanced file upload capabilities and share intent support.

## Features

- **Web Browsing**: Full-featured web browser with address bar and navigation controls
- **Address Bar Navigation**: Enter URLs or search terms directly
- **Session Restoration**: Automatically restores your last visited page when reopening the app
- **Default Homepage**: Opens to Google on first launch
- **Persistent Login**: Maintains login sessions across app restarts using cookie persistence
- **Multi-File Upload**: Native Android file picker with support for selecting multiple files
- **Share Intent Support**: Share files from other apps (Files, Google Drive, Photos, etc.) directly to any website
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
│   ├── java/com/uploadhelper/
│   │   ├── MainActivity.kt              # Main activity with browser UI and share intent handling
│   │   ├── WebViewManager.kt            # WebView configuration with cookie persistence
│   │   ├── FileUploadHandler.kt         # Multi-file upload handler
│   │   └── FileDownloader.kt            # Google Drive and URL file downloader
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml        # Main layout with address bar and WebView
│   │   ├── values/
│   │   │   ├── strings.xml              # App strings
│   │   │   └── themes.xml               # App theme
│   │   └── xml/
│   │       └── file_paths.xml           # FileProvider paths
│   └── AndroidManifest.xml              # App manifest with permissions and intents
└── build.gradle                         # App-level Gradle configuration
```

## Building the App

### Prerequisites
- **Android Studio**: Hedgehog (2023.1.1) or newer recommended
- **JDK**: Version 17 or newer
- **Gradle**: 8.2 (included via wrapper)

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to `c:\\Users\\reedb\\Documents\\Data\\mobile_print`
4. Wait for Gradle sync to complete (this may take a few minutes on first run)
5. If prompted about Gradle configuration, click "Sync Now"
6. Click "Run" (green play button) or use `Shift+F10`

### Option 2: Command Line
```bash
cd c:\\Users\\reedb\\Documents\\Data\\mobile_print
.\\gradlew assembleDebug
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

### Browsing the Web
1. Launch the app (opens to Google homepage on first launch)
2. Enter a URL or search term in the address bar
3. Use navigation buttons:
   - **Back**: Navigate to previous page
   - **Forward**: Navigate to next page
   - **Refresh**: Reload current page
   - **Home**: Return to Google homepage

### Uploading Files from WebView
1. Navigate to any website with file upload functionality
2. Tap the website's upload button
3. Select one or more files using the native Android file picker
4. Files will upload to the website

### Sharing Files from Other Apps
1. Open any file manager, Google Drive, or Photos app
2. Select one or more files
3. Tap "Share"
4. Choose "Upload Helper" from the share menu
5. App opens to your last visited page (or homepage)
6. Navigate to the website where you want to upload
7. Tap the website's upload button
8. Files will be automatically provided to the upload form

## Security

- **Cookie-Based Sessions**: Login sessions persist using secure HTTP-only cookies
- **HTTPS Preferred**: App supports both HTTP and HTTPS connections
- **No Third-Party Tracking**: No analytics or third-party SDKs
- **Sandboxed Storage**: WebView data is isolated to the app's private storage

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Android Views with WebView
- **Session Management**: Cookie-based persistence with automatic flush
- **Web Engine**: Android WebView with JavaScript enabled
- **File Handling**: Native Android file picker with EXTRA_ALLOW_MULTIPLE support
- **State Persistence**: SharedPreferences for session restoration

## Troubleshooting

### Gradle Build Issues
- **Error: "org.gradle.api.artifacts.Dependency"**: Sync project with Gradle files in Android Studio (File > Sync Project with Gradle Files)
- **Missing Gradle wrapper**: The wrapper files have been created. If issues persist, run `gradle wrapper` in the project directory

### Session Restoration Not Working
- Clear app data to reset: Settings > Apps > Upload Helper > Storage > Clear Data
- The app will return to the default homepage (Google) after clearing data

### File Upload Not Working
- Check internet connection
- Verify file type is supported
- Grant storage permissions in Settings > Apps > Upload Helper > Permissions

### Share Intent Not Appearing
- Verify file type is supported (PDF, images, Office docs)
- Check that app is installed correctly

## License

This app is for personal use.
