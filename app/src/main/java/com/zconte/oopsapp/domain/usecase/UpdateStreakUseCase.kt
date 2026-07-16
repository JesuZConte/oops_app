package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import java.time.LocalDate
import javax.inject.Inject

private const val XP_PER_SESSION = 10

class UpdateStreakUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(today: LocalDate): UserStats {
        val stats = progressRepository.getUserStats()
        val newStreak = when (stats.lastStudyDate) {
            today -> stats.streak
            today.minusDays(1) -> stats.streak + 1
            else -> 1
        }
        val updated = stats.copy(
            streak = newStreak,
            xp = stats.xp + XP_PER_SESSION,
            lastStudyDate = today
        )
        progressRepository.saveUserStats(updated)
        return updated
    }
}