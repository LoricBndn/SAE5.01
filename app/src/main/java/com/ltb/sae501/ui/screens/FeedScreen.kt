package com.ltb.sae501.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ltb.sae501.BuildConfig
import com.ltb.sae501.R
import com.ltb.sae501.network.RemoteDataSource
import com.ltb.sae501.network.dto.FeedItemResponse
import com.ltb.sae501.network.dto.RecognizedEmotionDto

sealed class GuessState {
    object NotGuessed : GuessState()
    data class Guessed(
        val selectedEmotionId: String,
        val isCorrect: Boolean,
        val correctEmotionId: String,
        val correctConfidence: Float
    ) : GuessState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    dataSource: RemoteDataSource
) {
    val feedItems by dataSource.getFeed().collectAsState(initial = null)
    var categories by remember { mutableStateOf<List<com.ltb.sae501.data.models.EmotionCategory>>(emptyList()) }
    val isLoading = feedItems == null

    // mode jeu
    var isGameMode by remember { mutableStateOf(false) }
    var guessedItems by remember { mutableStateOf<Map<String, GuessState>>(emptyMap()) }
    var currentStreak by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        dataSource.getCategories().collect { cats ->
            categories = cats
        }
    }

    // reset au changement de mode
    LaunchedEffect(isGameMode) {
        guessedItems = emptyMap()
        currentStreak = 0
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Fil public",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (isGameMode && currentStreak >= 2) {
                                GameModeStreakIndicator(streak = currentStreak)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedModeToggle(
                            isGameMode = isGameMode,
                            onModeChange = { isGameMode = it }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                FeedLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            feedItems.isNullOrEmpty() -> {
                EmptyFeedState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(feedItems!!) { item ->
                        val itemId = "${item.id}"
                        val guessState = guessedItems[itemId] ?: GuessState.NotGuessed

                        FeedCard(
                            item = item,
                            categories = categories,
                            isGameMode = isGameMode,
                            guessState = guessState,
                            onGuess = { selectedEmotionId ->
                                val correctEmotion = item.recognizedEmotions.firstOrNull()
                                if (correctEmotion != null) {
                                    val isCorrect = selectedEmotionId.equals(correctEmotion.emotionId, ignoreCase = true)
                                    guessedItems = guessedItems + (itemId to GuessState.Guessed(
                                        selectedEmotionId = selectedEmotionId,
                                        isCorrect = isCorrect,
                                        correctEmotionId = correctEmotion.emotionId,
                                        correctConfidence = correctEmotion.confidence
                                    ))
                                    currentStreak = if (isCorrect) currentStreak + 1 else 0
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun FeedModeToggle(
    isGameMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (!isGameMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.clickable { onModeChange(false) }
            ) {
                Text(
                    text = stringResource(R.string.feed_mode_normal),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (!isGameMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isGameMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isGameMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.clickable { onModeChange(true) }
            ) {
                Text(
                    text = stringResource(R.string.feed_mode_guess),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isGameMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isGameMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun GameModeStreakIndicator(streak: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF6B35).copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "\uD83D\uDD25",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "$streak",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35)
            )
        }
    }
}

@Composable
fun EmotionGuessGrid(
    categories: List<com.ltb.sae501.data.models.EmotionCategory>,
    onGuess: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.guess_prompt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 4 cols first row, rest second
        val firstRow = categories.take(4)
        val secondRow = categories.drop(4)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            firstRow.forEach { category ->
                EmotionChip(
                    category = category,
                    onClick = { onGuess(category.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (secondRow.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                secondRow.forEach { category ->
                    EmotionChip(
                        category = category,
                        onClick = { onGuess(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // padding si < 4
                repeat(4 - secondRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun EmotionChip(
    category: com.ltb.sae501.data.models.EmotionCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = category.emoji,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GuessResultBanner(
    guessState: GuessState.Guessed,
    categories: List<com.ltb.sae501.data.models.EmotionCategory>
) {
    val correctCategory = categories.find { it.id.equals(guessState.correctEmotionId, ignoreCase = true) }
    val correctEmoji = correctCategory?.emoji ?: ""
    val correctName = correctCategory?.name ?: guessState.correctEmotionId

    val backgroundColor = if (guessState.isCorrect) {
        Color(0xFF4CAF50).copy(alpha = 0.2f)
    } else {
        Color(0xFFFF9800).copy(alpha = 0.2f)
    }

    val borderColor = if (guessState.isCorrect) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFFF9800)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (guessState.isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
                if (guessState.isCorrect) {
                    Text(
                        text = stringResource(R.string.guess_correct),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                    Text(
                        text = correctEmoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = stringResource(R.string.guess_incorrect, correctName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                    Text(
                        text = correctEmoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Text(
                text = "${(guessState.correctConfidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = borderColor
            )
        }
    }
}

@Composable
fun EmptyFeedState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Aucun scan partagé",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Soyez le premier à partager !",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun FeedCard(
    item: FeedItemResponse,
    categories: List<com.ltb.sae501.data.models.EmotionCategory>,
    isGameMode: Boolean = false,
    guessState: GuessState = GuessState.NotGuessed,
    onGuess: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = item.displayName ?: "Anonyme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatRelativeTime(item.detectedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // badge si c'est notre post
                if (item.isOwnPost) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Vous",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // img 3:4
            val imageUrl = buildImageUrl(item.imageUrl)
            AsyncImage(
                model = imageUrl,
                contentDescription = "Image partagée",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(0.dp)),
                contentScale = ContentScale.Crop
            )

            // gradient bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
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

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    !isGameMode -> {
                        Text(
                            text = "Emotions détectées",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        item.recognizedEmotions.take(3).forEachIndexed { index, emotion ->
                            FeedEmotionRow(
                                emotion = emotion,
                                categories = categories,
                                rank = index + 1
                            )
                        }
                    }
                    guessState is GuessState.NotGuessed -> {
                        EmotionGuessGrid(
                            categories = categories,
                            onGuess = onGuess
                        )
                    }
                    guessState is GuessState.Guessed -> {
                        GuessResultBanner(
                            guessState = guessState,
                            categories = categories
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Emotions détectées",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        item.recognizedEmotions.take(3).forEachIndexed { index, emotion ->
                            FeedEmotionRow(
                                emotion = emotion,
                                categories = categories,
                                rank = index + 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedEmotionRow(
    emotion: RecognizedEmotionDto,
    categories: List<com.ltb.sae501.data.models.EmotionCategory>,
    rank: Int
) {
    val category = categories.find { it.id.equals(emotion.emotionId, ignoreCase = true) }
    val emotionName = category?.name ?: emotion.emotionId
    val emoji = category?.emoji ?: ""

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

            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium
                )
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

    Spacer(modifier = Modifier.height(4.dp))

    LinearProgressIndicator(
        progress = { emotion.confidence },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = when (rank) {
            1 -> MaterialTheme.colorScheme.primary
            2 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        },
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
}

private fun buildImageUrl(relativeUrl: String): String {
    if (relativeUrl.startsWith("http")) {
        return relativeUrl
    }
    val baseUrl = BuildConfig.API_BASE_URL
        .removeSuffix("/api/")
        .removeSuffix("/")

    return "$baseUrl$relativeUrl"
}
