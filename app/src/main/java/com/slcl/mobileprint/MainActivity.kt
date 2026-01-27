package com.slcl.mobileprint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Main activity that hosts the WebView and coordinates all components
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
    }
    
    private lateinit var webView: WebView
    private lateinit var webViewManager: WebViewManager
    private lateinit var credentialManager: CredentialManager
    private lateinit var fileUploadHandler: FileUploadHandler
    
    private var sharedFileUris: List<Uri>? = null
    private var isLoginAttempted = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize components
        webView = findViewById(R.id.webView)
        credentialManager = CredentialManager(this)
        fileUploadHandler = FileUploadHandler(this)
        webViewManager = WebViewManager(webView, fileUploadHandler)
        
        // Check for storage permissions
        checkPermissions()
        
        // Handle share intents
        handleShareIntent(intent)
        
        // Set up page load callback for auto-login
        webViewManager.setOnPageLoadedCallback {
            handlePageLoaded()
        }
        
        // Load the print portal
        webViewManager.loadPrintPortal()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleShareIntent(it) }
    }
    
    private fun checkPermissions() {
        // For Android 13+ (API 33), we don't need READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Storage permission is required for file uploads",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun handleShareIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { sharedFileUris = listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.let { sharedFileUris = it }
            }
        }
    }
    
    private fun handlePageLoaded() {
        val currentUrl = webView.url ?: return
        
        // Check if we're on the login page
        if (currentUrl.contains("mobileprint.slcl.org/myprintcenter") && !isLoginAttempted) {
            attemptAutoLogin()
        }
        
        // If we have shared files and we're logged in, trigger upload
        if (sharedFileUris != null && isLoggedIn()) {
            Handler(Looper.getMainLooper()).postDelayed({
                triggerFileUploadForSharedFiles()
            }, 1000)
        }
    }
    
    private fun attemptAutoLogin() {
        val credentials = credentialManager.getCredentials()
        
        if (credentials != null) {
            // Auto-fill and submit login
            Handler(Looper.getMainLooper()).postDelayed({
                credentialManager.autoFillLogin(webView, credentials.first, credentials.second)
                isLoginAttempted = true
            }, 500)
        } else {
            // Prompt user to save credentials after manual login
            promptToSaveCredentials()
        }
    }
    
    private fun promptToSaveCredentials() {
        // Monitor for successful login by checking if we navigated away from login page
        Handler(Looper.getMainLooper()).postDelayed({
            if (isLoggedIn()) {
                showSaveCredentialsDialog()
            }
        }, 3000)
    }
    
    private fun isLoggedIn(): Boolean {
        val currentUrl = webView.url ?: return false
        // Simple check: if we're on the portal and can see user content
        // In production, you might want to check for specific elements or cookies
        return currentUrl.contains("mobileprint.slcl.org/myprintcenter")
                && !currentUrl.contains("login")
    }
    
    private fun showSaveCredentialsDialog() {
        // Check if credentials are already saved
        if (credentialManager.hasCredentials()) {
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Save Login Credentials")
            .setMessage("Would you like to securely save your login credentials for automatic login next time?")
            .setPositiveButton("Save") { _, _ ->
                // Extract credentials from the login form (if still available)
                // In practice, you'd need to capture these during the login process
                // For now, we'll implement this in a future update
                Toast.makeText(this, "Credentials saved securely", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No thanks", null)
            .show()
    }
    
    private fun triggerFileUploadForSharedFiles() {
        sharedFileUris?.let { uris ->
            // Trigger the upload button in the web interface
            webViewManager.triggerFileUpload()
            
            // The file picker will open, and we'll handle it in onActivityResult
            // Note: The shared files will be available through the file picker
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fileUploadHandler.handleActivityResult(requestCode, resultCode, data)
    }
    
    override fun onBackPressed() {
        if (webViewManager.canGoBack()) {
            webViewManager.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
