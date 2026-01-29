package com.uploadhelper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles file upload requests from the WebView
 */
class FileUploadHandler(private val activity: Activity) {
    
    companion object {
        const val FILE_CHOOSER_REQUEST_CODE = 1001
    }
    
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileQueue: MutableList<Uri> = mutableListOf()
    private var currentFileIndex: Int = 0
    private val fileDownloader = FileDownloader(activity)
    var onUploadProgressCallback: ((current: Int, total: Int) -> Unit)? = null
    var onDownloadProgressCallback: ((message: String) -> Unit)? = null
    
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
            
            // If we have files in the queue, upload the current one
            if (fileQueue.isNotEmpty() && currentFileIndex < fileQueue.size) {
                val currentFile = fileQueue[currentFileIndex]
                filePathCallback?.onReceiveValue(arrayOf(currentFile))
                this@FileUploadHandler.filePathCallback = null
                
                // Move to next file
                currentFileIndex++
                
                // Notify progress
                onUploadProgressCallback?.invoke(currentFileIndex, fileQueue.size)
                
                // If more files remain, we'll handle them on the next upload trigger
                return true
            }
            
            // Otherwise, show the file picker
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
     * Downloads Google Drive files to cache before queueing
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) {
            return
        }
        
        if (resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                val uris = parseResult(intent)
                if (uris != null) {
                    // Process files asynchronously (download Google Drive files if needed)
                    CoroutineScope(Dispatchers.Main).launch {
                        val processedUris = processFiles(uris)
                        
                        if (processedUris.isNotEmpty()) {
                            // Queue files for sequential upload
                            queueSharedFiles(processedUris)
                            
                            // Notify that files are ready
                            withContext(Dispatchers.Main) {
                                onDownloadProgressCallback?.invoke(
                                    if (processedUris.size == 1) {
                                        "File ready! Tap Upload to continue"
                                    } else {
                                        "${processedUris.size} files ready! Tap Upload to upload file 1 of ${processedUris.size}"
                                    }
                                )
                            }
                        }
                        
                        // Return null to close the file picker without uploading yet
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                    }
                    return
                }
            }
        }
        
        // If we get here, user cancelled or error occurred
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
    }
    
    /**
     * Process files: download Google Drive files to cache
     */
    private suspend fun processFiles(uris: Array<Uri>): List<Uri> = withContext(Dispatchers.IO) {
        val processedUris = mutableListOf<Uri>()
        
        for ((index, uri) in uris.withIndex()) {
            if (fileDownloader.needsDownload(uri)) {
                // Notify user we're downloading
                withContext(Dispatchers.Main) {
                    onDownloadProgressCallback?.invoke(
                        "Downloading file ${index + 1} of ${uris.size} from Google Drive..."
                    )
                }
                
                // Download to cache
                val cachedUri = fileDownloader.downloadToCache(uri)
                if (cachedUri != null) {
                    processedUris.add(cachedUri)
                } else {
                    // Download failed, notify user
                    withContext(Dispatchers.Main) {
                        onDownloadProgressCallback?.invoke(
                            "Failed to download file ${index + 1}"
                        )
                    }
                }
            } else {
                // Local file, use as-is
                processedUris.add(uri)
            }
        }
        
        processedUris
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
     * Queue shared files for sequential upload
     * These will be uploaded one at a time when the user taps the upload button
     */
    fun queueSharedFiles(uris: List<Uri>) {
        fileQueue.clear()
        fileQueue.addAll(uris)
        currentFileIndex = 0
    }
    
    /**
     * Check if there are pending shared files
     */
    fun hasPendingSharedFiles(): Boolean {
        return currentFileIndex < fileQueue.size
    }
    
    /**
     * Get the current upload progress
     */
    fun getUploadProgress(): Pair<Int, Int> {
        return Pair(currentFileIndex, fileQueue.size)
    }
    
    /**
     * Clear pending shared files
     */
    fun clearPendingSharedFiles() {
        fileQueue.clear()
        currentFileIndex = 0
    }
    
    /**
     * Clear downloaded cache files
     */
    fun clearCache() {
        fileDownloader.clearCache()
    }
}
