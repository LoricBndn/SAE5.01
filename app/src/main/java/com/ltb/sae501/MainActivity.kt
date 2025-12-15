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
import com.ltb.sae501.network.RemoteDataSource
import com.ltb.sae501.ui.screens.CategoryManagementScreen
import com.ltb.sae501.auth.TokenManager
import com.ltb.sae501.ml.ModelMetadataManager
import com.ltb.sae501.preferences.CameraPreferences
import com.ltb.sae501.ml.DualModelDetector

class MainActivity : ComponentActivity() {

    private val codePermission = 100
    private val executeurCamera = Executors.newSingleThreadExecutor()
    private val executeurEmotion = Executors.newSingleThreadExecutor()
    private lateinit var detecteurEmotion: DualModelDetector

    private lateinit var dataSource: RemoteDataSource
    private lateinit var metadataManager: ModelMetadataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialiser les préférences
        TokenManager.init(this)
        CameraPreferences.init(this)

        dataSource = RemoteDataSource(this)
        detecteurEmotion = DualModelDetector(this)
        metadataManager = ModelMetadataManager(this)

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
            var isAuthenticated by remember { mutableStateOf(TokenManager.isLoggedIn()) }
            // État de la caméra géré au niveau de MainActivity
            var isFrontCamera by remember { mutableStateOf(CameraPreferences.isFrontCamera()) }

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
                                dataSource = dataSource,
                                isFrontCamera = isFrontCamera,
                                onCameraFlipped = { newValue ->
                                    isFrontCamera = newValue
                                    CameraPreferences.setFrontCamera(newValue)
                                }
                            )
                            Screen.History.route -> HistoryScreen(
                                dataSource = dataSource
                            )
                            Screen.Settings.route -> SettingsScreen(
                                dataSource = dataSource,
                                onNavigateToCategoryManagement = {
                                    if (TokenManager.isLoggedIn()) {
                                        currentScreen = "category_management"
                                    } else {
                                        currentScreen = "auth"
                                    }
                                },
                                isDarkModeEnabled = isDarkTheme,
                                onSetDarkMode = { isDarkTheme = it }
                            )
                            "category_management" -> {
                                if (TokenManager.isLoggedIn()) {
                                    CategoryManagementScreen(
                                        dataSource = dataSource,
                                        metadataManager = metadataManager,
                                        onNavigateBack = {
                                            currentScreen = Screen.Settings.route
                                        },
                                        onNavigateToTraining = {
                                            currentScreen = "model_training"
                                        }
                                    )
                                } else {
                                    AuthScreen(
                                        dataSource = dataSource,
                                        onAuthSuccess = {
                                            isAuthenticated = true
                                            currentScreen = "category_management"
                                        }
                                    )
                                }
                            }
                            "model_training" -> {
                                if (TokenManager.isLoggedIn()) {
                                    TrainingScreen(
                                        dataSource = dataSource,
                                        metadataManager = metadataManager,
                                        onNavigateBack = {
                                            currentScreen = "category_management"
                                        }
                                    )
                                } else {
                                    AuthScreen(
                                        dataSource = dataSource,
                                        onAuthSuccess = {
                                            isAuthenticated = true
                                            currentScreen = "model_training"
                                        }
                                    )
                                }
                            }
                            "auth" -> AuthScreen(
                                dataSource = dataSource,
                                onAuthSuccess = {
                                    isAuthenticated = true
                                    currentScreen = "category_management"
                                }
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