package pant.com.nousguard.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Defines the table name in the database
@Entity(tableName = "journal_entries")
data class JournalEntry(
    // Primary key for the table, autoGenerate = true means Room will generate unique IDs
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Default value for new entries

    // Encrypted title of the journal entry
    val encryptedTitle: String,

    // Encrypted content/body of the journal entry
    val encryptedContent: String,

    // Initialization Vector (IV) used for encrypting the title
    val titleIv: String,

    // Initialization Vector (IV) used for encrypting the content
    val contentIv: String,

    // Date when the entry was created/last updated
    // This will be converted by the DateConverter
    val timestamp: Date = Date()
)