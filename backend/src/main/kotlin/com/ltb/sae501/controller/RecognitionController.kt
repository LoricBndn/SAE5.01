package com.ltb.sae501.controller

import com.ltb.sae501.dto.RecognitionRequest
import com.ltb.sae501.dto.RecognitionResponse
import com.ltb.sae501.dto.RecognizedEmotionDto
import com.ltb.sae501.service.RecognitionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/recognitions")
@CrossOrigin(origins = ["*"])
class RecognitionController(
    private val recognitionService: RecognitionService
) {
    @PostMapping(consumes = ["multipart/form-data"])
    fun saveRecognition(
        @RequestParam("image") imageFile: MultipartFile,
        @RequestParam("emotions") emotionsJson: String,
        @RequestParam("userId", required = false) userId: String?
    ): ResponseEntity<RecognitionResponse> {
        try {
            // Parse emotions JSON (format: [{"emotion":"Joie","confidence":0.95}, ...])
            val emotions = parseEmotionsJson(emotionsJson)

            val imageData = imageFile.bytes
            val recognition = recognitionService.saveRecognition(imageData, emotions, userId)

            val emotionDtos = recognition.recognizedEmotions.map {
                RecognizedEmotionDto(it.emotion, it.confidence)
            }

            val response = RecognitionResponse(
                id = recognition.id,
                timestamp = recognition.timestamp,
                imageUrl = "/api/files/recognition/${recognition.id}",
                recognizedEmotions = emotionDtos,
                userId = recognition.user?.id
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping
    fun getAllRecognitions(): ResponseEntity<List<RecognitionResponse>> {
        val recognitions = recognitionService.getAllRecognitions()

        val responses = recognitions.map { recognition ->
            RecognitionResponse(
                id = recognition.id,
                timestamp = recognition.timestamp,
                imageUrl = "/api/files/recognition/${recognition.id}",
                recognizedEmotions = recognition.recognizedEmotions.map {
                    RecognizedEmotionDto(it.emotion, it.confidence)
                },
                userId = recognition.user?.id
            )
        }

        return ResponseEntity.ok(responses)
    }

    @GetMapping("/{id}")
    fun getRecognitionById(@PathVariable id: String): ResponseEntity<RecognitionResponse> {
        val recognition = recognitionService.getRecognitionById(id)
            ?: return ResponseEntity.notFound().build()

        val response = RecognitionResponse(
            id = recognition.id,
            timestamp = recognition.timestamp,
            imageUrl = "/api/files/recognition/${recognition.id}",
            recognizedEmotions = recognition.recognizedEmotions.map {
                RecognizedEmotionDto(it.emotion, it.confidence)
            },
            userId = recognition.user?.id
        )

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun deleteRecognition(@PathVariable id: String): ResponseEntity<Void> {
        return if (recognitionService.deleteRecognition(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun parseEmotionsJson(json: String): List<Pair<String, Float>> {
        // Simple JSON parsing for format: [{"emotion":"Joie","confidence":0.95}, ...]
        val emotions = mutableListOf<Pair<String, Float>>()
        val regex = """"emotion"\s*:\s*"([^"]+)"\s*,\s*"confidence"\s*:\s*([\d.]+)""".toRegex()

        regex.findAll(json).forEach { match ->
            val emotion = match.groupValues[1]
            val confidence = match.groupValues[2].toFloat()
            emotions.add(emotion to confidence)
        }

        return emotions
    }
}
