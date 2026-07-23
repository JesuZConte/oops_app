package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.SkippedUnitsResult
import javax.inject.Inject

class GetSkippedUnitsUseCase @Inject constructor(
    private val getLearningPathUseCase: GetLearningPathUseCase
) {
    suspend operator fun invoke(targetUnitId: String): SkippedUnitsResult {
        val allUnits = getLearningPathUseCase().flatMap { it.units }
        val targetIndex = allUnits.indexOfFirst { it.unit.id == targetUnitId }
        if (targetIndex < 0) return SkippedUnitsResult(targetUnit = null, skippedUnits = emptyList())

        val target = allUnits[targetIndex].unit
        val skipped = allUnits.subList(0, targetIndex)
            .filterNot { it.completed }
            .map { it.unit }
        return SkippedUnitsResult(target, skipped)
    }
}
