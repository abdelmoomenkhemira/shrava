package com.example.shrava.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shrava.data.entity.LocationPointEntity

@Dao
interface LocationPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<LocationPointEntity>)

    @Query("SELECT * FROM location_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    suspend fun getByActivityId(activityId: Long): List<LocationPointEntity>
}
