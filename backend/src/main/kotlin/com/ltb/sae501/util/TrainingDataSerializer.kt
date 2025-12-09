package com.ltb.sae501.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ltb.sae501.entity.EmotionCategory
import com.ltb.sae501.entity.TrainingImage
import java.io.File
import java.util.Base64

data class PythonTrainingData(
    val categories: List<PythonCategory>
)

data class PythonCategory(
    val categoryName: String,
    val images: List<PythonImage>
)

data class PythonImage(
    val id: String,
    val data: String
)

object TrainingDataSerializer {
    private val objectMapper = jacksonObjectMapper()

    fun serializeToJsonFile(
        imagesByCategory: Map<EmotionCategory, List<TrainingImage>>
    ): File {
        val data = PythonTrainingData(
            categories = imagesByCategory.map { (category, images) ->
                PythonCategory(
                    categoryName = category.nameEn.lowercase(),
                    images = images.map { img ->
                        PythonImage(
                            id = img.id,
                            data = Base64.getEncoder().encodeToString(img.imageData)
                        )
                    }
                )
            }
        )

        val tempFile = File.createTempFile("training_data_", ".json")
        objectMapper.writeValue(tempFile, data)
        return tempFile
    }
}
