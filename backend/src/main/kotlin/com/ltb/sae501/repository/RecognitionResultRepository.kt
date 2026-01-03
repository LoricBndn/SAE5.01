package com.ltb.sae501.repository

import com.ltb.sae501.entity.RecognitionResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecognitionResultRepository : JpaRepository<RecognitionResult, String> {
    fun findAllByOrderByDetectedAtDesc(): List<RecognitionResult>

    @Query("SELECT r FROM RecognitionResult r WHERE r.user.id = :userId ORDER BY r.detectedAt DESC")
    fun findByUserIdOrderByDetectedAtDesc(@Param("userId") userId: String): List<RecognitionResult>
}
