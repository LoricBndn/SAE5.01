package com.ltb.sae501.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ltb.sae501.ui.navigation.Screen

@Composable
fun BottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF2A2A2A),
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = {
                Text(
                    text = "🏠",
                    fontSize = 24.sp
                )
            },
            label = {
                Text(
                    text = Screen.Home.title,
                    fontWeight = if (currentScreen == Screen.Home.route) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFF18E06),
                selectedTextColor = Color(0xFFF18E06),
                unselectedIconColor = Color(0xFFB0B0B0),
                unselectedTextColor = Color(0xFFB0B0B0),
                indicatorColor = Color(0xFF3A3A3A)
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.Camera.route,
            onClick = { onNavigate(Screen.Camera.route) },
            icon = {
                Text(
                    text = "📷",
                    fontSize = 24.sp
                )
            },
            label = {
                Text(
                    text = Screen.Camera.title,
                    fontWeight = if (currentScreen == Screen.Camera.route) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFF18E06),
                selectedTextColor = Color(0xFFF18E06),
                unselectedIconColor = Color(0xFFB0B0B0),
                unselectedTextColor = Color(0xFFB0B0B0),
                indicatorColor = Color(0xFF3A3A3A)
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.History.route,
            onClick = { onNavigate(Screen.History.route) },
            icon = {
                Text(
                    text = "📋",
                    fontSize = 24.sp
                )
            },
            label = {
                Text(
                    text = Screen.History.title,
                    fontWeight = if (currentScreen == Screen.History.route) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFF18E06),
                selectedTextColor = Color(0xFFF18E06),
                unselectedIconColor = Color(0xFFB0B0B0),
                unselectedTextColor = Color(0xFFB0B0B0),
                indicatorColor = Color(0xFF3A3A3A)
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = {
                Text(
                    text = "⚙️",
                    fontSize = 24.sp
                )
            },
            label = {
                Text(
                    text = Screen.Settings.title,
                    fontWeight = if (currentScreen == Screen.Settings.route) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFF18E06),
                selectedTextColor = Color(0xFFF18E06),
                unselectedIconColor = Color(0xFFB0B0B0),
                unselectedTextColor = Color(0xFFB0B0B0),
                indicatorColor = Color(0xFF3A3A3A)
            )
        )
    }
}