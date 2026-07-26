package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository

class FakeProgressRepository(
    initialStats: UserStats = UserStats(streak = 0, xp = 0, lastStudyDate = null)
) : ProgressRepository {

    var stats: UserStats = initialStats
        private set

    override suspend fun getUserStats(): UserStats = stats

    override suspend fun saveUserStats(stats: UserStats) {
        this.stats = stats
    }
}
