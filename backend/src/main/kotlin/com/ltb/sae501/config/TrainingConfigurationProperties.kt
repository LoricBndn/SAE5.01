package com.ltb.sae501.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "training")
data class TrainingConfigurationProperties(
    var python: PythonConfig = PythonConfig(),
    var maxImages: Int = 10000,
    var minImagesPerCategory: Int = 5
)

data class PythonConfig(
    var executable: String = "python",
    var timeout: Long = 1800000,
    var scriptDir: String = "./training",
    var scriptName: String = "train_custom_model.py"
)
