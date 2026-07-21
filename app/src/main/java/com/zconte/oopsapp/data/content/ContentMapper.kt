package com.zconte.oopsapp.data.content

import com.zconte.oopsapp.data.local.entity.ExerciseEntity
import com.zconte.oopsapp.data.local.entity.SectionEntity
import com.zconte.oopsapp.data.local.entity.UnitEntity
import com.zconte.oopsapp.domain.model.ExerciseContent
import kotlinx.serialization.json.Json

data class SectionPackEntities(
    val section: SectionEntity,
    val units: List<UnitEntity>,
    val exercises: List<ExerciseEntity>
)

fun ContentPack.toEntities(json: Json): SectionPackEntities {
    val section = SectionEntity(
        id = sectionId,
        name = name,
        orderIndex = orderIndex,
        examVersion = examVersion
    )
    val unitEntities = units.map { unitPack ->
        UnitEntity(
            id = unitPack.unitId,
            sectionId = sectionId,
            name = unitPack.name,
            certObjective = unitPack.certObjective,
            orderIndex = unitPack.orderIndex
        )
    }
    val exerciseEntities = units.flatMap { unitPack ->
        unitPack.exercises.map { content ->
            ExerciseEntity(
                id = content.id,
                unitId = unitPack.unitId,
                type = content.type,
                payload = json.encodeToString(ExerciseContent.serializer(), content),
                difficulty = content.difficulty,
                examVersion = examVersion
            )
        }
    }
    return SectionPackEntities(section, unitEntities, exerciseEntities)
}