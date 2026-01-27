# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep credential manager encryption classes
-keep class com.slcl.mobileprint.CredentialManager { *; }

# Keep WebView related classes
-keepclassmembers class android.webkit.WebView {
   public *;
}

# Keep file upload handler
-keep class com.slcl.mobileprint.FileUploadHandler { *; }
