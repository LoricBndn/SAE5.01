package com.ltb.sae501.config

import com.ltb.sae501.util.PythonProcessExecutor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TrainingConfig(
    private val trainingProps: TrainingConfigurationProperties
) {
    @Bean
    fun pythonProcessExecutor(): PythonProcessExecutor {
        return PythonProcessExecutor(
            pythonExecutable = trainingProps.python.executable,
            workingDirectory = trainingProps.python.scriptDir
        )
    }
}
