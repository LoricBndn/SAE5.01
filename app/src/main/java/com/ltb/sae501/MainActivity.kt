package com.ltb.sae501

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ltb.sae501.ui.components.BottomNavBar
import com.ltb.sae501.ui.navigation.Screen
import com.ltb.sae501.ui.screens.*
import com.ltb.sae501.ui.theme.SAE501Theme
import java.util.concurrent.Executors
import com.ltb.sae501.firebase.FirebaseDataSource
import com.ltb.sae501.ui.screens.CategoryManagementScreen

class MainActivity : ComponentActivity() {

    private val codePermission = 100
    private val executeurCamera = Executors.newSingleThreadExecutor()
    private val executeurEmotion = Executors.newSingleThreadExecutor()
    private lateinit var detecteurEmotion: EmotionDetector

    private val firebaseDataSource = FirebaseDataSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        detecteurEmotion = EmotionDetector(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                codePermission
            )
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            SAE501Theme(darkTheme = isDarkTheme) {
                var currentScreen by remember { mutableStateOf(Screen.Home.route) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(
                            currentScreen = currentScreen,
                            onNavigate = { route -> currentScreen = route }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentScreen) {
                            Screen.Home.route -> HomeScreen(
                                onClickDetection = { currentScreen = Screen.Camera.route }
                            )
                            Screen.Camera.route -> CameraScreen(
                                detecteurEmotion = detecteurEmotion,
                                executeurEmotion = executeurEmotion,
                                dataSource = firebaseDataSource
                            )
                            Screen.History.route -> HistoryScreen(
                                dataSource = firebaseDataSource
                            )
                            Screen.Settings.route -> SettingsScreen(
                                dataSource = firebaseDataSource,
                                onNavigateToCategoryManagement = {
                                    currentScreen = "category_management"
                                },
                                isDarkModeEnabled = isDarkTheme,
                                onSetDarkMode = { isDarkTheme = it }
                            )
                            "category_management" -> CategoryManagementScreen(
                                dataSource = firebaseDataSource
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executeurCamera.shutdown()
        executeurEmotion.shutdown()
        detecteurEmotion.fermer()
    }
}