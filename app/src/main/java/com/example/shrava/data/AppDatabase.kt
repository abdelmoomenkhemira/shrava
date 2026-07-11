package com.example.shrava.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.shrava.data.dao.ActivityDao
import com.example.shrava.data.dao.LocationPointDao
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity

@Database(
    entities = [ActivityEntity::class, LocationPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun locationPointDao(): LocationPointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shrava_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
