package com.ltb.sae501.controller

import com.ltb.sae501.dto.TrainingImagesResponse
import com.ltb.sae501.dto.TrainingStatusResponse
import com.ltb.sae501.service.TrainingService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/training")
@CrossOrigin(origins = ["*"])
class TrainingController(
    private val trainingService: TrainingService
) {

    @GetMapping("/images/download")
    fun downloadAllTrainingImages(authentication: Authentication): ResponseEntity<TrainingImagesResponse> {
        val userId = authentication.principal as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val response = trainingService.getAllTrainingImagesForUser(userId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/start")
    fun startTraining(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val userId = authentication.principal as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val jobId = trainingService.startTraining(userId)
        return ResponseEntity.ok(mapOf("jobId" to jobId, "userId" to userId))
    }

    @GetMapping("/status")
    fun getTrainingStatus(authentication: Authentication): ResponseEntity<TrainingStatusResponse> {
        val userId = authentication.principal as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val status = trainingService.getTrainingStatus(userId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(status)
    }

    @GetMapping("/model/download", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadCustomModel(authentication: Authentication): ResponseEntity<ByteArray> {
        val userId = authentication.principal as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val modelData = trainingService.downloadCustomModel(userId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=custom_emotion_model.tflite")
            .body(modelData)
    }

    @PostMapping("/model/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadCustomModel(
        @RequestParam("model") modelFile: MultipartFile,
        @RequestParam("metadata") metadataJson: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userId = authentication.principal as? String
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            val modelData = modelFile.bytes
            trainingService.uploadCustomModel(userId, modelData, metadataJson)
            return ResponseEntity.status(HttpStatus.CREATED).build()
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
