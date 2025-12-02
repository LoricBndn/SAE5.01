package com.ltb.sae501.repository

import com.ltb.sae501.entity.CustomModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomModelRepository : JpaRepository<CustomModel, String> {
    fun findByUserIdOrderByVersionDesc(userId: String): List<CustomModel>
    fun findTopByUserIdOrderByCreatedAtDesc(userId: String): CustomModel?
    fun countByUserId(userId: String): Int
}
