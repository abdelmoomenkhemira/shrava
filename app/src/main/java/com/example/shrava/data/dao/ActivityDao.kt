package com.example.shrava.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shrava.data.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity): Long

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun getAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM activities WHERE type = 'Run' AND distanceMeters >= 1000 ORDER BY avgPaceSecondsPerKm ASC LIMIT 1")
    suspend fun getBestRun(): ActivityEntity?

    @Query("SELECT * FROM activities WHERE type = 'Run' ORDER BY startTime DESC")
    suspend fun getAllRuns(): List<ActivityEntity>
}
