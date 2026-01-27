package com.slcl.mobileprint

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Handles file upload requests from the WebView
 */
class FileUploadHandler(private val activity: Activity) {
    
    companion object {
        const val FILE_CHOOSER_REQUEST_CODE = 1001
    }
    
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    
    /**
     * Custom WebChromeClient that intercepts file chooser requests
     */
    inner class CustomWebChromeClient : WebChromeClient() {
        
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            this@FileUploadHandler.filePathCallback?.onReceiveValue(null)
            this@FileUploadHandler.filePathCallback = filePathCallback
            
            val intent = createFileChooserIntent()
            activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            
            return true
        }
    }
    
    /**
     * Create an intent for selecting multiple files
     */
    private fun createFileChooserIntent(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "image/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "text/csv",
                "application/rtf"
            ))
        }
        
        return Intent.createChooser(intent, "Select files to upload")
    }
    
    /**
     * Handle the result from the file chooser
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) {
            return
        }
        
        val results = if (resultCode == Activity.RESULT_OK) {
            data?.let { parseResult(it) }
        } else {
            null
        }
        
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }
    
    /**
     * Parse the selected files from the intent
     */
    private fun parseResult(data: Intent): Array<Uri>? {
        val clipData = data.clipData
        
        return if (clipData != null) {
            // Multiple files selected
            Array(clipData.itemCount) { i ->
                clipData.getItemAt(i).uri
            }
        } else {
            // Single file selected
            data.data?.let { arrayOf(it) }
        }
    }
    
    /**
     * Upload files directly to the share intent queue
     */
    fun queueSharedFiles(uris: List<Uri>) {
        // Store the URIs for upload after WebView is ready
        // This will be handled by MainActivity
    }
}
