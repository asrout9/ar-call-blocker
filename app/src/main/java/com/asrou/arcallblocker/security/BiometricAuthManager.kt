package com.asrou.arcallblocker.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.asrou.arcallblocker.core.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles device authentication via Biometrics (Fingerprint / Face)
 * or device fallback credentials (PIN, Pattern, Password).
 *
 * Uses AndroidX BiometricPrompt with [BIOMETRIC_STRONG] or [DEVICE_CREDENTIAL].
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class AuthCapability {
        AVAILABLE,
        NO_HARDWARE,
        NOT_ENROLLED,
        UNAVAILABLE
    }

    /**
     * Check if the device has biometric or device PIN/Pattern/Password set up.
     */
    fun checkCapability(): AuthCapability {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> AuthCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthCapability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> AuthCapability.NO_HARDWARE
            else -> AuthCapability.UNAVAILABLE
        }
    }

    /**
     * Launch the system biometric / device credential prompt.
     *
     * @param activity The host [FragmentActivity].
     * @param title Prompt title.
     * @param subtitle Optional prompt subtitle.
     * @param onSuccess Callback when authentication succeeds.
     * @param onError Callback on authentication error or user cancel.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock AR Call Blocker",
        subtitle: String = "Verify your identity using Biometrics or Device PIN",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                AppLogger.d("Biometric/Credential authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                AppLogger.d("Biometric/Credential authentication error ($errorCode): $errString")
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                AppLogger.d("Biometric/Credential authentication failed attempt")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
