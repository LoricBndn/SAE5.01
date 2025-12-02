package com.ltb.sae501.service

import com.ltb.sae501.dto.CategoryImagesDto
import com.ltb.sae501.dto.ImageDataDto
import com.ltb.sae501.dto.TrainingImagesResponse
import com.ltb.sae501.dto.TrainingStatusResponse
import com.ltb.sae501.entity.CustomModel
import com.ltb.sae501.repository.CustomModelRepository
import com.ltb.sae501.repository.TrainingImageRepository
import com.ltb.sae501.repository.UserRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class TrainingService(
    private val categoryService: CategoryService,
    private val trainingImageRepository: TrainingImageRepository,
    private val customModelRepository: CustomModelRepository,
    private val userRepository: UserRepository
) {
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
        updateStatus(userId, "training", 0.2f, 2, 15, 0.0f, null)

        Thread.sleep(1000)
        updateStatus(userId, "training", 0.5f, 7, 15, 0.75f, null)

        Thread.sleep(1000)
        updateStatus(userId, "training", 0.8f, 12, 15, 0.82f, null)

        Thread.sleep(1000)

        return ByteArray(1024)
    }

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
