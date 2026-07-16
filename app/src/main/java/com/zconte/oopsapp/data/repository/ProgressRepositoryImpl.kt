package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.dao.UserStatsDao
import com.zconte.oopsapp.data.local.entity.UserStatsEntity
import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import java.time.LocalDate
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val userStatsDao: UserStatsDao,
    private val reviewStateDao: ReviewStateDao,
    private val exerciseDao: ExerciseDao
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

    override suspend fun getReadinessByObjective(): Map<String, Float> {
        val mastered = reviewStateDao.getMasteredCountByObjective()
            .associate { it.objective to it.masteredCount }
        val total = exerciseDao.getTotalCountByObjective()
            .associate { it.objective to it.totalCount }
        return total.mapValues { (objective, totalCount) ->
            if (totalCount == 0) 0f else (mastered[objective] ?: 0).toFloat() / totalCount
        }
    }
}