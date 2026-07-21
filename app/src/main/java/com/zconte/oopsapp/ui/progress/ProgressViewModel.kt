package com.zconte.oopsapp.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.domain.model.SectionPath
import com.zconte.oopsapp.domain.usecase.GetLearningPathUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val sections: List<SectionPath> = emptyList()
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val getLearningPathUseCase: GetLearningPathUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val sections = getLearningPathUseCase()
            _uiState.update { it.copy(sections = sections) }
        }
    }
}
