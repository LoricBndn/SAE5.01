package com.ltb.sae501.controller

import com.ltb.sae501.dto.CategoryResponse
import com.ltb.sae501.dto.TrainingImageResponse
import com.ltb.sae501.service.CategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = ["*"])
class CategoryController(
    private val categoryService: CategoryService
) {
    @GetMapping
    fun getAllCategories(): ResponseEntity<List<CategoryResponse>> {
        val categories = categoryService.getAllCategories()

        val responses = categories.map { category ->
            CategoryResponse(
                id = category.id,
                name = category.name,
                nameEn = category.nameEn,
                emoji = category.emoji,
                color = category.color,
                description = category.description,
                imageCount = category.imageCount,
                trainingImages = category.trainingImages.map {
                    "/api/files/training/${it.id}"
                },
                createdAt = category.createdAt,
                lastUpdated = category.lastUpdated
            )
        }

        return ResponseEntity.ok(responses)
    }

    @GetMapping("/{id}")
    fun getCategoryById(
        @PathVariable id: String,
        authentication: Authentication?
    ): ResponseEntity<CategoryResponse> {
        val userId = authentication?.principal as? String

        val category = if (userId != null) {
            categoryService.getCategoryByIdForUser(id, userId)
        } else {
            categoryService.getCategoryById(id)
        }

        if (category == null) {
            return ResponseEntity.notFound().build()
        }

        val response = CategoryResponse(
            id = category.id,
            name = category.name,
            nameEn = category.nameEn,
            emoji = category.emoji,
            color = category.color,
            description = category.description,
            imageCount = category.imageCount,
            trainingImages = category.trainingImages.map {
                "/api/files/training/${it.id}"
            },
            createdAt = category.createdAt,
            lastUpdated = category.lastUpdated
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/initialize")
    fun initializeDefaultCategories(): ResponseEntity<Map<String, Any>> {
        val initialized = categoryService.initializeDefaultCategories()

        return if (initialized) {
            ResponseEntity.ok(mapOf("message" to "Categories initialized successfully"))
        } else {
            ResponseEntity.ok(mapOf("message" to "Categories already exist"))
        }
    }

    @PostMapping("/{categoryId}/training-images", consumes = ["multipart/form-data"])
    fun uploadTrainingImage(
        @PathVariable categoryId: String,
        @RequestParam("image") imageFile: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<TrainingImageResponse> {
        try {
            val userId = authentication.principal as? String
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

            val imageData = imageFile.bytes
            val fileName = imageFile.originalFilename ?: "image.jpg"

            val trainingImage = categoryService.uploadTrainingImage(categoryId, userId, imageData, fileName)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

            val response = TrainingImageResponse(
                id = trainingImage.id,
                categoryId = categoryId,
                imageUrl = "/api/files/training/${trainingImage.id}",
                fileName = trainingImage.fileName,
                uploadedAt = trainingImage.uploadedAt
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/{categoryId}/training-images/{imageId}")
    fun deleteTrainingImage(
        @PathVariable categoryId: String,
        @PathVariable imageId: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userId = authentication.principal as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return if (categoryService.deleteTrainingImage(categoryId, imageId, userId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
