package com.slcl.mobileprint

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for downloading files from cloud storage providers
 * (e.g., Google Drive) to local cache before uploading
 */
class FileDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "FileDownloader"
        private const val CACHE_DIR_NAME = "mobile_print_uploads"
        private const val BUFFER_SIZE = 8192
    }
    
    /**
     * Check if a URI is from Google Drive
     */
    fun isGoogleDriveUri(uri: Uri): Boolean {
        val authority = uri.authority ?: return false
        return authority == "com.google.android.apps.docs.storage" ||
               authority.contains("com.google.android.apps.docs")
    }
    
    /**
     * Check if a URI is from a cloud storage provider that needs downloading
     */
    fun needsDownload(uri: Uri): Boolean {
        return isGoogleDriveUri(uri)
    }
    
    /**
     * Download a file from a URI to the app's cache directory
     * Returns the local file URI, or null if download fails
     */
    suspend fun downloadToCache(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            // Get filename from content resolver
            val filename = getFileName(uri) ?: "upload_${System.currentTimeMillis()}"
            
            // Create cache directory
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Create cache file
            val cacheFile = File(cacheDir, filename)
            
            // Open input stream from URI
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            Log.d(TAG, "Downloaded file to cache: ${cacheFile.absolutePath}")
            
            // Return file URI
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file from URI: $uri", e)
            null
        }
    }
    
    /**
     * Get the filename from a content URI
     */
    private fun getFileName(uri: Uri): String? {
        var filename: String? = null
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    filename = cursor.getString(nameIndex)
                }
            }
        }
        
        return filename
    }
    
    /**
     * Check if a string is a valid file URL
     */
    fun isValidFileUrl(url: String): Boolean {
        try {
            // Check if it's a valid HTTP/HTTPS URL
            if (!url.startsWith("http://", ignoreCase = true) && 
                !url.startsWith("https://", ignoreCase = true)) {
                return false
            }
            
            // Check if it ends with a supported file extension
            return isSupportedFileType(url)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Check if URL points to a supported file type
     */
    private fun isSupportedFileType(url: String): Boolean {
        val supportedExtensions = listOf(
            ".pdf",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            ".doc", ".docx", ".txt", ".rtf",
            ".xls", ".xlsx", ".csv",
            ".ppt", ".pptx"
        )
        
        val lowerUrl = url.lowercase()
        return supportedExtensions.any { lowerUrl.contains(it) }
    }
    
    /**
     * Download a file from a URL to the app's cache directory
     * Returns the local file URI, or null if download fails
     */
    suspend fun downloadFromUrl(url: String): Uri? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading file from URL: $url")
            
            // Get filename from URL
            val filename = getFilenameFromUrl(url)
            
            // Create cache directory
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Create cache file
            val cacheFile = File(cacheDir, filename)
            
            // Download file
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = 30000 // 30 seconds
            connection.readTimeout = 30000
            
            connection.getInputStream().use { input ->
                FileOutputStream(cacheFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            Log.d(TAG, "Downloaded file to cache: ${cacheFile.absolutePath}")
            
            // Return file URI
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file from URL: $url", e)
            null
        }
    }
    
    /**
     * Extract filename from URL
     */
    private fun getFilenameFromUrl(url: String): String {
        try {
            // Get the last segment of the URL path
            val uri = Uri.parse(url)
            val path = uri.path ?: ""
            val filename = path.substringAfterLast('/')
            
            // If we got a valid filename, use it
            if (filename.isNotEmpty() && filename.contains('.')) {
                // Sanitize filename
                return filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract filename from URL", e)
        }
        
        // Fallback to timestamp-based filename
        return "download_${System.currentTimeMillis()}.pdf"
    }
    
    /**
     * Clear all cached files
     */
    fun clearCache() {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                Log.d(TAG, "Cleared cache directory")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
}
