package com.ltb.sae501.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class NavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun BottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem("home", Icons.Default.Home, "Accueil"),
        NavItem("camera", Icons.Outlined.PhotoCamera, "Caméra"),
        NavItem("history", Icons.Outlined.History, "Historique"),
        NavItem("settings", Icons.Default.Settings, "Paramètres")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}