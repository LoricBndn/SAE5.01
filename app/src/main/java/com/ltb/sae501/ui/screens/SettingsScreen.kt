package com.ltb.sae501.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltb.sae501.firebase.FirebaseDataSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataSource: FirebaseDataSource,
    onNavigateToCategoryManagement: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showInitDialog by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2A2A2A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Dataset
            Text(
                text = "Dataset FER-2013",
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Gestion des catégories
            SettingsItem(
                title = "Gestion des émotions",
                subtitle = "Gérer les images d'entraînement par émotion",
                icon = "🎭",
                onClick = onNavigateToCategoryManagement
            )

            // Initialiser les catégories
            SettingsItem(
                title = "Initialiser les catégories",
                subtitle = "Créer les 7 catégories d'émotions FER-2013",
                icon = "🔄",
                onClick = { showInitDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section Application
            Text(
                text = "Application",
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // À propos
            SettingsItem(
                title = "À propos",
                subtitle = "Version 1.0.0 - SAE 5.01",
                icon = "ℹ️",
                onClick = { /* TODO */ }
            )
        }
    }

    // Dialog de confirmation pour l'initialisation
    if (showInitDialog) {
        AlertDialog(
            onDismissRequest = { if (!isInitializing) showInitDialog = false },
            title = { Text("Initialiser les catégories") },
            text = {
                Column {
                    Text("Cette action va créer les 7 catégories d'émotions du dataset FER-2013 :")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 😠 Colère\n• 🤢 Dégoût\n• 😨 Peur\n• 😄 Joie\n• 😢 Tristesse\n• 😲 Surprise\n• 😐 Neutre")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Les catégories existantes ne seront pas modifiées.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (isInitializing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF18E06)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isInitializing = true
                        coroutineScope.launch {
                            val success = dataSource.initializeDefaultCategories()
                            isInitializing = false
                            showInitDialog = false
                            // TODO: Afficher un message de succès/échec
                        }
                    },
                    enabled = !isInitializing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF18E06)
                    )
                ) {
                    Text("Initialiser")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInitDialog = false },
                    enabled = !isInitializing
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
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
                    text = icon,
                    fontSize = 32.sp
                )

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFFB0B0B0),
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Ouvrir",
                tint = Color(0xFFB0B0B0)
            )
        }
    }
}