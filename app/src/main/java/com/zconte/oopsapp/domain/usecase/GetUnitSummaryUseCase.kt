package com.zconte.oopsapp.domain.usecase

import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.repository.ContentRepository
import javax.inject.Inject

class GetUnitSummaryUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(unitId: String): UnitSummary? =
        contentRepository.getUnitSummary(unitId)
}
