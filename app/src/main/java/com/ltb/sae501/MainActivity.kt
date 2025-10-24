package com.ltb.sae501

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ltb.sae501.ui.screens.HomeScreen
import com.ltb.sae501.ui.theme.SAE501Theme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val codePermission = 100
    private val executeurCamera = Executors.newSingleThreadExecutor()
    private val executeurEmotion = Executors.newSingleThreadExecutor()
    private lateinit var detecteurEmotion: EmotionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        detecteurEmotion = EmotionDetector(this)

        // Vérifier les permissions caméra
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
            SAE501Theme {
                var afficherCamera by remember { mutableStateOf(false) }

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (afficherCamera) {
                            EcranCamera(
                                detecteurEmotion = detecteurEmotion,
                                executeurEmotion = executeurEmotion
                            )
                        } else {
                            HomeScreen { afficherCamera = true }
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
