package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.UserStatsDao
import com.zconte.oopsapp.data.local.entity.UserStatsEntity
import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import java.time.LocalDate
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val userStatsDao: UserStatsDao
) : ProgressRepository {

    override suspend fun getUserStats(): UserStats {
        val entity = userStatsDao.get()
        return UserStats(
            streak = entity?.streak ?: 0,
            xp = entity?.xp ?: 0,
            lastStudyDate = entity?.lastStudyDate?.let { LocalDate.ofEpochDay(it) }
        )
    }

    override suspend fun saveUserStats(stats: UserStats) {
        userStatsDao.upsert(
            UserStatsEntity(
                streak = stats.streak,
                xp = stats.xp,
                lastStudyDate = stats.lastStudyDate?.toEpochDay()
            )
        )
    }
}