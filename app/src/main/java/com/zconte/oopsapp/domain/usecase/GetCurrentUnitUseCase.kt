package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.LearningUnit
import javax.inject.Inject

class GetCurrentUnitUseCase @Inject constructor(
    private val getLearningPathUseCase: GetLearningPathUseCase
) {
    suspend operator fun invoke(): LearningUnit? =
        getLearningPathUseCase()
            .flatMap { it.units }
            .firstOrNull { it.unlocked && !it.completed }
            ?.unit
}
