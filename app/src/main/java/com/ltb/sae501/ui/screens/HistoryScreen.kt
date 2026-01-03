package com.ltb.sae501.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import android.util.Log
import com.ltb.sae501.data.models.RecognitionResult
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.network.RemoteDataSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: com.ltb.sae501.data.RecognitionRepository,
    dataSource: RemoteDataSource
) {
    val history by repository.getHistory().collectAsState(initial = emptyList())
    val unsyncedCount by repository.getUnsyncedCount().collectAsState(initial = 0)
    val isLoggedIn = com.ltb.sae501.auth.TokenManager.isLoggedIn()
    
    var categories by remember { mutableStateOf<List<com.ltb.sae501.data.models.EmotionCategory>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        dataSource.getCategories().collect { cats ->
            categories = cats
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<RecognitionResult?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(syncMessage) {
        syncMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            syncMessage = null
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Historique des reconnaissances",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    if (isLoggedIn && unsyncedCount > 0) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isSyncing = true
                                    syncMessage = null
                                    val syncedCount = repository.syncAllPending()
                                    isSyncing = false
                                    syncMessage = if (syncedCount > 0) {
                                        "$syncedCount photo${if (syncedCount > 1) "s" else ""} synchronisée${if (syncedCount > 1) "s" else ""}"
                                    } else {
                                        "Échec de la synchronisation"
                                    }
                                }
                            },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = androidx.compose.ui.Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                androidx.compose.material.icons.Icons.Default.Sync.let { icon ->
                                    androidx.compose.foundation.layout.Box {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Synchroniser",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        if (unsyncedCount > 0) {
                                            androidx.compose.material3.Badge(
                                                modifier = androidx.compose.ui.Modifier.align(Alignment.TopEnd)
                                            ) {
                                                Text("$unsyncedCount")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (history.isEmpty()) {
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(history) { result ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ModernHistoryCard(
                            result = result,
                            categories = categories,
                            onDelete = { showDeleteDialog = result }
                        )
                    }
                }
            }
        }
        
        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Supprimer cette reconnaissance ?") },
                text = { Text("Cette action est irréversible et supprimera l'image de votre appareil.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                showDeleteDialog?.let { item ->
                                    repository.deleteRecognition(item.id, item.imageLocalPath)
                                }
                                showDeleteDialog = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Aucune reconnaissance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Commencez par détecter des émotions !",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ModernHistoryCard(
    result: RecognitionResult,
    categories: List<com.ltb.sae501.data.models.EmotionCategory>,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            Column(modifier = Modifier.padding(20.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        modifier = Modifier.size(110.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = result.imageLocalPath,
                            contentDescription = "Image reconnue",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatRelativeTime(result.detectedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Émotions détectées",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.recognizedEmotions.take(3).forEachIndexed { index, emotion ->
                        EmotionProgressBar(
                            emotion = emotion,
                            categories = categories,
                            rank = index + 1
                        )
                    }

                    if (result.recognizedEmotions.size > 3) {
                        Text(
                            text = "+${result.recognizedEmotions.size - 3} autres",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionProgressBar(
    emotion: RecognizedEmotion,
    categories: List<com.ltb.sae501.data.models.EmotionCategory>,
    rank: Int
) {
    val emotionName = categories.find { it.id == emotion.emotionId }?.name 
        ?: emotion.emotionId
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when (rank) {
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = emotionName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = "${(emotion.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    1 -> MaterialTheme.colorScheme.primary
                    2 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        LinearProgressIndicator(
            progress = emotion.confidence,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when (rank) {
                1 -> MaterialTheme.colorScheme.primary
                2 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    }
}

/**
 * Formate un timestamp en temps relatif (ex: "Il y a 2h", "Aujourd'hui", etc.)
 */
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    
    return when {
        seconds < 60 -> "À l'instant"
        minutes < 60 -> "Il y a ${minutes}min"
        hours < 24 -> "Il y a ${hours}h"
        days == 1L -> "Hier"
        days < 7 -> "Il y a ${days} jours"
        else -> {
            SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(timestamp))
        }
    }
}