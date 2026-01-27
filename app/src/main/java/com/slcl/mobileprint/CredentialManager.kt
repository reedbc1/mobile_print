package com.slcl.mobileprint

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.WebView
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages secure storage and retrieval of user credentials using Android Keystore
 */
class CredentialManager(private val context: Context) {
    
    companion object {
        private const val KEY_ALIAS = "MobilePrintCredentials"
        private const val PREFS_NAME = "MobilePrintPrefs"
        private const val PREF_LIBRARY_CARD = "library_card"
        private const val PREF_PIN = "pin"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = "]"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }
    
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    
    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    private fun decrypt(encryptedData: String): String? {
        return try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV (first 12 bytes for GCM)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save credentials securely
     */
    fun saveCredentials(libraryCard: String, pin: String) {
        val encryptedCard = encrypt(libraryCard)
        val encryptedPin = encrypt(pin)
        
        prefs.edit()
            .putString(PREF_LIBRARY_CARD, encryptedCard)
            .putString(PREF_PIN, encryptedPin)
            .apply()
    }
    
    /**
     * Retrieve stored credentials
     * @return Pair of (libraryCard, pin) or null if not found
     */
    fun getCredentials(): Pair<String, String>? {
        val encryptedCard = prefs.getString(PREF_LIBRARY_CARD, null) ?: return null
        val encryptedPin = prefs.getString(PREF_PIN, null) ?: return null
        
        val libraryCard = decrypt(encryptedCard) ?: return null
        val pin = decrypt(encryptedPin) ?: return null
        
        return Pair(libraryCard, pin)
    }
    
    /**
     * Check if credentials are stored
     */
    fun hasCredentials(): Boolean {
        return prefs.contains(PREF_LIBRARY_CARD) && prefs.contains(PREF_PIN)
    }
    
    /**
     * Clear stored credentials
     */
    fun clearCredentials() {
        prefs.edit()
            .remove(PREF_LIBRARY_CARD)
            .remove(PREF_PIN)
            .apply()
    }
    
    /**
     * Auto-fill the login form and submit
     */
    fun autoFillLogin(webView: WebView, libraryCard: String, pin: String) {
        val javascript = """
            (function() {
                var usernameField = document.getElementById('input-login-username');
                var passwordField = document.getElementById('input-login-password');
                
                if (usernameField && passwordField) {
                    usernameField.value = '$libraryCard';
                    passwordField.value = '$pin';
                    
                    // Find and click the login button
                    var loginButton = document.querySelector('button[type="submit"], input[type="submit"], .button-login');
                    if (loginButton) {
                        loginButton.click();
                        return true;
                    }
                }
                return false;
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(javascript, null)
    }
}
