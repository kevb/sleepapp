package com.sleepwithme.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepwithme.app.MainViewModel
import com.sleepwithme.app.timer.TimerState

@Composable
fun PlayerScreen(viewModel: MainViewModel) {
    val timerState by viewModel.sleepTimer.state.collectAsState()
    val remainingSeconds by viewModel.sleepTimer.remainingSeconds.collectAsState()
    val timerDuration by viewModel.timerDurationMins.collectAsState()

    val collection by viewModel.collection.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val trackIndex by viewModel.trackIndex.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()

    val currentTrack = collection?.tracks?.getOrNull(trackIndex)
    val totalTracks = collection?.tracks?.size ?: 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Collection & Track info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = collection?.title ?: "Loading...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentTrack?.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (totalTracks > 0) {
                    Text(
                        text = "${trackIndex + 1} / $totalTracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }

            // Sleep Timer (dominant element)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SLEEP TIMER",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatTimer(remainingSeconds),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    color = when (timerState) {
                        TimerState.Running -> MaterialTheme.colorScheme.primary
                        TimerState.Fading -> Color(0xFFFFA726) // amber
                        TimerState.Stopped -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))

                // Timer adjust
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { viewModel.adjustTimer(-5) }) {
                        Text("-5m")
                    }
                    Text(
                        text = "${timerDuration}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    FilledTonalButton(onClick = { viewModel.adjustTimer(5) }) {
                        Text("+5m")
                    }
                }
            }

            // Transport controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Position
                if (currentTrack != null) {
                    Text(
                        text = "${formatTime(positionMs / 1000)} / ${formatTime(currentTrack.durationSecs.toLong())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.previous() },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("<<", fontSize = 18.sp)
                    }

                    Button(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(72.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "||" else ">",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.next() },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(">>", fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
