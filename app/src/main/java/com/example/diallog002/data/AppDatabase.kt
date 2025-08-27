package com.example.diallog002.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

class Converters {
	@TypeConverter
	fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

	@TypeConverter
	fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Database(entities = [CallLog::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
	abstract fun callLogDao(): CallLogDao

	companion object {
		@Volatile
		private var INSTANCE: AppDatabase? = null

		fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
			INSTANCE ?: Room.databaseBuilder(
				context.applicationContext,
				AppDatabase::class.java,
				"diallog.db"
			).fallbackToDestructiveMigration().build().also { INSTANCE = it }
		}
	}
}

