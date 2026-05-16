@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
package com.tiktok.stickermaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.tiktok.stickermaker.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiktok.stickermaker.stickers.StickerPackManager
import com.tiktok.stickermaker.ui.screens.EditorScreen
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DownloadDone
import com.tiktok.stickermaker.ui.theme.TikTokStickerMakerTheme
import com.tiktok.stickermaker.ui.theme.DarkBg
import com.tiktok.stickermaker.ui.theme.NeonCyan
import com.tiktok.stickermaker.ui.theme.NeonMagenta
import com.tiktok.stickermaker.ui.theme.SurfaceDark
import com.tiktok.stickermaker.ui.theme.TextPrimary
import com.tiktok.stickermaker.ui.theme.TextSecondary
import com.tiktok.stickermaker.ui.viewmodel.AppState
import com.tiktok.stickermaker.ui.viewmodel.StickerViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
            TikTokStickerMakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    MainApp(initialUrl)
                }
            }
        }
    }
}

@Composable
fun MainApp(initialUrl: String, viewModel: StickerViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isConnected = viewModel.isBackendConnected

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.background(DarkBg)
            ) {
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.padding(16.dp).size(80.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceDark)) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    "TikTok Sticker Maker",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    selected = viewModel.state is AppState.Home,
                    onClick = { 
                        viewModel.reset()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = NeonCyan.copy(alpha = 0.1f),
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, null) },
                    label = { Text("My Stickers") },
                    selected = viewModel.state is AppState.Library,
                    onClick = { 
                        viewModel.showLibrary()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = NeonCyan.copy(alpha = 0.1f),
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, null) },
                    label = { Text("App Logs") },
                    selected = viewModel.state is AppState.Logs,
                    onClick = { 
                        viewModel.showLogs()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = NeonCyan.copy(alpha = 0.1f),
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("StickerMaker", fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isConnected) Color.Green else Color.Red,
                                        shape = CircleShape
                                    )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg,
                        titleContentColor = NeonCyan,
                        navigationIconContentColor = NeonCyan
                    )
                )
            },
            containerColor = DarkBg
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(
                    targetState = viewModel.state,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                    }
                ) { targetState ->
                    when (val currentState = targetState) {
                        is AppState.Home -> HomeScreen(
                            initialUrl = initialUrl,
                            onProcess = { viewModel.processVideo(it) }
                        )
                        is AppState.Loading -> LoadingScreen("Resolving Video...")
                        is AppState.Downloaded -> DownloadedScreen(
                            videoFile = currentState.videoFile,
                            onSaveToGallery = { viewModel.saveVideoToGallery(it) },
                            onCreateSticker = { viewModel.startEditing(it) }
                        )
                        is AppState.Editor -> EditorScreen(
                            videoFile = currentState.videoFile,
                            onCreateSticker = { start, duration -> viewModel.createSticker(currentState.videoFile, start, duration) },
                            onBack = { viewModel.reset() }
                        )
                        is AppState.Exporting -> LoadingScreen("Generating Sticker...")
                        is AppState.Success -> SuccessScreen(
                            onAddWhatsApp = { viewModel.addToWhatsApp() },
                            onDone = { viewModel.reset() }
                        )
                        is AppState.Library -> com.tiktok.stickermaker.ui.screens.LibraryScreen(
                            onBack = { viewModel.reset() }
                        )
                        is AppState.Logs -> LogsScreen(onBack = { viewModel.reset() })
                        is AppState.Error -> ErrorScreen(currentState.message) { viewModel.reset() }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(initialUrl: String, onProcess: (String) -> Unit) {
    var url by remember { mutableStateOf(initialUrl) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(200.dp).padding(bottom = 32.dp)
        )
        
        Text(
            "TikTok Video to Sticker",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Paste TikTok Link") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                focusedLabelColor = NeonCyan,
                unfocusedBorderColor = Color.Gray,
                cursorColor = NeonCyan
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onProcess(url) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Download & Resolve", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DownloadedScreen(
    videoFile: File,
    onSaveToGallery: (File) -> Unit,
    onCreateSticker: (File) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(80.dp), tint = NeonCyan)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Video Ready!", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { onCreateSticker(videoFile) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Icon(Icons.Default.Movie, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("Create Sticker", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { onSaveToGallery(videoFile) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
        ) {
            Icon(Icons.Default.Save, null, tint = NeonCyan)
            Spacer(Modifier.width(8.dp))
            Text("Save Video", color = NeonCyan)
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(120.dp).graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = NeonCyan)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = NeonCyan)
        }
    }
}

@Composable
fun SuccessScreen(onAddWhatsApp: () -> Unit, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Green)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Sticker Pack Ready!", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onAddWhatsApp,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("Add to WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
        ) {
            Text("Create More", color = NeonCyan)
        }
    }
}

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val logs = com.tiktok.stickermaker.utils.AppLogger.logs
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("App Logs", com.tiktok.stickermaker.utils.AppLogger.getFullLogs())
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Logs copied", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = NeonCyan, navigationIconContentColor = NeonCyan, actionIconContentColor = NeonCyan)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)
        ) {
            items(logs) { entry ->
                val color = when (entry.level) {
                    com.tiktok.stickermaker.utils.LogLevel.ERROR -> Color.Red
                    com.tiktok.stickermaker.utils.LogLevel.DEBUG -> Color.Gray
                    else -> TextPrimary
                }
                Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                    Text(
                        text = "[${entry.timestamp}] ${entry.tag ?: ""}",
                        color = TextSecondary,
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
                            color = TextSecondary.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Divider(modifier = Modifier.padding(top = 4.dp), color = Color.Gray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Red)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Oops! Something went wrong", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
            Text("Try Again", color = Color.Black)
        }
    }
}
