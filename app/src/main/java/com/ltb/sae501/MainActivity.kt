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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.ltb.sae501.preferences.AppPreferences

class MainActivity : ComponentActivity() {

    private val codePermission = 100
    private val executeurCamera = Executors.newSingleThreadExecutor()
    private val executeurEmotion = Executors.newSingleThreadExecutor()
    private lateinit var detecteurEmotion: DualModelDetector

    private lateinit var dataSource: RemoteDataSource
    private lateinit var metadataManager: ModelMetadataManager
    private lateinit var repository: com.ltb.sae501.data.RecognitionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TokenManager.init(this)
        CameraPreferences.init(this)
        AppPreferences.init(this)

        dataSource = RemoteDataSource(this)
        detecteurEmotion = DualModelDetector(this)
        metadataManager = ModelMetadataManager(this)
        repository = com.ltb.sae501.data.RecognitionRepository.getInstance(this, dataSource)

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
            var isDarkTheme by remember { mutableStateOf(AppPreferences.isDarkModeEnabled()) }
            var isAuthenticated by remember { mutableStateOf(TokenManager.isLoggedIn()) }
            val coroutineScope = rememberCoroutineScope()
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
                                repository = repository,
                                isFrontCamera = isFrontCamera,
                                onCameraFlipped = { newValue ->
                                    isFrontCamera = newValue
                                    CameraPreferences.setFrontCamera(newValue)
                                }
                            )
                            Screen.History.route -> HistoryScreen(
                                repository = repository,
                                dataSource = dataSource
                            )
                            Screen.Profile.route -> ProfileScreen(
                                repository = repository,
                                onLogout = {
                                    TokenManager.clearToken()
                                    isAuthenticated = false
                                    currentScreen = Screen.Home.route
                                },
                                onNavigateToAuth = {
                                    currentScreen = "auth"
                                },
                                onNavigateToSettings = {
                                    currentScreen = "settings"
                                }
                            )
                            "category_management" -> {
                                if (TokenManager.isLoggedIn()) {
                                    CategoryManagementScreen(
                                        dataSource = dataSource,
                                        metadataManager = metadataManager,
                                        onNavigateBack = {
                                            currentScreen = Screen.Profile.route
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
                                            coroutineScope.launch {
                                                repository.syncAllPending()
                                            }
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
                                            coroutineScope.launch {
                                                repository.syncAllPending()
                                            }
                                            currentScreen = "model_training"
                                        }
                                    )
                                }
                            }
                            "auth" -> AuthScreen(
                                dataSource = dataSource,
                                onAuthSuccess = {
                                    isAuthenticated = true
                                    coroutineScope.launch {
                                        repository.syncAllPending()
                                    }
                                    currentScreen = Screen.Profile.route
                                }
                            )
                            "settings" -> SettingsScreen(
                                isDarkModeEnabled = isDarkTheme,
                                onSetDarkMode = { 
                                    isDarkTheme = it 
                                    AppPreferences.setDarkModeEnabled(it)
                                },
                                dataSource = dataSource,
                                onNavigateToCategoryManagement = {
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