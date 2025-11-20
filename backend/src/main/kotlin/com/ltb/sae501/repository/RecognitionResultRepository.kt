package com.ltb.sae501.repository

import com.ltb.sae501.entity.RecognitionResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RecognitionResultRepository : JpaRepository<RecognitionResult, String> {
    fun findAllByOrderByTimestampDesc(): List<RecognitionResult>
    fun findByUserIdOrderByTimestampDesc(userId: String?): List<RecognitionResult>
}
