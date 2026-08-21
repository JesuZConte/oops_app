package com.zconte.oopsapp.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zconte.oopsapp.data.local.AppDatabase
import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseDao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        unitId = "u1",
        type = "fill_blank",
        payload = "{}",
        difficulty = 1,
        examVersion = "core"
    )

    private fun reviewState(
        exerciseId: String,
        repetitions: Int,
        easeFactor: Double,
        dueDate: Long,
        lastReviewedAt: Long
    ) = ReviewStateEntity(
        exerciseId = exerciseId,
        easeFactor = easeFactor,
        intervalDays = 1,
        repetitions = repetitions,
        dueDate = dueDate,
        lastReviewedAt = lastReviewedAt
    )

    @Test
    fun getDue_ordersByRepetitionsAscendingFirst() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("weak"), exercise("strong")))
        db.reviewStateDao().upsert(reviewState("strong", repetitions = 5, easeFactor = 2.5, dueDate = 100, lastReviewedAt = 50))
        db.reviewStateDao().upsert(reviewState("weak", repetitions = 0, easeFactor = 2.5, dueDate = 100, lastReviewedAt = 50))

        val result = exerciseDao.getDue(today = 100)

        assertEquals(listOf("weak", "strong"), result.map { it.id })
    }

    @Test
    fun getDue_withinSameRepetitionsBucket_mostRecentlyReviewedWinsOverEaseFactor() = runBlocking {
        // Both repetitions = 1 (a fresh pass). "fresh-yesterday" was just reviewed and has a
        // slightly worse-ranking easeFactor than "old-qa-backlog", which hasn't been touched in
        // weeks. Without the lastReviewedAt tiebreaker, easeFactor ASC alone would put
        // "old-qa-backlog" first -- burying next-day reinforcement of newly taught content.
        // This is the bug an advisor review caught before the spec was committed.
        exerciseDao.insertAll(listOf(exercise("fresh-yesterday"), exercise("old-qa-backlog")))
        db.reviewStateDao().upsert(
            reviewState("fresh-yesterday", repetitions = 1, easeFactor = 2.6, dueDate = 100, lastReviewedAt = 99)
        )
        db.reviewStateDao().upsert(
            reviewState("old-qa-backlog", repetitions = 1, easeFactor = 2.5, dueDate = 100, lastReviewedAt = 40)
        )

        val result = exerciseDao.getDue(today = 100)

        assertEquals(listOf("fresh-yesterday", "old-qa-backlog"), result.map { it.id })
    }

    @Test
    fun getDue_withinSameRepetitionsAndLastReviewedAt_easeFactorAscendingBreaksTie() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("harder"), exercise("easier")))
        db.reviewStateDao().upsert(reviewState("easier", repetitions = 2, easeFactor = 2.8, dueDate = 100, lastReviewedAt = 50))
        db.reviewStateDao().upsert(reviewState("harder", repetitions = 2, easeFactor = 1.5, dueDate = 100, lastReviewedAt = 50))

        val result = exerciseDao.getDue(today = 100)

        assertEquals(listOf("harder", "easier"), result.map { it.id })
    }

    @Test
    fun getStale_returnsOnlyItemsDueAtOrBeforeCutoff() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("very-old"), exercise("recent")))
        db.reviewStateDao().upsert(reviewState("very-old", repetitions = 3, easeFactor = 2.5, dueDate = 50, lastReviewedAt = 40))
        db.reviewStateDao().upsert(reviewState("recent", repetitions = 3, easeFactor = 2.5, dueDate = 95, lastReviewedAt = 90))

        val result = exerciseDao.getStale(cutoff = 55)

        assertEquals(listOf("very-old"), result.map { it.id })
    }

    @Test
    fun getStale_includesItemsExactlyAtCutoff() = runBlocking {
        exerciseDao.insertAll(listOf(exercise("exact")))
        db.reviewStateDao().upsert(reviewState("exact", repetitions = 3, easeFactor = 2.5, dueDate = 55, lastReviewedAt = 40))

        val result = exerciseDao.getStale(cutoff = 55)

        assertEquals(listOf("exact"), result.map { it.id })
    }
}
