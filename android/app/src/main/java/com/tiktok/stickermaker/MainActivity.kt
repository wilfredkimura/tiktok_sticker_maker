@file:OptIn(ExperimentalMaterial3Api::class)
package com.tiktok.stickermaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
        is AppState.Logs -> LogsScreen(
            onBack = { viewModel.reset() }
        )
        is AppState.Error -> ErrorScreen(state.message) { viewModel.reset() }
    }
}

@Composable
fun HomeScreen(initialUrl: String, onProcess: (String) -> Unit, viewModel: StickerViewModel = viewModel()) {
    var url by remember { mutableStateOf(initialUrl) }
    val isConnected = viewModel.isBackendConnected

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("TikTok Sticker Maker") },
                actions = {
                    IconButton(onClick = { viewModel.showLogs() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.List,
                            contentDescription = "Logs"
                        )
                    }
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
fun LogsScreen(onBack: () -> Unit) {
    val logs = com.tiktok.stickermaker.utils.AppLogger.logs

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("App Logs", com.tiktok.stickermaker.utils.AppLogger.getFullLogs())
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Logs copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Share, // Share icon as "Copy" placeholder or use custom
                            contentDescription = "Copy Logs"
                        )
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
        ) {
            items(logs) { entry ->
                val color = when (entry.level) {
                    com.tiktok.stickermaker.utils.LogLevel.ERROR -> Color.Red
                    com.tiktok.stickermaker.utils.LogLevel.DEBUG -> Color.Gray
                    else -> Color.Black
                }
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[${entry.timestamp}] ${entry.tag ?: ""}",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = entry.message,
                                    color = color,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                entry.source?.let {
                                    Text(
                                        text = "at $it",
                                        color = Color.Gray.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Log Line", entry.message)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Line copied", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                    contentDescription = "Copy Line",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(top = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
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
