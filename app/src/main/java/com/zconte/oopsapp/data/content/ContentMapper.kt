package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.TopicEntity
import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json

fun ContentPack.toEntities(json: Json): Pair<TopicEntity, List<ExerciseEntity>> {
    val topic = TopicEntity(
        id = topicId,
        name = name,
        certObjective = certObjective,
        orderIndex = 0
    )
    val exerciseEntities = exercises.map { content ->
        ExerciseEntity(
            id = content.id,
            topicId = topicId,
            type = content.type,
            payload = json.encodeToString(ExerciseContent.serializer(), content),
            difficulty = content.difficulty
        )
    }
    return topic to exerciseEntities
}