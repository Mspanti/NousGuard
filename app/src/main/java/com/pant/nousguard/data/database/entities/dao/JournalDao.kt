package pant.com.nousguard.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pant.com.nousguard.data.database.entities.JournalEntry

@Dao
interface JournalDao {

    // Inserts a new journal entry into the database.
    // OnConflictStrategy.REPLACE means if an entry with the same primary key exists, it will be replaced.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry)

    // Updates an existing journal entry.
    @Update
    suspend fun update(entry: JournalEntry)

    // Deletes a specific journal entry.
    @Delete
    suspend fun delete(entry: JournalEntry)

    // Deletes a journal entry by its ID.
    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    // Retrieves all journal entries, ordered by timestamp in descending order (newest first).
    // Returns a Flow, which emits new data whenever the underlying database changes.
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    // Retrieves a single journal entry by its ID.
    @Query("SELECT * FROM journal_entries WHERE id = :entryId LIMIT 1")
    suspend fun getEntryById(entryId: Long): JournalEntry?
}