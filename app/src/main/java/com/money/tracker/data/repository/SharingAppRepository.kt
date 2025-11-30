package com.money.tracker.data.repository

import com.money.tracker.data.dao.SharingAppDao
import com.money.tracker.data.entity.SharingApp
import kotlinx.coroutines.flow.Flow

class SharingAppRepository(private val sharingAppDao: SharingAppDao) {
    val enabledApps: Flow<List<SharingApp>> = sharingAppDao.getEnabledApps()
    val allApps: Flow<List<SharingApp>> = sharingAppDao.getAllApps()

    suspend fun insert(app: SharingApp) = sharingAppDao.insert(app)
    suspend fun update(app: SharingApp) = sharingAppDao.update(app)
    suspend fun delete(app: SharingApp) = sharingAppDao.delete(app)
}
