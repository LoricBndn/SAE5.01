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
import androidx.compose.material.icons.filled.Delete
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
import com.ltb.sae501.firebase.FirebaseDataSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(dataSource: FirebaseDataSource) {
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<EmotionCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<EmotionCategory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddImageDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // Charger les catégories
    LaunchedEffect(Unit) {
        dataSource.getCategories().collect { loadedCategories ->
            categories = loadedCategories
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion des Émotions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2A2A2A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFF18E06))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(paddingValues)
            ) {
                // Liste des catégories
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = {
                                selectedCategory = category
                                showAddImageDialog = true
                            },
                            onDeleteImage = { imageUrl ->
                                coroutineScope.launch {
                                    dataSource.deleteTrainingImage(category.id, imageUrl)
                                }
                            }
                        )
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
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(16.dp)
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
                            .size(50.dp)
                            .background(
                                Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.emoji,
                            fontSize = 28.sp
                        )
                    }

                    Column {
                        Text(
                            text = category.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = category.description,
                            color = Color(0xFFB0B0B0),
                            fontSize = 12.sp
                        )
                    }
                }

                // Badge avec le nombre d'images
                Surface(
                    color = Color(0xFFF18E06),
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
                                    .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // Bouton de suppression
                            IconButton(
                                onClick = { onDeleteImage(imageUrl) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
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
                                .border(2.dp, Color(0xFFF18E06), RoundedCornerShape(8.dp))
                                .background(Color(0xFF3A3A3A))
                                .clickable(onClick = onClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter",
                                tint = Color(0xFFF18E06),
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
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFFF18E06), RoundedCornerShape(8.dp))
                        .background(Color(0xFF3A3A3A))
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
                            tint = Color(0xFFF18E06),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Ajouter des images",
                            color = Color(0xFFB0B0B0),
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
                Text("Ajoutez une image d'entraînement pour l'émotion ${category.name}.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cette image sera utilisée pour améliorer la reconnaissance de cette émotion.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF18E06)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload en cours...",
                        fontSize = 12.sp,
                        color = Color(0xFFF18E06)
                    )
                }

                if (uploadError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uploadError,
                        fontSize = 12.sp,
                        color = Color.Red,
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
                    containerColor = Color(0xFFF18E06)
                )
            ) {
                Text("Choisir une image")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Annuler")
            }
        }
    )
}