package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UserStats
import com.zconte.oopsapp.domain.repository.ProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeProgressRepositoryForStreak(initial: UserStats) : ProgressRepository {
    var stats = initial

    override suspend fun getUserStats(): UserStats = stats
    override suspend fun saveUserStats(stats: UserStats) {
        this.stats = stats
    }
}

class UpdateStreakUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `first ever study session sets streak to 1`() = runTest {
        val repository = FakeProgressRepositoryForStreak(UserStats(streak = 0, xp = 0, lastStudyDate = null))
        val useCase = UpdateStreakUseCase(repository)

        val result = useCase(today)

        assertEquals(1, result.streak)
    }

    @Test
    fun `studying the day after the last session increments the streak`() = runTest {
        val repository = FakeProgressRepositoryForStreak(
            UserStats(streak = 3, xp = 30, lastStudyDate = today.minusDays(1))
        )
        val useCase = UpdateStreakUseCase(repository)

        val result = useCase(today)

        assertEquals(4, result.streak)
    }

    @Test
    fun `studying after a gap resets the streak to 1`() = runTest {
        val repository = FakeProgressRepositoryForStreak(
            UserStats(streak = 5, xp = 50, lastStudyDate = today.minusDays(3))
        )
        val useCase = UpdateStreakUseCase(repository)

        val result = useCase(today)

        assertEquals(1, result.streak)
    }
}