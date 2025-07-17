package pant.com.nousguard.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pant.com.nousguard.data.database.dao.JournalDao
import pant.com.nousguard.data.database.entities.JournalEntry
import pant.com.nousguard.security.EncryptionManager
import javax.crypto.SecretKey

class JournalRepository(
    private val journalDao: JournalDao,
    private val encryptionManager: EncryptionManager,
    private val encryptionKey: SecretKey // The actual SecretKey from Keystore
) {

    // --- Public API for ViewModels to interact with ---

    // Get all journal entries (Flow for observing changes)
    val allJournalEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    /**
     * Inserts a new journal entry after encrypting its title and content.
     */
    suspend fun insertJournalEntry(title: String, content: String) {
        withContext(Dispatchers.IO) { // Perform heavy operations on IO thread
            try {
                // Encrypt title
                val (encryptedTitle, titleIv) = encryptionManager.encrypt(title, encryptionKey)
                // Encrypt content
                val (encryptedContent, contentIv) = encryptionManager.encrypt(content, encryptionKey)

                val newEntry = JournalEntry(
                    encryptedTitle = encryptedTitle,
                    encryptedContent = encryptedContent,
                    titleIv = titleIv,
                    contentIv = contentIv
                )
                journalDao.insert(newEntry)
                Log.d("JournalRepository", "Journal entry inserted successfully (encrypted).")
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error inserting journal entry: ${e.message}", e)
                // Re-throw or handle as per your error handling strategy
                throw RuntimeException("Failed to insert journal entry", e)
            }
        }
    }

    /**
     * Updates an existing journal entry after encrypting its title and content.
     * Original ID and timestamps are preserved.
     */
    suspend fun updateJournalEntry(entry: JournalEntry, newTitle: String, newContent: String) {
        withContext(Dispatchers.IO) {
            try {
                val (encryptedTitle, titleIv) = encryptionManager.encrypt(newTitle, encryptionKey)
                val (encryptedContent, contentIv) = encryptionManager.encrypt(newContent, encryptionKey)

                val updatedEntry = entry.copy(
                    encryptedTitle = encryptedTitle,
                    encryptedContent = encryptedContent,
                    titleIv = titleIv, // Important: new IV for updated content
                    contentIv = contentIv, // Important: new IV for updated content
                    timestamp = java.util.Date() // Update timestamp on modification
                )
                journalDao.update(updatedEntry)
                Log.d("JournalRepository", "Journal entry updated successfully (encrypted).")
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error updating journal entry: ${e.message}", e)
                throw RuntimeException("Failed to update journal entry", e)
            }
        }
    }

    /**
     * Deletes a journal entry.
     */
    suspend fun deleteJournalEntry(entry: JournalEntry) {
        withContext(Dispatchers.IO) {
            try {
                journalDao.delete(entry)
                Log.d("JournalRepository", "Journal entry deleted successfully.")
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error deleting journal entry: ${e.message}", e)
                throw RuntimeException("Failed to delete journal entry", e)
            }
        }
    }

    /**
     * Retrieves and decrypts a specific journal entry by its ID.
     */
    suspend fun getAndDecryptJournalEntry(entryId: Long): JournalEntry? {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedEntry = journalDao.getEntryById(entryId)
                if (encryptedEntry != null) {
                    val decryptedTitle = encryptionManager.decrypt(
                        encryptedEntry.encryptedTitle,
                        encryptedEntry.titleIv,
                        encryptionKey
                    )
                    val decryptedContent = encryptionManager.decrypt(
                        encryptedEntry.encryptedContent,
                        encryptedEntry.contentIv,
                        encryptionKey
                    )
                    Log.d("JournalRepository", "Journal entry decrypted successfully.")
                    // Return a new JournalEntry object with decrypted values
                    encryptedEntry.copy(
                        encryptedTitle = decryptedTitle,
                        encryptedContent = decryptedContent
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error retrieving/decrypting journal entry: ${e.message}", e)
                throw RuntimeException("Failed to retrieve or decrypt journal entry", e)
            }
        }
    }

    /**
     * Retrieves all journal entries and decrypts them.
     * This might be heavy for many entries, consider decrypting on demand in UI if performance is an issue.
     */
    fun getAllAndDecryptJournalEntries(): Flow<List<JournalEntry>> {
        // Map the flow of encrypted entries to a flow of decrypted entries
        return journalDao.getAllEntries().map { encryptedEntries ->
            encryptedEntries.map { entry ->
                try {
                    val decryptedTitle = encryptionManager.decrypt(
                        entry.encryptedTitle,
                        entry.titleIv,
                        encryptionKey
                    )
                    val decryptedContent = encryptionManager.decrypt(
                        entry.encryptedContent,
                        entry.contentIv,
                        encryptionKey
                    )
                    entry.copy(
                        encryptedTitle = decryptedTitle,
                        encryptedContent = decryptedContent
                    )
                } catch (e: Exception) {
                    Log.e("JournalRepository", "Error decrypting an entry in flow: ${e.message}", e)
                    // You might want to return a placeholder or skip the entry if decryption fails
                    entry.copy(encryptedTitle = "Decryption Failed!", encryptedContent = "Error: Cannot decrypt content.")
                }
            }
        }
    }
}