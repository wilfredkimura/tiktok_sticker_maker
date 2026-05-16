package com.tiktok.stickermaker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    videoFile: File,
    onCreateSticker: (startTime: Float, duration: Float, text: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var totalDurationMs by remember { mutableStateOf(0L) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoFile.absolutePath))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        totalDurationMs = duration
                    }
                }
            })
        }
    }

    var startTime by remember { mutableStateOf(0f) }
    var durationSec by remember { mutableStateOf(6f) }
    val totalDurationSec = totalDurationMs / 1000f

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    var textOverlay by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.White) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sticker Studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back") 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.tiktok.stickermaker.ui.theme.DarkBg,
                    titleContentColor = com.tiktok.stickermaker.ui.theme.NeonCyan,
                    navigationIconContentColor = com.tiktok.stickermaker.ui.theme.NeonCyan
                )
            )
        },
        containerColor = com.tiktok.stickermaker.ui.theme.DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Video Preview with Text Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(com.tiktok.stickermaker.ui.theme.SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Real-time Text Overlay Preview
                if (textOverlay.isNotEmpty()) {
                    Text(
                        text = textOverlay,
                        color = textColor,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Input
            OutlinedTextField(
                value = textOverlay,
                onValueChange = { textOverlay = it },
                label = { Text("Sticker Text") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = com.tiktok.stickermaker.ui.theme.NeonMagenta,
                    focusedLabelColor = com.tiktok.stickermaker.ui.theme.NeonMagenta
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Trimming Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.tiktok.stickermaker.ui.theme.SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trim Range", style = MaterialTheme.typography.labelLarge, color = com.tiktok.stickermaker.ui.theme.NeonCyan)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start: ${"%.1f".format(startTime)}s", color = com.tiktok.stickermaker.ui.theme.TextSecondary)
                        Text("Length: ${"%.1f".format(durationSec)}s", color = com.tiktok.stickermaker.ui.theme.TextSecondary)
                    }
                    Slider(
                        value = startTime,
                        onValueChange = { 
                            startTime = it
                            exoPlayer.seekTo((it * 1000).toLong())
                        },
                        valueRange = 0f..maxOf(0f, totalDurationSec - 1f),
                        colors = SliderDefaults.colors(thumbColor = com.tiktok.stickermaker.ui.theme.NeonCyan, activeTrackColor = com.tiktok.stickermaker.ui.theme.NeonCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // WhatsApp Chat Preview Mockup
            Text("Chat Preview", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start), color = com.tiktok.stickermaker.ui.theme.TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Color(0xFF075E54).copy(alpha = 0.2f)) // WhatsApp Dark Green background
                    .padding(16.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .align(Alignment.End),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sticker\nPreview", textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f))
                        // In reality, we'd show the current video frame here
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onCreateSticker(startTime, durationSec, textOverlay) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.tiktok.stickermaker.ui.theme.NeonMagenta)
            ) {
                Icon(Icons.Default.Done, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Sticker", fontWeight = FontWeight.Bold)
            }
        }
    }
}
