package com.tiktok.stickermaker.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.tiktok.stickermaker.stickers.StickerPackManager
import com.tiktok.stickermaker.ui.theme.NeonMagenta
import com.tiktok.stickermaker.ui.theme.TextPrimary
import com.tiktok.stickermaker.ui.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var stickers by remember { mutableStateOf(com.tiktok.stickermaker.utils.StickerStorage.getAllStickers(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
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
        containerColor = com.tiktok.stickermaker.ui.theme.DarkBg,
        floatingActionButton = {
            if (stickers.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { StickerPackManager.addStickerPackToWhatsApp(context) },
                    containerColor = NeonMagenta,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Import to WhatsApp") }
                )
            }
        }
    ) { padding ->
        if (stickers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your sticker library is empty", color = com.tiktok.stickermaker.ui.theme.TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stickers) { sticker ->
                    StickerItem(sticker) {
                        sticker.delete()
                        stickers = com.tiktok.stickermaker.utils.StickerStorage.getAllStickers(context)
                    }
                }
            }
        }
    }
}

@Composable
fun StickerItem(file: File, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .decoderFactory(if (android.os.Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
            }
        }
    }
}
