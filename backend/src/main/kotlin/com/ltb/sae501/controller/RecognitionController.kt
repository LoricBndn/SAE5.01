package com.ltb.sae501.controller

import com.ltb.sae501.dto.FeedItemResponse
import com.ltb.sae501.dto.RecognitionRequest
import com.ltb.sae501.dto.RecognitionResponse
import com.ltb.sae501.dto.RecognizedEmotionDto
import com.ltb.sae501.service.RecognitionService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/recognitions")
@CrossOrigin(origins = ["*"])
class RecognitionController(
    private val recognitionService: RecognitionService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @PostMapping(consumes = ["multipart/form-data"])
    fun saveRecognition(
        @RequestParam("image") imageFile: MultipartFile,
        @RequestParam("emotions") emotionsJson: String,
        @RequestParam("isPublic", required = false, defaultValue = "false") isPublic: Boolean,
        @RequestParam("displayName", required = false) displayName: String?,
        authentication: Authentication
    ): ResponseEntity<RecognitionResponse> {
        try {
            val userId = authentication.principal as String
            logger.info("[SYNC] Receiving recognition from user: $userId")
            logger.info("[SYNC] Image size: ${imageFile.size} bytes, Emotions JSON: $emotionsJson")
            logger.info("[SYNC] isPublic: $isPublic, displayName: $displayName")

            val emotions = parseEmotionsJson(emotionsJson)
            logger.info("[SYNC] Parsed ${emotions.size} emotions: $emotions")

            val imageData = imageFile.bytes
            val recognition = recognitionService.saveRecognition(imageData, emotions, userId, isPublic, displayName)
            logger.info("[SYNC] Successfully saved recognition with ID: ${recognition.id}")

            val emotionDtos = recognition.recognizedEmotions.map {
                RecognizedEmotionDto(
                    id = it.id,
                    recognitionId = recognition.id,
                    emotionId = it.emotionCategory?.id ?: "",
                    confidence = it.confidence,
                    detectedAt = it.detectedAt
                )
            }

            val response = RecognitionResponse(
                id = recognition.id,
                detectedAt = recognition.detectedAt,
                imageUrl = "/api/files/recognition/${recognition.id}",
                recognizedEmotions = emotionDtos,
                userId = recognition.user?.id
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("[SYNC ERROR] Failed to save recognition", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/feed")
    fun getFeed(authentication: Authentication?): ResponseEntity<List<FeedItemResponse>> {
        val currentUserId = authentication?.principal as? String
        val publicRecognitions = recognitionService.getPublicRecognitions()

        val responses = publicRecognitions.map { recognition ->
            FeedItemResponse(
                id = recognition.id,
                detectedAt = recognition.detectedAt,
                imageUrl = "/api/files/recognition/${recognition.id}",
                recognizedEmotions = recognition.recognizedEmotions.map {
                    RecognizedEmotionDto(
                        id = it.id,
                        recognitionId = recognition.id,
                        emotionId = it.emotionCategory?.id ?: "",
                        confidence = it.confidence,
                        detectedAt = it.detectedAt
                    )
                },
                displayName = recognition.displayName,
                isOwnPost = currentUserId != null && recognition.user?.id == currentUserId
            )
        }

        return ResponseEntity.ok(responses)
    }

    @GetMapping
    fun getAllRecognitions(authentication: Authentication): ResponseEntity<List<RecognitionResponse>> {
        val userId = authentication.principal as String
        val recognitions = recognitionService.getRecognitionsByUser(userId)

        val responses = recognitions.map { recognition ->
            RecognitionResponse(
                id = recognition.id,
                detectedAt = recognition.detectedAt,
                imageUrl = "/api/files/recognition/${recognition.id}",
                recognizedEmotions = recognition.recognizedEmotions.map {
                    RecognizedEmotionDto(
                        id = it.id,
                        recognitionId = recognition.id,
                        emotionId = it.emotionCategory?.id ?: "",
                        confidence = it.confidence,
                        detectedAt = it.detectedAt
                    )
                },
                userId = recognition.user?.id
            )
        }

        return ResponseEntity.ok(responses)
    }

    @GetMapping("/{id}")
    fun getRecognitionById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<RecognitionResponse> {
        val userId = authentication.principal as String
        val recognition = recognitionService.getRecognitionById(id)
            ?: return ResponseEntity.notFound().build()

        if (recognition.user?.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val response = RecognitionResponse(
            id = recognition.id,
            detectedAt = recognition.detectedAt,
            imageUrl = "/api/files/recognition/${recognition.id}",
            recognizedEmotions = recognition.recognizedEmotions.map {
                RecognizedEmotionDto(
                    id = it.id,
                    recognitionId = recognition.id,
                    emotionId = it.emotionCategory?.id ?: "",
                    confidence = it.confidence,
                    detectedAt = it.detectedAt
                )
            },
            userId = recognition.user?.id
        )

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun deleteRecognition(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userId = authentication.principal as String
        val recognition = recognitionService.getRecognitionById(id)
            ?: return ResponseEntity.notFound().build()

        if (recognition.user?.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return if (recognitionService.deleteRecognition(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun parseEmotionsJson(json: String): List<Pair<String, Float>> {
        val emotions = mutableListOf<Pair<String, Float>>()
        val regex = """"emotionId"\s*:\s*"([^"]+)"\s*,\s*"confidence"\s*:\s*([\d.]+)""".toRegex()

        regex.findAll(json).forEach { match ->
            val emotionId = match.groupValues[1]
            val confidence = match.groupValues[2].toFloat()
            emotions.add(emotionId to confidence)
        }

        return emotions
    }
}
