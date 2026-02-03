package com.ltb.sae501.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Accueil")
    object Camera : Screen("camera", "Caméra")
    object Feed : Screen("feed", "Fil")
    object History : Screen("history", "Historique")
    object Profile : Screen("profile", "Profil")
}