package pant.com.nousguard

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pant.com.nousguard.data.JournalRepository
import pant.com.nousguard.data.database.NousGuardDatabase
import pant.com.nousguard.security.EncryptionManager
import javax.crypto.SecretKey

class NousGuardApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val encryptionManager: EncryptionManager by lazy {
        EncryptionManager(this)
    }

    lateinit var database: NousGuardDatabase
        private set

    lateinit var dbEncryptionKey: SecretKey
        private set

    // THIS IS THE MISSING PART: Declare the JournalRepository
    lateinit var journalRepository: JournalRepository
        private set // Set externally only within this class

    override fun onCreate() {
        super.onCreate()
        Log.d("NousGuardApp", "NousGuard Application started!")

        applicationScope.launch {
            try {
                dbEncryptionKey = encryptionManager.getOrCreateSecretKey()
                Log.d("NousGuardApp", "Database encryption key retrieved/created successfully.")

                database = NousGuardDatabase.getDatabase(this@NousGuardApplication, dbEncryptionKey)
                Log.d("NousGuardApp", "NousGuard Database initialized successfully.")

                // THIS IS THE MISSING PART: Initialize the repository
                journalRepository = JournalRepository(
                    journalDao = database.journalDao(),
                    encryptionManager = encryptionManager,
                    encryptionKey = dbEncryptionKey
                )
                Log.d("NousGuardApp", "JournalRepository initialized successfully.")

            } catch (e: Exception) {
                Log.e("NousGuardApp", "Failed to initialize encryption or database: ${e.message}", e)
                // Handle critical initialization failure, e.g., show an error to the user and exit.
            }
        }
    }
}