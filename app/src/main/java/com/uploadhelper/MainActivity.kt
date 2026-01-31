package com.uploadhelper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main activity that hosts the WebView and coordinates all components
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
        private const val PREFS_NAME = "UploadHelperPrefs"
        private const val PREF_LAST_URL = "lastUrl"
        private const val PREF_FIRST_LAUNCH = "firstLaunch"
    }
    
    private lateinit var webView: WebView
    private lateinit var webViewManager: WebViewManager
    private lateinit var fileUploadHandler: FileUploadHandler
    private lateinit var urlInput: EditText
    private lateinit var backButton: ImageButton
    private lateinit var refreshButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var prefs: SharedPreferences
    
    private var sharedFileUris: List<Uri>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize components
        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        backButton = findViewById(R.id.backButton)
        refreshButton = findViewById(R.id.refreshButton)
        progressBar = findViewById(R.id.progressBar)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        fileUploadHandler = FileUploadHandler(this)
        webViewManager = WebViewManager(webView, fileUploadHandler)
        
        // Check for storage permissions
        checkPermissions()
        
        // Handle share intents
        handleShareIntent(intent)
        
        // Set up navigation buttons
        setupNavigationButtons()
        
        // Set up page load callback
        webViewManager.setOnPageLoadedCallback {
            handlePageLoaded()
        }
        
        // Set up URL change callback
        webViewManager.setOnUrlChangedCallback { url ->
            updateAddressBar(url)
        }
        
        // Set up progress callback
        webViewManager.setOnProgressChangedCallback { progress ->
            updateProgress(progress)
        }
        
        // Set up upload progress callback
        fileUploadHandler.onUploadProgressCallback = { current, total ->
            runOnUiThread {
                if (current < total) {
                    Toast.makeText(
                        this,
                        "File $current of $total uploaded. Tap Upload for next file.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "All $total files uploaded successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        
        // Set up download progress callback
        fileUploadHandler.onDownloadProgressCallback = { message ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        // Load initial page (homepage or last URL or shared intent)
        loadInitialPage()
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
                // Check if it's a URL (text/plain)
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        handleSharedUrl(sharedText)
                    }
                } else {
                    // Handle file URI
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    uri?.let { sharedFileUris = listOf(it) }
                }
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
    
    private fun handleSharedUrl(url: String) {
        val fileDownloader = FileDownloader(this)
        
        if (fileDownloader.isValidFileUrl(url)) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    this@MainActivity,
                    "Downloading file from URL...",
                    Toast.LENGTH_SHORT
                ).show()
                
                val cachedUri = fileDownloader.downloadFromUrl(url)
                if (cachedUri != null) {
                    sharedFileUris = listOf(cachedUri)
                    Toast.makeText(
                        this@MainActivity,
                        "File downloaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Trigger upload for shared files
                    triggerFileUploadForSharedFiles()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to download file from URL",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this,
                "URL does not point to a supported file type",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun handlePageLoaded() {
        // If we have shared files, trigger upload
        if (sharedFileUris != null) {
            // Wait a bit longer to ensure page is fully loaded and interactive
            Handler(Looper.getMainLooper()).postDelayed({
                triggerFileUploadForSharedFiles()
            }, 2000)
        }
    }
    
    private fun setupNavigationButtons() {
        // Back button
        backButton.setOnClickListener {
            if (webViewManager.canGoBack()) {
                webViewManager.goBack()
            }
        }
        
        // Refresh button
        refreshButton.setOnClickListener {
            webView.reload()
        }
        
        // URL input - handle enter/go key
        urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || 
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val url = urlInput.text.toString()
                loadUrl(url)
                true
            } else {
                false
            }
        }
        
        // URL input - select all text when tapped
        urlInput.setSelectAllOnFocus(true)
        urlInput.setOnClickListener {
            urlInput.selectAll()
        }
    }
    
    private fun loadInitialPage() {
        // Priority: 1. Share intent, 2. Last URL, 3. Default homepage
        if (sharedFileUris != null) {
            // If opened via share intent, load last URL or homepage
            val lastUrl = prefs.getString(PREF_LAST_URL, null)
            loadUrl(lastUrl ?: getString(R.string.default_homepage))
        } else {
            val isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true)
            if (isFirstLaunch) {
                // First launch - go to homepage
                prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply()
                loadUrl(getString(R.string.default_homepage))
            } else {
                // Restore last URL or use homepage
                val lastUrl = prefs.getString(PREF_LAST_URL, null)
                loadUrl(lastUrl ?: getString(R.string.default_homepage))
            }
        }
    }
    
    private fun loadUrl(url: String) {
        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else if (url.contains(".") && !url.contains(" ")) {
            // Looks like a domain, add https://
            "https://$url"
        } else {
            // Treat as search query
            "https://www.google.com/search?q=${Uri.encode(url)}"
        }
        webViewManager.loadUrl(finalUrl)
    }
    
    private fun updateAddressBar(url: String) {
        urlInput.setText(url)
        // Save last URL
        prefs.edit().putString(PREF_LAST_URL, url).apply()
        // Update button state
        backButton.isEnabled = webViewManager.canGoBack()
    }
    
    private fun updateProgress(progress: Int) {
        if (progress < 100) {
            progressBar.visibility = android.view.View.VISIBLE
            progressBar.progress = progress
        } else {
            progressBar.visibility = android.view.View.GONE
        }
    }
    
    private fun triggerFileUploadForSharedFiles() {
        sharedFileUris?.let { uris ->
            // Check if any files are from Google Drive and need downloading
            val fileDownloader = FileDownloader(this)
            val needsDownload = uris.any { fileDownloader.needsDownload(it) }
            
            if (needsDownload) {
                // Process files asynchronously (download Google Drive files)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        this@MainActivity,
                        "Processing files from Google Drive...",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    val processedUris = mutableListOf<Uri>()
                    for ((index, uri) in uris.withIndex()) {
                        if (fileDownloader.needsDownload(uri)) {
                            Toast.makeText(
                                this@MainActivity,
                                "Downloading file ${index + 1} of ${uris.size} from Google Drive...",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            val cachedUri = fileDownloader.downloadToCache(uri)
                            if (cachedUri != null) {
                                processedUris.add(cachedUri)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to download file ${index + 1}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            processedUris.add(uri)
                        }
                    }
                    
                    // Queue the processed files
                    queueProcessedFiles(processedUris)
                }
            } else {
                // No Google Drive files, queue directly
                queueProcessedFiles(uris)
            }
            
            // Clear the shared files after processing starts
            sharedFileUris = null
        }
    }
    
    private fun queueProcessedFiles(uris: List<Uri>) {
        if (uris.isEmpty()) {
            Toast.makeText(
                this,
                "No files to upload",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Queue the shared files for sequential upload
        fileUploadHandler.queueSharedFiles(uris)
        
        val fileCount = uris.size
        // Show instruction to user
        Toast.makeText(
            this,
            if (fileCount == 1) {
                "File ready! Tap the Upload button to continue"
            } else {
                "$fileCount files ready! Tap Upload to upload file 1 of $fileCount"
            },
            Toast.LENGTH_LONG
        ).show()
        
        // Note: We cannot programmatically trigger file upload due to browser security.
        // File inputs require a real user gesture (tap/click) to open.
        // The files are queued and will upload one at a time when user taps the upload button.
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
        // Flush cookies to ensure login session persists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.CookieManager.getInstance().flush()
        }
        // Clear cached downloaded files
        fileUploadHandler.clearCache()
        webView.destroy()
    }
}
