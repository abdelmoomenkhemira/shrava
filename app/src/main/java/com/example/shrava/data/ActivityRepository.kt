package com.example.shrava.data

import com.example.shrava.data.dao.ActivityDao
import com.example.shrava.data.dao.LocationPointDao
import com.example.shrava.data.entity.ActivityEntity
import com.example.shrava.data.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

class ActivityRepository(
    private val activityDao: ActivityDao,
    private val locationPointDao: LocationPointDao
) {

    fun getAllActivities(): Flow<List<ActivityEntity>> = activityDao.getAll()

    suspend fun getActivityById(id: Long): ActivityEntity? = activityDao.getById(id)

    suspend fun insertActivity(activity: ActivityEntity): Long = activityDao.insert(activity)

    suspend fun deleteActivity(activityId: Long) = activityDao.deleteById(activityId)

    suspend fun insertLocationPoints(points: List<LocationPointEntity>) {
        locationPointDao.insertAll(points)
    }

    suspend fun getLocationPoints(activityId: Long): List<LocationPointEntity> {
        return locationPointDao.getByActivityId(activityId)
    }
}
