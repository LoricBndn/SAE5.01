package com.ltb.sae501.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ltb.sae501.data.models.EmotionCategory
import com.ltb.sae501.network.RemoteDataSource
import com.ltb.sae501.ui.theme.AccentDelete
import com.ltb.sae501.ui.theme.AccentPink
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    dataSource: RemoteDataSource,
    metadataManager: com.ltb.sae501.ml.ModelMetadataManager,
    onNavigateBack: () -> Unit = {},
    onNavigateToTraining: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<EmotionCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<EmotionCategory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }
    var showAddImageDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var modelMetadata by remember { mutableStateOf<com.ltb.sae501.ml.ModelMetadata?>(null) }

    LaunchedEffect(Unit) {
        modelMetadata = metadataManager.loadMetadata()
    }

    LaunchedEffect(retryTrigger) {
        isLoading = true
        hasError = false
        dataSource.getCategories().collect { loadedCategories ->
            isLoading = false
            if (loadedCategories.isEmpty()) {
                hasError = true
            } else {
                categories = loadedCategories
                hasError = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion de l'IA", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            hasError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 64.sp
                        )
                        Text(
                            text = "Impossible de se connecter au serveur",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vérifiez que le serveur backend est démarré et accessible.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "Sur un appareil physique, assurez-vous d'être sur le même réseau WiFi que le serveur.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Réessayer", color = Color.White)
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                ) {
                    // Liste des catégories
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SmartToy,
                                                contentDescription = null,
                                                tint = AccentPink,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Text(
                                            text = "Modèle Personnalisé",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (modelMetadata != null) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("✓", fontSize = 20.sp, color = Color(0xFF4CAF50))
                                            Column {
                                                Text(
                                                    "Modèle entraîné (Précision: ${(modelMetadata!!.accuracy * 100).toInt()}%)",
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Dernière mise à jour: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(modelMetadata!!.trainingDate))}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = onNavigateToTraining,
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Réentraîner le Modèle", color = Color.White)
                                        }
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("⚠", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                "Aucun modèle personnalisé",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            "Uploadez au moins 10 images par catégorie puis entraînez votre modèle",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                        Button(
                                            onClick = onNavigateToTraining,
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Entraîner le Modèle", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        items(categories) { category ->
                            CategoryCard(
                                category = category,
                                onClick = {
                                    selectedCategory = category
                                    showAddImageDialog = true
                                },
                                onDeleteImage = { imageUrl ->
                                    coroutineScope.launch {
                                        val success = dataSource.deleteTrainingImage(category.id, imageUrl)
                                        if (success) {
                                            // Recharger les catégories après la suppression
                                            retryTrigger++
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Popup pour ajouter une image
    if (showAddImageDialog && selectedCategory != null) {
        AddImageDialog(
            category = selectedCategory!!,
            isUploading = isUploading,
            uploadError = uploadError,
            onDismiss = {
                showAddImageDialog = false
                isUploading = false
                uploadError = null
            },
            onImageSelected = { uri ->
                coroutineScope.launch {
                    isUploading = true
                    uploadError = null
                    val success = dataSource.uploadTrainingImage(
                        categoryId = selectedCategory!!.id,
                        imageUri = uri
                    )
                    isUploading = false
                    if (success) {
                        showAddImageDialog = false
                        // Recharger les catégories après l'ajout
                        retryTrigger++
                    } else {
                        uploadError = "Échec de l'upload. Vérifiez votre connexion internet."
                    }
                }
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: EmotionCategory,
    onClick: () -> Unit,
    onDeleteImage: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // En-tête de la catégorie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Emoji avec couleur de fond
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.emoji,
                            fontSize = 24.sp
                        )
                    }

                    Column {
                        Text(
                            text = category.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = category.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // Badge avec le nombre d'images
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${category.imageCount} images",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Galerie d'images miniatures
            if (category.trainingImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(category.trainingImages) { imageUrl ->
                        Box {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Image d'entraînement",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // Bouton de suppression
                            IconButton(
                                onClick = { onDeleteImage(imageUrl) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(AccentDelete.copy(alpha = 0.9f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Bouton "Ajouter une image"
                    item {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable(onClick = onClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Ajouter des images",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddImageDialog(
    category: EmotionCategory,
    isUploading: Boolean,
    uploadError: String?,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = category.emoji, fontSize = 24.sp)
                Text(text = "Ajouter une image - ${category.name}")
            }
        },
        text = {
            Column {
                Text(
                    "Ajoutez une image d'entraînement pour l'émotion ${category.name}.",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cette image sera utilisée pour améliorer la reconnaissance de cette émotion.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload en cours...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uploadError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uploadError,
                        fontSize = 12.sp,
                        color = AccentDelete,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Choisir une image", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Annuler", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}