package com.zconte.oopsapp.domain.repository

import com.zconte.oopsapp.domain.model.UserStats

interface ProgressRepository {
    suspend fun getUserStats(): UserStats
    suspend fun saveUserStats(stats: UserStats)
    suspend fun getReadinessByObjective(): Map<String, Float>
}