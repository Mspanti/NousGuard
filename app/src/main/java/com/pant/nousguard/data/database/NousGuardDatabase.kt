package pant.com.nousguard.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory
import pant.com.nousguard.data.database.dao.JournalDao
import pant.com.nousguard.data.database.entities.JournalEntry
import pant.com.nousguard.data.database.typeconverters.DateConverter
import javax.crypto.SecretKey

@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class) // For storing Date objects
abstract class NousGuardDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: NousGuardDatabase? = null
        private const val DATABASE_NAME = "nousguard_db"

        // Updated getDatabase function to accept SecretKey directly
        fun getDatabase(
            context: Context,
            encryptionSecretKey: SecretKey // Now accepts SecretKey
        ): NousGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NousGuardDatabase::class.java,
                    DATABASE_NAME
                )
                    // Use the raw key material (bytes) from the SecretKey for SQLCipher
                    .openHelperFactory(SupportFactory(encryptionSecretKey.encoded))
                    .fallbackToDestructiveMigration() // For development, can remove later or implement proper migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}