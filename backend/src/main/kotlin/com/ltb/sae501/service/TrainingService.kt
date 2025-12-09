package com.ltb.sae501.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ltb.sae501.config.TrainingConfigurationProperties
import com.ltb.sae501.dto.CategoryImagesDto
import com.ltb.sae501.dto.ImageDataDto
import com.ltb.sae501.dto.TrainingImagesResponse
import com.ltb.sae501.dto.TrainingStatusResponse
import com.ltb.sae501.entity.CustomModel
import com.ltb.sae501.repository.CustomModelRepository
import com.ltb.sae501.repository.TrainingImageRepository
import com.ltb.sae501.repository.UserRepository
import com.ltb.sae501.util.PythonProcessExecutor
import com.ltb.sae501.util.TrainingDataSerializer
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class TrainingService(
    private val categoryService: CategoryService,
    private val trainingImageRepository: TrainingImageRepository,
    private val customModelRepository: CustomModelRepository,
    private val userRepository: UserRepository,
    private val trainingConfig: TrainingConfigurationProperties,
    private val pythonExecutor: PythonProcessExecutor
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()
    private val trainingStatusMap = ConcurrentHashMap<String, TrainingStatusResponse>()

    fun getAllTrainingImagesForUser(userId: String): TrainingImagesResponse {
        val imagesByCategory = categoryService.getAllTrainingImagesForUser(userId)

        val categories = imagesByCategory.map { (category, images) ->
            CategoryImagesDto(
                categoryId = category.id,
                categoryName = category.nameEn,
                images = images.map { image ->
                    ImageDataDto(
                        id = image.id,
                        data = image.imageData
                    )
                }
            )
        }

        return TrainingImagesResponse(categories = categories)
    }

    @Async
    @Transactional
    fun startTraining(userId: String): String {
        val jobId = UUID.randomUUID().toString()

        trainingStatusMap[userId] = TrainingStatusResponse(
            status = "preparing",
            progress = 0.0f,
            currentEpoch = 0,
            totalEpochs = 15,
            accuracy = 0.0f,
            errorMessage = null
        )

        try {
            updateStatus(userId, "training", 0.1f, 1, 15, 0.0f, null)

            val modelBytes = executeTraining(userId)

            saveCustomModel(userId, modelBytes)

            updateStatus(userId, "completed", 1.0f, 15, 15, 0.85f, null)

        } catch (e: Exception) {
            updateStatus(userId, "failed", 0.0f, 0, 15, 0.0f, e.message)
        }

        return jobId
    }

    fun getTrainingStatus(userId: String): TrainingStatusResponse? {
        return trainingStatusMap[userId]
    }

    fun downloadCustomModel(userId: String): ByteArray? {
        val customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
        return customModel?.modelData
    }

    @Transactional
    fun uploadCustomModel(userId: String, modelData: ByteArray, metadataJson: String): CustomModel {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        val version = (customModelRepository.countByUserId(userId) + 1)

        val customModel = CustomModel(
            id = UUID.randomUUID().toString(),
            user = user,
            modelData = modelData,
            version = version,
            accuracy = 0.0f,
            trainingImageCount = 0,
            createdAt = System.currentTimeMillis(),
            metadataJson = metadataJson
        )

        return customModelRepository.save(customModel)
    }

    private fun updateStatus(
        userId: String,
        status: String,
        progress: Float,
        currentEpoch: Int,
        totalEpochs: Int,
        accuracy: Float,
        errorMessage: String?
    ) {
        trainingStatusMap[userId] = TrainingStatusResponse(
            status = status,
            progress = progress,
            currentEpoch = currentEpoch,
            totalEpochs = totalEpochs,
            accuracy = accuracy,
            errorMessage = errorMessage
        )
    }

    private fun executeTraining(userId: String): ByteArray {
        updateStatus(userId, "preparing", 0.05f, 0, 15, 0.0f, null)

        val (available, version) = pythonExecutor.checkPythonAvailability()
        if (!available) {
            throw IllegalStateException("Python non trouvé : ${trainingConfig.python.executable}")
        }
        logger.info("Python version: $version")

        val imagesByCategory = categoryService.getAllTrainingImagesForUser(userId)
        val totalImages = imagesByCategory.values.sumOf { it.size }

        if (totalImages < trainingConfig.minImagesPerCategory * 7) {
            throw IllegalArgumentException(
                "Données insuffisantes: $totalImages images (minimum ${trainingConfig.minImagesPerCategory} par catégorie)"
            )
        }
        logger.info("$totalImages images chargées")

        updateStatus(userId, "preparing_data", 0.1f, 0, 15, 0.0f, null)
        val jsonFile = TrainingDataSerializer.serializeToJsonFile(imagesByCategory)

        try {
            val args = listOf(userId, jsonFile.absolutePath)

            updateStatus(userId, "training", 0.15f, 1, 15, 0.0f, null)
            val result = pythonExecutor.execute(
                scriptName = trainingConfig.python.scriptName,
                args = args,
                timeoutMs = trainingConfig.python.timeout,
                progressCallback = { line -> parseProgressUpdate(userId, line) }
            )

            if (result.timedOut) {
                throw IllegalStateException("Training timeout après ${trainingConfig.python.timeout}ms")
            }

            if (result.exitCode != 0) {
                logger.error("Training échoué : ${result.stderr}")
                throw IllegalStateException("Training échoué : ${result.stderr.take(200)}")
            }

            val jsonOutput = result.stdout.lines().lastOrNull { it.trim().startsWith("{") }
                ?: throw IllegalStateException("Pas de JSON dans output Python")

            val pythonResult = objectMapper.readValue(jsonOutput, PythonTrainingResult::class.java)
            if (!pythonResult.success) {
                throw IllegalStateException("Python a reporté un échec")
            }

            logger.info("Training terminé : accuracy=${pythonResult.accuracy}")

            updateStatus(userId, "saving_model", 0.95f, 15, 15, pythonResult.accuracy.toFloat(), null)
            val modelFile = File(trainingConfig.python.scriptDir, pythonResult.modelPath)

            if (!modelFile.exists()) {
                throw IllegalStateException("Fichier modèle non trouvé : ${modelFile.absolutePath}")
            }

            val modelBytes = modelFile.readBytes()

            modelFile.delete()
            logger.info("Fichier temporaire supprimé")

            return modelBytes

        } finally {
            jsonFile.delete()
        }
    }

    private fun parseProgressUpdate(userId: String, line: String) {
        try {
            val epochRegex = """Epoch\s+(\d+)/(\d+)""".toRegex()
            val epochMatch = epochRegex.find(line) ?: return

            val current = epochMatch.groupValues[1].toInt()
            val total = epochMatch.groupValues[2].toInt()
            val progress = 0.15f + (current.toFloat() / total * 0.8f)

            val accRegex = """val_accuracy:\s+([\d.]+)""".toRegex()
            val accuracy = accRegex.find(line)?.groupValues?.get(1)?.toFloat() ?: 0.0f

            updateStatus(userId, "training", progress, current, total, accuracy, null)
        } catch (e: Exception) {
        }
    }

    data class PythonTrainingResult(
        val success: Boolean,
        val accuracy: Double,
        @com.fasterxml.jackson.annotation.JsonProperty("model_path")
        val modelPath: String,
        @com.fasterxml.jackson.annotation.JsonProperty("model_size")
        val modelSize: Int
    )

    @Transactional
    private fun saveCustomModel(userId: String, modelData: ByteArray) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        val imageCount = trainingImageRepository.countByCategoryIdAndUserId("", userId)
        val version = (customModelRepository.countByUserId(userId) + 1)

        val customModel = CustomModel(
            id = UUID.randomUUID().toString(),
            user = user,
            modelData = modelData,
            version = version,
            accuracy = 0.85f,
            trainingImageCount = imageCount,
            createdAt = System.currentTimeMillis(),
            metadataJson = "{\"epochs\": 15, \"batchSize\": 16}"
        )

        customModelRepository.save(customModel)
    }
}
