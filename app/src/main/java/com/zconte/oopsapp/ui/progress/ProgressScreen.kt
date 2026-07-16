package com.zconte.oopsapp.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(uiState.readinessByObjective.entries.toList()) { (objective, readiness) ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("$objective — ${(readiness * 100).toInt()}%")
                LinearProgressIndicator(progress = { readiness })
            }
        }
    }
}