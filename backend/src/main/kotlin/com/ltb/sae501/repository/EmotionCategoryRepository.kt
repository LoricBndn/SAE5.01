package com.ltb.sae501.repository

import com.ltb.sae501.entity.EmotionCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmotionCategoryRepository : JpaRepository<EmotionCategory, String> {
    fun findAllByOrderByNameAsc(): List<EmotionCategory>
}
