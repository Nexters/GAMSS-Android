package com.gamss.android.feature.emotion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.domain.emotion.ConversationEmotion
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private const val MAX_DIARY_LEN = 140
private const val PERCENT = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionScreen(
    viewModel: EmotionViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    viewModel.collectSideEffect { }

    Scaffold(
        topBar = { TopAppBar(title = { Text("오늘의 일기 감정") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "오늘 있었던 일을 일기로 적어주세요 (${MAX_DIARY_LEN}자 이내)",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChange,
                label = { Text("오늘의 일기") },
                minLines = 5,
                supportingText = { Text("${state.input.length}/$MAX_DIARY_LEN") },
                isError = state.input.length > MAX_DIARY_LEN,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::onAnalyze,
                enabled = !state.isRunning &&
                    state.input.isNotBlank() &&
                    state.input.length <= MAX_DIARY_LEN,
            ) {
                Text("감정 분석")
            }
            if (state.isRunning) {
                CircularProgressIndicator()
            }
            if (state.notRecognized) {
                Text("일기를 인식하지 못했어요. 내용을 입력했는지 확인해 주세요.")
            }
            state.error?.let { Text("오류: $it") }
            state.result?.let { ResultCard(it) }
        }
    }
}

@Composable
private fun ResultCard(result: ConversationEmotion) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "대표 감정: ${result.emotion.topLabel} (${(result.emotion.confidence * PERCENT).toInt()}%)",
                style = MaterialTheme.typography.titleLarge,
            )
            Text("전체 점수", style = MaterialTheme.typography.titleSmall)
            result.emotion.scores.entries
                .sortedByDescending { it.value }
                .forEach { (label, score) ->
                    Text("· $label ${(score * PERCENT).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                }
        }
    }
}
