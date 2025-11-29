package com.ltb.sae501.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltb.sae501.network.RemoteDataSource
import com.ltb.sae501.ui.theme.AccentPink
import com.ltb.sae501.ui.theme.AccentDelete
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isDarkModeEnabled: Boolean,
    onSetDarkMode: (Boolean) -> Unit,
    dataSource: RemoteDataSource,
    onNavigateToCategoryManagement: () -> Unit = {}
) {
    var isPercentageShown by remember { mutableStateOf(true) }
    var isAutoSaveEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .systemBarsPadding(),
    ) {
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsSwitchItem(
                title = "Thème sombre",
                subtitle = "Mode nuit",
                icon = Icons.Outlined.DarkMode,
                iconTint = MaterialTheme.colorScheme.primary,
                checked = isDarkModeEnabled,
                onCheckedChange = onSetDarkMode
            )


            SettingsSwitchItem(
                title = "Afficher le pourcentage",
                subtitle = "Précision affichée",
                icon = Icons.Filled.Percent,
                iconTint = MaterialTheme.colorScheme.primary,
                checked = isPercentageShown,
                onCheckedChange = { isPercentageShown = it }
            )

            SettingsSwitchItem(
                title = "Sauvegarde automatique",
                subtitle = "Enregistrement auto",
                icon = Icons.Outlined.PhotoCamera,
                iconTint = MaterialTheme.colorScheme.primary,
                checked = isAutoSaveEnabled,
                onCheckedChange = { isAutoSaveEnabled = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsActionItem(
                title = "Modifier l'IA",
                subtitle = "Images d'entraînement",
                icon = Icons.Outlined.Assessment,
                iconTint = AccentPink,
                buttonText = "Gérer",
                buttonColor = AccentPink,
                onClick = onNavigateToCategoryManagement
            )

            SettingsDangerItem(
                title = "Supprimer les données",
                subtitle = "Réinitialiser l'app",
                icon = Icons.Filled.Delete,
                iconTint = AccentDelete,
                buttonText = "Supprimer",
                onClick = { showDeleteDialog = true }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer toutes les données?") },
            text = { Text("Ceci inclut votre historique et vos configurations. Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Logique de suppression ici
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDelete)
                ) {
                    Text("Confirmer la suppression")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconAndText(title, subtitle, icon, iconTint)

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0),
                    uncheckedThumbColor = Color.White
                )
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Blanc
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconAndText(title, subtitle, icon, iconTint, modifier = Modifier.weight(1f))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsDangerItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Blanc
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconAndText(title, subtitle, icon, iconTint, modifier = Modifier.weight(1f))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentDelete), // Couleur Rouge
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}

@Composable
private fun SettingsIconAndText(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}