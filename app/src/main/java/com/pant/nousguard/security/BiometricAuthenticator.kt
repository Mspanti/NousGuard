
package pant.com.nousguard.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity // <-- Use FragmentActivity for broader compatibility
import java.util.concurrent.Executor

/**
 * A utility class to handle biometric authentication using AndroidX BiometricPrompt.
 * It provides methods to check authentication capability and initiate authentication.
 */
class BiometricAuthenticator(
    private val context: Context,
    private val executor: Executor,
    private val onAuthSuccess: () -> Unit,
    private val onAuthError: (errorCode: Int, errString: CharSequence) -> Unit,
    private val onAuthFailed: () -> Unit,
    private val onBiometricNotAvailable: () -> Unit
) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric authentication is available and set up on the device.
     * It checks for BIOMETRIC_WEAK (e.g., fingerprint, face) or DEVICE_CREDENTIAL (PIN/Pattern/Password).
     * @return True if authentication is possible, false otherwise.
     */
    fun canAuthenticate(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val result = biometricManager.canAuthenticate(authenticators)
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Log these specific errors for debugging if needed
                // Log.e("BiometricAuth", "Biometric not available: $result")
                false
            }
            else -> {
                // Other errors like BIOMETRIC_ERROR_SECURITY_UPDATE, etc.
                false
            }
        }
    }

    /**
     * Initiates the biometric authentication flow.
     * If biometrics are not available or set up, it calls onAuthError.
     * @param activity The FragmentActivity context (e.g., your MainActivity instance).
     * @param title The title displayed on the biometric prompt.
     * @param subtitle The subtitle displayed on the biometric prompt.
     * @param description The description displayed on the biometric prompt.
     */
    fun authenticate(activity: FragmentActivity, title: String, subtitle: String, description: String) {
        if (!canAuthenticate()) {
            // If authentication is not possible, call the new callback
            onBiometricNotAvailable()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or // Allows fingerprint, face, etc.
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL // Allows fallback to PIN/Pattern/Password
            )
            .setConfirmationRequired(true) // User must explicitly confirm after successful scan
            .build()

        val biometricPrompt = BiometricPrompt(
            activity, // FragmentActivity instance
            executor, // Executor for callbacks
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onAuthError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthFailed()
                }
            }
        )
        biometricPrompt.authenticate(promptInfo)
    }
}