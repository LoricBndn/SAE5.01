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
    private val userRepository: UserRepository
) {
    @Transactional
    fun saveRecognition(
        imageData: ByteArray,
        emotions: List<Pair<String, Float>>,
        userId: String?
    ): RecognitionResult {
        val recognition = RecognitionResult(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            imageData = imageData,
            user = userId?.let { userRepository.findById(it).orElse(null) }
        )

        val savedRecognition = recognitionResultRepository.save(recognition)

        emotions.forEach { (emotion, confidence) ->
            val recognizedEmotion = RecognizedEmotion(
                recognitionResult = savedRecognition,
                emotion = emotion,
                confidence = confidence
            )
            savedRecognition.recognizedEmotions.add(recognizedEmotion)
        }

        recognizedEmotionRepository.saveAll(savedRecognition.recognizedEmotions)

        return savedRecognition
    }

    fun getAllRecognitions(): List<RecognitionResult> {
        return recognitionResultRepository.findAllByOrderByTimestampDesc()
    }

    fun getRecognitionById(id: String): RecognitionResult? {
        return recognitionResultRepository.findById(id).orElse(null)
    }

    fun getRecognitionsByUser(userId: String?): List<RecognitionResult> {
        return recognitionResultRepository.findByUserIdOrderByTimestampDesc(userId)
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
