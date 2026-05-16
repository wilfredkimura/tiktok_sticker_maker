package com.tiktok.stickermaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiktok.stickermaker.ui.screens.EditorScreen
import com.tiktok.stickermaker.ui.viewmodel.AppState
import com.tiktok.stickermaker.ui.viewmodel.StickerViewModel
import com.tiktok.stickermaker.stickers.StickerPackManager
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var initialUrl = ""
        if (intent?.action == Intent.ACTION_SEND && "text/plain" == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                initialUrl = it
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(initialUrl)
                }
            }
        }
    }
}

@Composable
fun MainApp(initialUrl: String, viewModel: StickerViewModel = viewModel()) {
    val state = viewModel.state
    val context = LocalContext.current

    when (state) {
        is AppState.Home -> HomeScreen(
            initialUrl = initialUrl,
            onProcess = { viewModel.processVideo(it) }
        )
        is AppState.Loading -> LoadingScreen("Resolving video...")
        is AppState.Editor -> EditorScreen(
            videoFile = state.videoFile,
            onCreateSticker = { start, duration -> viewModel.createSticker(state.videoFile, start, duration) },
            onBack = { viewModel.reset() }
        )
        is AppState.Exporting -> LoadingScreen("Generating sticker...")
        is AppState.Success -> SuccessScreen(
            onAddWhatsApp = { 
                StickerPackManager.addStickerPackToWhatsApp(
                    context = context,
                    identifier = "tiktok_stickers_1",
                    stickerPackName = "TikTok Hits"
                ) 
            },
            onDone = { viewModel.reset() }
        )
        is AppState.Error -> ErrorScreen(state.message) { viewModel.reset() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(initialUrl: String, onProcess: (String) -> Unit, viewModel: StickerViewModel = viewModel()) {
    var url by remember { mutableStateOf(initialUrl) }
    val isConnected = viewModel.isBackendConnected

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("TikTok Sticker Maker") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isConnected) androidx.compose.ui.graphics.Color.Green 
                                    else androidx.compose.ui.graphics.Color.Red,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "Connected" else "Offline",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("TikTok Video URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onProcess(url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank()
            ) {
                Text("Process Video")
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
fun SuccessScreen(onAddWhatsApp: () -> Unit, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sticker Created Successfully!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddWhatsApp, modifier = Modifier.fillMaxWidth()) {
            Text("Add to WhatsApp")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Create Another")
        }
    }
}

@Composable
fun ErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
