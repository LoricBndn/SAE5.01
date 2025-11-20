package com.ltb.sae501.repository

import com.ltb.sae501.entity.TrainingImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrainingImageRepository : JpaRepository<TrainingImage, String> {
    fun findByCategoryId(categoryId: String): List<TrainingImage>
    fun countByCategoryId(categoryId: String): Int
    fun findByCategoryIdAndUserId(categoryId: String, userId: String): List<TrainingImage>
    fun countByCategoryIdAndUserId(categoryId: String, userId: String): Int
}
