package com.ltb.sae501.service

import com.ltb.sae501.entity.EmotionCategory
import com.ltb.sae501.entity.TrainingImage
import com.ltb.sae501.repository.EmotionCategoryRepository
import com.ltb.sae501.repository.TrainingImageRepository
import com.ltb.sae501.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CategoryService(
    private val categoryRepository: EmotionCategoryRepository,
    private val trainingImageRepository: TrainingImageRepository,
    private val userRepository: UserRepository
) {
    fun getAllCategories(): List<EmotionCategory> {
        return categoryRepository.findAllByOrderByNameAsc()
    }

    fun getAllCategoriesForUser(userId: String): List<EmotionCategory> {
        val categories = categoryRepository.findAllByOrderByNameAsc()
        return categories.map { category ->
            val userImages = trainingImageRepository.findByCategoryIdAndUserId(category.id, userId)
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

        val trainingImage = TrainingImage(
            id = UUID.randomUUID().toString(),
            category = category,
            user = user,
            imageData = imageData,
            fileName = fileName,
            uploadedAt = System.currentTimeMillis()
        )

        val savedImage = trainingImageRepository.save(trainingImage)

        category.imageCount = trainingImageRepository.countByCategoryIdAndUserId(categoryId, userId)
        category.lastUpdated = System.currentTimeMillis()
        categoryRepository.save(category)

        return savedImage
    }

    @Transactional
    fun deleteTrainingImage(categoryId: String, imageId: String, userId: String): Boolean {
        val image = trainingImageRepository.findById(imageId).orElse(null) ?: return false

        if (image.category?.id != categoryId || image.user?.id != userId) {
            return false
        }

        trainingImageRepository.delete(image)

        val category = categoryRepository.findById(categoryId).orElse(null)
        category?.let {
            it.imageCount = trainingImageRepository.countByCategoryIdAndUserId(categoryId, userId)
            it.lastUpdated = System.currentTimeMillis()
            categoryRepository.save(it)
        }

        return true
    }

    fun getTrainingImagesByCategory(categoryId: String, userId: String): List<TrainingImage> {
        return trainingImageRepository.findByCategoryIdAndUserId(categoryId, userId)
    }

    fun getCategoryByIdForUser(categoryId: String, userId: String): EmotionCategory? {
        val category = categoryRepository.findById(categoryId).orElse(null) ?: return null
        val userImages = trainingImageRepository.findByCategoryIdAndUserId(categoryId, userId)
        category.trainingImages = userImages.toMutableList()
        category.imageCount = userImages.size
        return category
    }

    @Transactional
    fun initializeDefaultCategories(): Boolean {
        if (categoryRepository.count() > 0) {
            return false
        }

        val defaultCategories = listOf(
            EmotionCategory("colere", "Colère", "Angry", "😠", "#FF5252", "Émotion de colère"),
            EmotionCategory("degout", "Dégoût", "Disgust", "🤢", "#9C27B0", "Émotion de dégoût"),
            EmotionCategory("peur", "Peur", "Fear", "😨", "#673AB7", "Émotion de peur"),
            EmotionCategory("joie", "Joie", "Happy", "😄", "#FFC107", "Émotion de joie"),
            EmotionCategory("tristesse", "Tristesse", "Sad", "😢", "#2196F3", "Émotion de tristesse"),
            EmotionCategory("surprise", "Surprise", "Surprise", "😲", "#FF9800", "Émotion de surprise"),
            EmotionCategory("neutre", "Neutre", "Neutral", "😐", "#9E9E9E", "Émotion neutre")
        )

        categoryRepository.saveAll(defaultCategories)
        return true
    }
}
