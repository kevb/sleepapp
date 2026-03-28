package com.sleepwithme.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.sleepwithme.app.MainViewModel
import com.sleepwithme.app.player.AmbientMode
import com.sleepwithme.app.timer.TimerState

private val Indigo = Color(0xFF6C63FF)
private val IndigoDeep = Color(0xFF3D35A0)
private val Amber = Color(0xFFFFA726)
private val Muted = Color(0xFF666680)

@Composable
fun PlayerScreen(viewModel: MainViewModel) {
    val timerState by viewModel.sleepTimer.state.collectAsState()
    val remainingSeconds by viewModel.sleepTimer.remainingSeconds.collectAsState()
    val timerDuration by viewModel.timerDurationMins.collectAsState()

    val collection by viewModel.collection.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val trackIndex by viewModel.trackIndex.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()

    val ambientMode by viewModel.ambientMode.collectAsState()

    val currentTrack = collection?.tracks?.getOrNull(trackIndex)
    val totalTracks = collection?.tracks?.size ?: 0

    // Pulsing alpha for fading state
    val infiniteTransition = rememberInfiniteTransition(label = "fade-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val timerColor by animateColorAsState(
        targetValue = when (timerState) {
            TimerState.Running -> Indigo
            TimerState.Fading -> Amber
            TimerState.Stopped -> Muted
        },
        animationSpec = tween(500),
        label = "timer-color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF0A0A12),
                        Color(0xFF060610)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Collection & Track info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = (collection?.title ?: "Loading...").uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Indigo.copy(alpha = 0.7f),
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = currentTrack?.title ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (totalTracks > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${trackIndex + 1} of $totalTracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Ambient toggle
            OutlinedButton(
                onClick = { viewModel.cycleAmbient() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = when (ambientMode) {
                        AmbientMode.Off -> Color.White.copy(alpha = 0.3f)
                        AmbientMode.Rain -> Indigo
                        AmbientMode.Storm -> Amber
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (ambientMode) {
                        AmbientMode.Off -> Color.White.copy(alpha = 0.1f)
                        AmbientMode.Rain -> Indigo.copy(alpha = 0.5f)
                        AmbientMode.Storm -> Amber.copy(alpha = 0.5f)
                    }
                )
            ) {
                Text(
                    text = when (ambientMode) {
                        AmbientMode.Off -> "Ambient Off"
                        AmbientMode.Rain -> "Rain"
                        AmbientMode.Storm -> "Storm"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Sleep Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SLEEP TIMER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.25f),
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = formatTimer(remainingSeconds),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Thin,
                    fontFamily = FontFamily.Default,
                    color = timerColor,
                    modifier = Modifier.alpha(
                        if (timerState == TimerState.Fading) pulseAlpha else 1f
                    ),
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(16.dp))

                // Timer adjust
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.adjustTimer(-5) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("-5", fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = "${timerDuration}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.adjustTimer(5) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("+5", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Transport controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Progress bar
                if (currentTrack != null && currentTrack.durationSecs > 0) {
                    val progress = (positionMs / 1000f) / currentTrack.durationSecs
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Indigo.copy(alpha = 0.8f),
                        trackColor = Color.White.copy(alpha = 0.08f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(positionMs / 1000),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = formatTime(currentTrack.durationSecs.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Seek offset state
                var seekOffsetMins by remember { mutableIntStateOf(0) }

                Box(contentAlignment = Alignment.Center) {
                    // Offset overlay
                    if (seekOffsetMins != 0) {
                        Text(
                            text = if (seekOffsetMins > 0) "+${seekOffsetMins}m" else "${seekOffsetMins}m",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            color = Indigo,
                            modifier = Modifier.offset(y = (-56).dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind
                        SeekButton(
                            label = "\u25C0\u25C0",
                            onTick = { seekOffsetMins -= 2 },
                            onRelease = {
                                if (seekOffsetMins != 0) {
                                    viewModel.seekRelative(seekOffsetMins * 60 * 1000L)
                                    seekOffsetMins = 0
                                }
                            }
                        )

                        // Play/Pause
                        Button(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo
                            ),
                            contentPadding = PaddingValues(0.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp
                            )
                        ) {
                            Text(
                                text = if (isPlaying) "\u23F8" else "\u25B6",
                                fontSize = if (isPlaying) 28.sp else 24.sp,
                                color = Color.White
                            )
                        }

                        // Forward
                        SeekButton(
                            label = "\u25B6\u25B6",
                            onTick = { seekOffsetMins += 2 },
                            onRelease = {
                                if (seekOffsetMins != 0) {
                                    viewModel.seekRelative(seekOffsetMins * 60 * 1000L)
                                    seekOffsetMins = 0
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SeekButton(
    label: String,
    onTick: () -> Unit,
    onRelease: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            onTick() // immediate first tick
            delay(400) // initial delay before repeat
            while (pressed) {
                onTick()
                delay(300) // repeat interval
            }
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            letterSpacing = (-4).sp,
            color = if (pressed) Indigo else Color.White.copy(alpha = 0.7f)
        )
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
