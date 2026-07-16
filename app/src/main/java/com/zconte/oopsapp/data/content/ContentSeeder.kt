package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.TopicDao
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ContentSeeder @Inject constructor(
    private val contentLoader: ContentLoader,
    private val topicDao: TopicDao,
    private val exerciseDao: ExerciseDao,
    private val json: Json
) {
    suspend fun seedIfEmpty() {
        if (exerciseDao.count() > 0) return
        val pack = contentLoader.loadPack("content/streams.json")
        val (topic, exercises) = pack.toEntities(json)
        topicDao.insertAll(listOf(topic))
        exerciseDao.insertAll(exercises)
    }
}