package com.slcl.mobileprint

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Manages WebView configuration and behavior
 */
class WebViewManager(
    private val webView: WebView,
    private val fileUploadHandler: FileUploadHandler
) {
    
    companion object {
        const val PRINT_PORTAL_URL = "https://mobileprint.slcl.org/myprintcenter/"
    }
    
    private var onPageLoadedCallback: (() -> Unit)? = null
    
    init {
        setupWebView()
    }
    
    private fun setupWebView() {
        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // Enable zoom controls but hide the zoom buttons
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            
            // Enable caching
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Enable cookies
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            
            // Improve rendering
            loadWithOverviewMode = true
            useWideViewPort = true
            
            // Enable safe browsing
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        
        // Set custom WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPageLoadedCallback?.invoke()
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Allow all navigation within the print portal
                return false
            }
        }
        
        // Set custom WebChromeClient for file uploads
        webView.webChromeClient = fileUploadHandler.CustomWebChromeClient()
    }
    
    /**
     * Load the print portal URL
     */
    fun loadPrintPortal() {
        webView.loadUrl(PRINT_PORTAL_URL)
    }
    
    /**
     * Set a callback to be invoked when a page finishes loading
     */
    fun setOnPageLoadedCallback(callback: () -> Unit) {
        onPageLoadedCallback = callback
    }
    
    /**
     * Check if we can go back in the WebView history
     */
    fun canGoBack(): Boolean = webView.canGoBack()
    
    /**
     * Go back in the WebView history
     */
    fun goBack() {
        webView.goBack()
    }
    
    /**
     * Execute JavaScript in the WebView
     */
    fun executeJavaScript(script: String, callback: ((String) -> Unit)? = null) {
        webView.evaluateJavascript(script, callback)
    }
    
    /**
     * Clear all cookies and cache
     */
    fun clearSession() {
        CookieManager.getInstance().removeAllCookies(null)
        webView.clearCache(true)
        webView.clearHistory()
    }
    
    /**
     * Inject files into the upload form (for share intent)
     */
    fun triggerFileUpload() {
        val javascript = """
            (function() {
                var uploadButton = document.querySelector('.button-toolbar-upload');
                if (uploadButton) {
                    uploadButton.click();
                    return true;
                }
                return false;
            })();
        """.trimIndent()
        
        executeJavaScript(javascript)
    }
}
