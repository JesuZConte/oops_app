package com.zconte.oopsapp.data.repository

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.ReviewStateDao
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private class FakeExerciseDaoForRepository(
    private val due: List<ExerciseEntity> = emptyList(),
    private val stale: List<ExerciseEntity> = emptyList()
) : ExerciseDao {
    var lastStaleCutoff: Long? = null

    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override suspend fun clearAll() {}
    override suspend fun getDue(today: Long): List<ExerciseEntity> = due
    override suspend fun getStale(cutoff: Long): List<ExerciseEntity> {
        lastStaleCutoff = cutoff
        return stale
    }
    override suspend fun getByUnit(unitId: String): List<ExerciseEntity> = emptyList()
    override suspend fun getBySection(sectionId: String): List<ExerciseEntity> = emptyList()
}

private class NoOpReviewStateDao : ReviewStateDao {
    override suspend fun upsert(state: ReviewStateEntity) {}
    override suspend fun getByExerciseId(exerciseId: String): ReviewStateEntity? = null
    override suspend fun getExistingIds(exerciseIds: List<String>): List<String> = emptyList()
}

class ExerciseRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun entity(id: String) = ExerciseEntity(
        id = id,
        unitId = "u1",
        type = "fill_blank",
        payload = """{"id":"$id","type":"fill_blank","difficulty":1,"prompt":"p","answer":"a","explanation":"e"}""",
        difficulty = 1,
        examVersion = "core"
    )

    @Test
    fun `getStaleExercises delegates to the DAO with the cutoff converted to epoch day`() = runTest {
        val exerciseDao = FakeExerciseDaoForRepository(stale = listOf(entity("stale-1")))
        val repository = ExerciseRepositoryImpl(exerciseDao, NoOpReviewStateDao(), json)

        val cutoff = LocalDate.of(2026, 7, 1)
        val result = repository.getStaleExercises(cutoff)

        assertEquals(listOf("stale-1"), result.map { it.id })
        assertEquals(cutoff.toEpochDay(), exerciseDao.lastStaleCutoff)
    }

    @Test
    fun `getDueExercises still delegates to getDue and respects the limit, unchanged`() = runTest {
        val exerciseDao = FakeExerciseDaoForRepository(due = listOf(entity("due-1"), entity("due-2")))
        val repository = ExerciseRepositoryImpl(exerciseDao, NoOpReviewStateDao(), json)

        val result = repository.getDueExercises(LocalDate.of(2026, 7, 1), limit = 1)

        assertEquals(listOf("due-1"), result.map { it.id })
    }
}
