package com.ltb.sae501.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltb.sae501.data.models.EmotionCategory
import com.ltb.sae501.ml.ModelMetadata
import com.ltb.sae501.ml.ModelMetadataManager
import com.ltb.sae501.network.RemoteDataSource
import com.ltb.sae501.network.dto.TrainingStatusResponse
import com.ltb.sae501.ui.theme.AccentPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    dataSource: RemoteDataSource,
    metadataManager: ModelMetadataManager,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<EmotionCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showTrainingDialog by remember { mutableStateOf(false) }
    var trainingStatus by remember { mutableStateOf<TrainingStatusResponse?>(null) }
    var isTraining by remember { mutableStateOf(false) }
    var modelMetadata by remember { mutableStateOf<ModelMetadata?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val minImagesPerCategory = 10

    LaunchedEffect(Unit) {
        modelMetadata = metadataManager.loadMetadata()

        dataSource.getCategories().collect { categoriesList ->
            if (categoriesList.isEmpty()) {
                errorMessage = "Erreur de chargement des catégories"
            } else {
                categories = categoriesList
            }
            isLoading = false
        }
    }

    val insufficientCategories = categories.filter { it.imageCount < minImagesPerCategory }
    val canTrain = insufficientCategories.isEmpty() && categories.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entraînement du Modèle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Statut du Modèle",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (modelMetadata != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                                Column {
                                    Text(
                                        "Modèle personnalisé entraîné",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Précision : ${(modelMetadata!!.accuracy * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "${modelMetadata!!.trainingImageCount} images d'entraînement",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF18E06)
                                )
                                Text(
                                    "Aucun modèle personnalisé",
                                    color = Color(0xFFF18E06),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Exigences d'Entraînement",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Minimum $minImagesPerCategory images par catégorie",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            items(categories) { category ->
                CategoryProgressCard(category, minImagesPerCategory)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        showTrainingDialog = true
                        coroutineScope.launch {
                            isTraining = true
                            try {
                                val startResponse = dataSource.startTraining()

                                while (isTraining) {
                                    delay(3000)

                                    val statusResponse = dataSource.getTrainingStatus()
                                    trainingStatus = statusResponse

                                    if (statusResponse?.status == "completed") {
                                        val modelData = dataSource.downloadCustomModel()
                                        if (modelData != null) {
                                            metadataManager.saveModelFile(modelData)

                                            val newMetadata = ModelMetadata(
                                                version = (modelMetadata?.version ?: 0) + 1,
                                                userId = "",
                                                trainingDate = System.currentTimeMillis(),
                                                accuracy = statusResponse.accuracy,
                                                trainingImageCount = categories.sumOf { it.imageCount },
                                                epochsTrained = statusResponse.totalEpochs,
                                                categories = categories.associate { it.id to it.imageCount }
                                            )
                                            metadataManager.saveMetadata(newMetadata)
                                            modelMetadata = newMetadata
                                        }
                                        isTraining = false
                                    } else if (statusResponse?.status == "failed") {
                                        errorMessage = statusResponse.errorMessage ?: "Erreur inconnue"
                                        isTraining = false
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message
                                isTraining = false
                            }
                        }
                    },
                    enabled = canTrain && !isTraining,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPink,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isTraining) "Entraînement en cours..."
                               else if (modelMetadata != null) "Réentraîner le Modèle"
                               else "Entraîner le Modèle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!canTrain && !isLoading) {
                    Text(
                        text = "Ajoutez au moins $minImagesPerCategory images par catégorie pour commencer",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showTrainingDialog && trainingStatus != null) {
        TrainingProgressDialog(
            status = trainingStatus!!,
            onDismiss = {
                if (!isTraining) {
                    showTrainingDialog = false
                    trainingStatus = null
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Erreur") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CategoryProgressCard(category: EmotionCategory, minImages: Int) {
    val progress = (category.imageCount.toFloat() / minImages).coerceAtMost(1f)
    val isSufficient = category.imageCount >= minImages

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .background(
                            Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )

                Column {
                    Text(
                        text = category.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${category.imageCount} / $minImages images",
                        color = if (isSufficient) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(80.dp)
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (isSufficient) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TrainingProgressDialog(status: TrainingStatusResponse, onDismiss: () -> Unit) {
    val progress = status.progress.coerceIn(0f, 1f)

    AlertDialog(
        onDismissRequest = { if (status.status == "completed" || status.status == "failed") onDismiss() },
        title = {
            Text(
                text = when (status.status) {
                    "completed" -> "✓ Entraînement Terminé"
                    "failed" -> "✗ Échec de l'Entraînement"
                    else -> "Entraînement en cours..."
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (status.status == "training" || status.status == "preparing") {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("Epoch ${status.currentEpoch} / ${status.totalEpochs}")

                    if (status.accuracy > 0) {
                        Text("Précision : ${(status.accuracy * 100).toInt()}%")
                    }

                    Text("Progression : ${(progress * 100).toInt()}%")
                } else if (status.status == "completed") {
                    Text("Le modèle a été entraîné avec succès !")
                    Text("Précision finale : ${(status.accuracy * 100).toInt()}%")
                } else if (status.status == "failed") {
                    Text("Erreur : ${status.errorMessage ?: "Erreur inconnue"}")
                }
            }
        },
        confirmButton = {
            if (status.status == "completed" || status.status == "failed") {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    )
}
