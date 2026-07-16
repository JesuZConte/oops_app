package com.zconte.oopsapp.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionScreen(
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete) onSessionComplete()
    }

    val exercise = uiState.currentExercise
    if (exercise == null) {
        Text("Cargando sesion...", modifier = modifier.padding(16.dp))
        return
    }

    var answer by remember(exercise.id) { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        Text(exercise.prompt)
        exercise.code?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
        Spacer(Modifier.height(16.dp))

        if (!uiState.isAnswered) {
            OutlinedTextField(value = answer, onValueChange = { answer = it })
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.submitAnswer(answer) }) { Text("Responder") }
        } else {
            Text(if (uiState.isCorrect) "Correcto!" else "Incorrecto. Respuesta: ${exercise.answer}")
            Spacer(Modifier.height(4.dp))
            Text(exercise.explanation)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.nextExercise() }) { Text("Siguiente") }
        }
    }
}