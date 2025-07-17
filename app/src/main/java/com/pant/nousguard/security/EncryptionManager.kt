package pant.com.nousguard.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionManager(private val context: Context) {

    private val KEY_ALIAS = "nousguard_encryption_key"
    private val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC // Cipher Block Chaining
    private val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7 // PKCS7 Padding
    private val TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$PADDING"

    private lateinit var keyStore: KeyStore

    init {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null) // Load the keystore
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to load KeyStore: ${e.message}", e)
        }
    }

    /**
     * Retrieves the encryption key from Android Keystore. If the key does not exist,
     * it generates a new one and stores it securely.
     */
    fun getOrCreateSecretKey(): SecretKey {
        // Check if the key already exists
        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey != null && existingKey is SecretKey) {
            Log.d("EncryptionManager", "Existing key retrieved from Keystore.")
            return existingKey
        }

        // If key doesn't exist, generate a new one
        return generateNewSecretKey()
    }

    private fun generateNewSecretKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(false) // For simplicity, no auth needed for key access. Consider `true` for higher security.
                .setRandomizedEncryptionRequired(true) // Ensures IV is random for each encryption
                .build()

            keyGenerator.init(keyGenParameterSpec)
            val newKey = keyGenerator.generateKey()
            Log.d("EncryptionManager", "New key generated and stored in Keystore.")
            return newKey
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Failed to generate or store key: ${e.message}", e)
            throw RuntimeException("Failed to generate or store encryption key", e)
        }
    }

    /**
     * Encrypts the plaintext string using AES-256.
     * Returns a Pair of (encrypted data, IV) in Base64 encoded strings.
     */
    fun encrypt(plaintext: String, secretKey: SecretKey): Pair<String, String> {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // Get the Initialization Vector
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val encryptedText = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            val ivString = Base64.encodeToString(iv, Base64.DEFAULT)

            Log.d("EncryptionManager", "Text encrypted successfully.")
            Pair(encryptedText, ivString)
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Encryption failed: ${e.message}", e)
            throw RuntimeException("Encryption failed", e)
        }
    }

    /**
     * Decrypts the Base64 encoded ciphertext using AES-256 and the provided IV.
     * Returns the original plaintext string.
     */
    fun decrypt(encryptedText: String, ivString: String, secretKey: SecretKey): String {
        return try {
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val iv = Base64.decode(ivString, Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val decryptedString = decryptedBytes.toString(Charsets.UTF_8)

            Log.d("EncryptionManager", "Text decrypted successfully.")
            decryptedString
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Decryption failed: ${e.message}", e)
            throw RuntimeException("Decryption failed", e)
        }
    }
}