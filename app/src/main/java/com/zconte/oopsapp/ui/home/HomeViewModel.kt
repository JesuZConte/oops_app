package com.zconte.oopsapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zconte.oopsapp.data.content.ContentSeeder
import com.zconte.oopsapp.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val streak: Int = 0,
    val xp: Int = 0,
    val isReady: Boolean = false,
    val streamsReadiness: Float = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val contentSeeder: ContentSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentSeeder.seedIfNeeded()
            refreshStats()
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshStats() }
    }

    private suspend fun refreshStats() {
        val stats = progressRepository.getUserStats()
        val readiness = progressRepository.getReadinessByObjective()
        _uiState.update {
            it.copy(
                streak = stats.streak,
                xp = stats.xp,
                streamsReadiness = readiness["streams-lambdas"] ?: 0f
            )
        }
    }
}