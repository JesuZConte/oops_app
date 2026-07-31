package com.zconte.oopsapp.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.UnitSummary
import com.zconte.oopsapp.domain.usecase.GetUnitSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitSummaryUiState(
    val unitId: String = "",
    val summary: UnitSummary? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class UnitSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUnitSummaryUseCase: GetUnitSummaryUseCase
) : ViewModel() {

    private val unitId: String = checkNotNull(savedStateHandle["unitId"])

    private val _uiState = MutableStateFlow(UnitSummaryUiState(unitId = unitId))
    val uiState: StateFlow<UnitSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val summary = getUnitSummaryUseCase(unitId)
            _uiState.update { it.copy(summary = summary, isLoading = false) }
        }
    }
}
