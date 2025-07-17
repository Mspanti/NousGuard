package pant.com.nousguard.data.database.typeconverters

import androidx.room.TypeConverter
import java.util.Date

class DateConverter {

    // Converts a Long (timestamp) from the database to a Date object.
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    // Converts a Date object to a Long (timestamp) for storage in the database.
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}