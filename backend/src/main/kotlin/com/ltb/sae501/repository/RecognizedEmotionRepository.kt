package com.ltb.sae501.repository

import com.ltb.sae501.entity.RecognizedEmotion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RecognizedEmotionRepository : JpaRepository<RecognizedEmotion, Long> {
    fun findByRecognitionResultId(recognitionId: String): List<RecognizedEmotion>
}
