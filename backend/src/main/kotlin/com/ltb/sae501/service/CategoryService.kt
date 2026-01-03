package com.ltb.sae501.service

import com.ltb.sae501.entity.EmotionCategory
import com.ltb.sae501.entity.TrainingImage
import com.ltb.sae501.entity.CustomModel
import com.ltb.sae501.repository.EmotionCategoryRepository
import com.ltb.sae501.repository.TrainingImageRepository
import com.ltb.sae501.repository.UserRepository
import com.ltb.sae501.repository.CustomModelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CategoryService(
    private val categoryRepository: EmotionCategoryRepository,
    private val trainingImageRepository: TrainingImageRepository,
    private val userRepository: UserRepository,
    private val customModelRepository: CustomModelRepository
) {
    fun getAllCategories(): List<EmotionCategory> {
        return categoryRepository.findAllByOrderByNameAsc()
    }

    fun getAllCategoriesForUser(userId: String): List<EmotionCategory> {
        val customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
        val categories = categoryRepository.findAllByOrderByNameAsc()
        return categories.map { category ->
            val userImages = if (customModel != null) {
                trainingImageRepository.findByCategoryIdAndCustomModelId(category.id, customModel.id)
            } else {
                emptyList()
            }
            category.trainingImages = userImages.toMutableList()
            category.imageCount = userImages.size
            category
        }
    }

    fun getCategoryById(id: String): EmotionCategory? {
        return categoryRepository.findById(id).orElse(null)
    }

    @Transactional
    fun uploadTrainingImage(categoryId: String, userId: String, imageData: ByteArray, fileName: String): TrainingImage? {
        val category = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val user = userRepository.findById(userId).orElse(null) ?: return null

        var customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
        if (customModel == null) {
            customModel = CustomModel(
                id = UUID.randomUUID().toString(),
                user = user,
                accuracy = 0.0f,
                metadata = "{}",
                modelData = byteArrayOf(),
                version = "0",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            customModel = customModelRepository.save(customModel)
        }

        val trainingImage = TrainingImage(
            id = UUID.randomUUID().toString(),
            category = category,
            customModel = customModel,
            imageData = imageData,
            fileName = fileName,
            uploadedAt = System.currentTimeMillis()
        )

        val savedImage = trainingImageRepository.save(trainingImage)

        category.imageCount = trainingImageRepository.countByCategoryIdAndCustomModelId(categoryId, customModel.id)
        category.updatedAt = System.currentTimeMillis()
        categoryRepository.save(category)

        return savedImage
    }

    @Transactional
    fun deleteTrainingImage(categoryId: String, imageId: String, userId: String): Boolean {
        val image = trainingImageRepository.findById(imageId).orElse(null) ?: return false

        if (image.category?.id != categoryId || image.customModel?.user?.id != userId) {
            return false
        }

        trainingImageRepository.delete(image)

        val category = categoryRepository.findById(categoryId).orElse(null)
        category?.let {
            val customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
            it.imageCount = if (customModel != null) {
                trainingImageRepository.countByCategoryIdAndCustomModelId(categoryId, customModel.id)
            } else {
                0
            }
            it.updatedAt = System.currentTimeMillis()
            categoryRepository.save(it)
        }

        return true
    }

    fun getTrainingImagesByCategory(categoryId: String, userId: String): List<TrainingImage> {
        val customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
        return if (customModel != null) {
            trainingImageRepository.findByCategoryIdAndCustomModelId(categoryId, customModel.id)
        } else {
            emptyList()
        }
    }

    fun getCategoryByIdForUser(categoryId: String, userId: String): EmotionCategory? {
        val category = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
        val userImages = if (customModel != null) {
            trainingImageRepository.findByCategoryIdAndCustomModelId(categoryId, customModel.id)
        } else {
            emptyList()
        }
        category.trainingImages = userImages.toMutableList()
        category.imageCount = userImages.size
        return category
    }

    fun getAllTrainingImagesForUser(userId: String): Map<EmotionCategory, List<TrainingImage>> {
        val customModel = customModelRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
        val categories = categoryRepository.findAllByOrderByNameAsc()
        return if (customModel != null) {
            categories.associateWith { category ->
                trainingImageRepository.findByCategoryIdAndCustomModelId(category.id, customModel.id)
            }.filterValues { it.isNotEmpty() }
        } else {
            emptyMap()
        }
    }

    @Transactional
    fun initializeDefaultCategories(): Boolean {
        if (categoryRepository.count() > 0) {
            return false
        }

        val defaultCategories = listOf(
            EmotionCategory("angry", "Colère", "Angry", "😠", "#FF5252", "Émotion de colère"),
            EmotionCategory("disgust", "Dégoût", "Disgust", "🤢", "#9C27B0", "Émotion de dégoût"),
            EmotionCategory("fear", "Peur", "Fear", "😨", "#673AB7", "Émotion de peur"),
            EmotionCategory("happy", "Joie", "Happy", "😄", "#FFC107", "Émotion de joie"),
            EmotionCategory("sad", "Tristesse", "Sad", "😢", "#2196F3", "Émotion de tristesse"),
            EmotionCategory("surprise", "Surpris", "Surprise", "😲", "#FF9800", "Émotion de surprise"),
            EmotionCategory("neutral", "Neutre", "Neutral", "😐", "#9E9E9E", "Émotion neutre")
        )

        categoryRepository.saveAll(defaultCategories)
        return true
    }
}
