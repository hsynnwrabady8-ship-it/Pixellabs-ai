package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

data class AppCheckTokenStatus(
    val isValid: Boolean,
    val token: String?,
    val expireTimeMillis: Long?,
    val errorMessage: String? = null
)

@SuppressLint("ApplySharedPref")
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )

            if (!debugToken.isNullOrEmpty()) {
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret saved synchronously to SharedPreferences.")
            } else {
                val existingSecret = prefs.getString(DEBUG_SECRET_KEY, null)
                if (existingSecret != null) {
                    Log.d(TAG, "Reusing existing persisted App Check debug secret in store.")
                } else {
                    Log.w(TAG, "No debug token found in Intent extra and none in SharedPreferences.")
                }
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release: Look up recaptcha_site_key safely without throwing Resources.NotFoundException
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed with siteKey: ${siteKey.take(8)}***")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

/**
 * Explicitly verifies or forces refresh of the App Check attestation token.
 * Used before executing Firebase AI Logic inference to diagnose whether
 * requests will be blocked due to attestation failures.
 */
suspend fun verifyAndFetchAppCheckToken(forceRefresh: Boolean = false): AppCheckTokenStatus {
    return try {
        val appCheck = FirebaseAppCheck.getInstance()
        val tokenTask = appCheck.getAppCheckToken(forceRefresh)
        val result = withTimeoutOrNull(8_000L) { tokenTask.await() }
        if (result != null && result.token.isNotBlank()) {
            Log.d(TAG, "App Check token verified successfully (tokenLength=${result.token.length}, expiresAt=${result.expireTimeMillis})")
            AppCheckTokenStatus(
                isValid = true,
                token = result.token,
                expireTimeMillis = result.expireTimeMillis
            )
        } else {
            val msg = "App Check returned empty or null token"
            Log.w(TAG, msg)
            AppCheckTokenStatus(
                isValid = false,
                token = null,
                expireTimeMillis = null,
                errorMessage = msg
            )
        }
    } catch (e: Exception) {
        val msg = "${e.javaClass.simpleName}: ${e.message}"
        Log.e(TAG, "App Check token query failed: $msg", e)
        AppCheckTokenStatus(
            isValid = false,
            token = null,
            expireTimeMillis = null,
            errorMessage = msg
        )
    }
}
