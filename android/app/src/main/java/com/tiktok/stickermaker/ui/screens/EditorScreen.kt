package com.tiktok.stickermaker.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    videoFile: File,
    onCreateSticker: (startTime: Float, duration: Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoFile.absolutePath))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var startTime by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(6f) }
    val totalDuration = remember(exoPlayer.duration) { 
        if (exoPlayer.duration > 0) exoPlayer.duration / 1000f else 15f 
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Clip") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Video Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trimming Controls
            Text("Start Time: ${"%.1f".format(startTime)}s")
            Slider(
                value = startTime,
                onValueChange = { startTime = it },
                valueRange = 0f..maxOf(0f, totalDuration - duration)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Duration: ${"%.1f".format(duration)}s (Max 6s)")
            Slider(
                value = duration,
                onValueChange = { duration = it },
                valueRange = 1f..minOf(6f, totalDuration - startTime)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onCreateSticker(startTime, duration) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Sticker")
            }
        }
    }
}
