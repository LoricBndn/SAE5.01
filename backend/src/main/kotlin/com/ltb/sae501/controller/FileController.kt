package com.ltb.sae501.controller

import com.ltb.sae501.service.CategoryService
import com.ltb.sae501.service.RecognitionService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = ["*"])
class FileController(
    private val recognitionService: RecognitionService,
    private val categoryService: CategoryService
) {
    @GetMapping("/recognition/{recognitionId}")
    fun getRecognitionImage(
        @PathVariable recognitionId: String,
        authentication: Authentication?
    ): ResponseEntity<ByteArray> {
        val recognition = recognitionService.getRecognitionById(recognitionId)
            ?: return ResponseEntity.notFound().build()

        if (authentication != null) {
            val userId = authentication.principal as String
            if (recognition.user?.id != userId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.IMAGE_JPEG

        return ResponseEntity.ok()
            .headers(headers)
            .body(recognition.imageData)
    }

    @GetMapping("/training/{imageId}")
    fun getTrainingImage(@PathVariable imageId: String): ResponseEntity<ByteArray> {
        val categories = categoryService.getAllCategories()

        for (category in categories) {
            val trainingImage = category.trainingImages.find { it.id == imageId }
            if (trainingImage != null) {
                val headers = HttpHeaders()
                headers.contentType = MediaType.IMAGE_JPEG

                return ResponseEntity.ok()
                    .headers(headers)
                    .body(trainingImage.imageData)
            }
        }

        return ResponseEntity.notFound().build()
    }
}
