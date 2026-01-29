package com.uploadhelper

import android.net.Uri
import android.os.Build
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
    
    private var onPageLoadedCallback: (() -> Unit)? = null
    private var onUrlChangedCallback: ((String) -> Unit)? = null
    private var onProgressChangedCallback: ((Int) -> Unit)? = null
    
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
            
            // Ensure cookies persist across app sessions
            // This allows the website's "keep me logged in" feature to work
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush()
            }
            
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
                url?.let { onUrlChangedCallback?.invoke(it) }
                onPageLoadedCallback?.invoke()
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Allow all navigation
                return false
            }
        }
        
        // Set custom WebChromeClient for file uploads and progress
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return fileUploadHandler.CustomWebChromeClient()
                    .onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }
            
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChangedCallback?.invoke(newProgress)
            }
        }
    }
    
    /**
     * Load a URL in the WebView
     */
    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }
    
    /**
     * Set a callback to be invoked when a page finishes loading
     */
    fun setOnPageLoadedCallback(callback: () -> Unit) {
        onPageLoadedCallback = callback
    }
    
    /**
     * Set a callback to be invoked when the URL changes
     */
    fun setOnUrlChangedCallback(callback: (String) -> Unit) {
        onUrlChangedCallback = callback
    }
    
    /**
     * Set a callback to be invoked when page load progress changes
     */
    fun setOnProgressChangedCallback(callback: (Int) -> Unit) {
        onProgressChangedCallback = callback
    }
    
    /**
     * Check if we can go back in the WebView history
     */
    fun canGoBack(): Boolean = webView.canGoBack()
    
    /**
     * Check if we can go forward in the WebView history
     */
    fun canGoForward(): Boolean = webView.canGoForward()
    
    /**
     * Go back in the WebView history
     */
    fun goBack() {
        webView.goBack()
    }
    
    /**
     * Go forward in the WebView history
     */
    fun goForward() {
        webView.goForward()
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
}
