package com.ltb.sae501.service

import com.ltb.sae501.entity.RecognitionResult
import com.ltb.sae501.entity.RecognizedEmotion
import com.ltb.sae501.repository.RecognitionResultRepository
import com.ltb.sae501.repository.RecognizedEmotionRepository
import com.ltb.sae501.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class RecognitionService(
    private val recognitionResultRepository: RecognitionResultRepository,
    private val recognizedEmotionRepository: RecognizedEmotionRepository,
    private val userRepository: UserRepository,
    private val emotionCategoryRepository: com.ltb.sae501.repository.EmotionCategoryRepository
) {
    @Transactional
    fun saveRecognition(
        imageData: ByteArray,
        emotions: List<Pair<String, Float>>,
        userId: String
    ): RecognitionResult {
        val user = userRepository.findById(userId).orElse(null)
            ?: throw IllegalArgumentException("User not found: $userId")

        val recognition = RecognitionResult(
            id = UUID.randomUUID().toString(),
            detectedAt = System.currentTimeMillis(),
            imageData = imageData,
            user = user
        )

        val savedRecognition = recognitionResultRepository.save(recognition)

        emotions.forEach { (emotionId, confidence) ->
            val emotionCategory = emotionCategoryRepository.findById(emotionId).orElse(null)
                ?: throw IllegalArgumentException("Emotion category not found: $emotionId")
            
            val recognizedEmotion = RecognizedEmotion(
                id = UUID.randomUUID().toString(),
                recognitionResult = savedRecognition,
                emotionCategory = emotionCategory,
                confidence = confidence,
                detectedAt = savedRecognition.detectedAt
            )
            savedRecognition.recognizedEmotions.add(recognizedEmotion)
        }

        return savedRecognition
    }

    fun getAllRecognitions(): List<RecognitionResult> {
        return recognitionResultRepository.findAllByOrderByDetectedAtDesc()
    }

    fun getRecognitionById(id: String): RecognitionResult? {
        return recognitionResultRepository.findById(id).orElse(null)
    }

    fun getRecognitionsByUser(userId: String): List<RecognitionResult> {
        return recognitionResultRepository.findByUserIdOrderByDetectedAtDesc(userId)
    }

    @Transactional
    fun deleteRecognition(id: String): Boolean {
        return if (recognitionResultRepository.existsById(id)) {
            recognitionResultRepository.deleteById(id)
            true
        } else {
            false
        }
    }
}
