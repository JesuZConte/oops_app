package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.dao.ContentMetaDao
import com.zconte.oopsapp.data.local.dao.ExerciseDao
import com.zconte.oopsapp.data.local.dao.SectionDao
import com.zconte.oopsapp.data.local.dao.UnitDao
import com.zconte.oopsapp.data.local.entity.ContentMetaEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val CONTENT_VERSION_KEY = "content_version"
private const val CURRENT_CONTENT_VERSION = "2"

class ContentSeeder @Inject constructor(
    private val contentLoader: ContentLoader,
    private val sectionDao: SectionDao,
    private val unitDao: UnitDao,
    private val exerciseDao: ExerciseDao,
    private val contentMetaDao: ContentMetaDao,
    private val json: Json
) {
    private val packAssetPaths = listOf(
        "content/java-fundamentals.json",
        "content/streams.json"
    )

    suspend fun seedIfNeeded() {
        val seededVersion = contentMetaDao.get(CONTENT_VERSION_KEY)?.value
        if (seededVersion == CURRENT_CONTENT_VERSION) return

        sectionDao.clearAll()
        unitDao.clearAll()
        exerciseDao.clearAll()

        packAssetPaths.forEach { assetPath ->
            val pack = contentLoader.loadPack(assetPath)
            val entities = pack.toEntities(json)
            sectionDao.insertAll(listOf(entities.section))
            unitDao.insertAll(entities.units)
            exerciseDao.insertAll(entities.exercises)
        }

        contentMetaDao.upsert(ContentMetaEntity(configKey = CONTENT_VERSION_KEY, value = CURRENT_CONTENT_VERSION))
    }
}