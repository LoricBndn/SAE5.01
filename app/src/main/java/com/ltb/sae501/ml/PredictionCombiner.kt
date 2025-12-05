package com.ltb.sae501.ml

import android.util.Log

object PredictionCombiner {

    private const val TAG = "PredictionCombiner"

    // combine les 2 modèles selon la confiance
    fun combine(
        baseProbabilities: FloatArray,
        customProbabilities: FloatArray?,
        labels: List<String>
    ): CombinedEmotionResult {

        // pas de modèle custom
        if (customProbabilities == null) {
            Log.d(TAG, "Aucun modèle personnalisé disponible - utilisation modèle de base uniquement")
            val maxIndex = baseProbabilities.indices.maxByOrNull { baseProbabilities[it] } ?: 0
            return CombinedEmotionResult(
                emotion = labels[maxIndex],
                confidence = baseProbabilities[maxIndex],
                source = PredictionSource.BASE,
                baseProbabilities = baseProbabilities,
                customProbabilities = null,
                combinedProbabilities = baseProbabilities,
                weights = null
            )
        }

        val baseMaxProb = baseProbabilities.maxOrNull() ?: 0f

        // confiance élevée du base
        if (baseMaxProb > 0.8f) {
            Log.d(TAG, "Haute confiance modèle de base (${baseMaxProb}) - utilisation modèle de base uniquement")
            val maxIndex = baseProbabilities.indices.maxByOrNull { baseProbabilities[it] } ?: 0
            return CombinedEmotionResult(
                emotion = labels[maxIndex],
                confidence = baseMaxProb,
                source = PredictionSource.BASE,
                baseProbabilities = baseProbabilities,
                customProbabilities = customProbabilities,
                combinedProbabilities = baseProbabilities,
                weights = null
            )
        }

        // pondération adaptative
        val weights = if (baseMaxProb < 0.5f) {
            // confiance basse base -> priorité custom
            Log.d(TAG, "Basse confiance modèle de base (${baseMaxProb}) - combinaison hybride avec priorité au modèle personnalisé (30/70)")
            Pair(0.3f, 0.7f) // 30% base, 70% custom
        } else {
            // confiance moyenne
            Log.d(TAG, "Confiance moyenne modèle de base (${baseMaxProb}) - combinaison hybride équilibrée (60/40)")
            Pair(0.6f, 0.4f) // 60% base, 40% custom
        }

        val combined = FloatArray(baseProbabilities.size) { i ->
            baseProbabilities[i] * weights.first + customProbabilities[i] * weights.second
        }

        val maxIndex = combined.indices.maxByOrNull { combined[it] } ?: 0

        Log.d(TAG, "Résultat combinaison hybride: émotion=${labels[maxIndex]}, confiance=${combined[maxIndex]}")
        return CombinedEmotionResult(
            emotion = labels[maxIndex],
            confidence = combined[maxIndex],
            source = PredictionSource.HYBRID,
            baseProbabilities = baseProbabilities,
            customProbabilities = customProbabilities,
            combinedProbabilities = combined,
            weights = weights
        )
    }

    fun combineWithStrategy(
        baseProbabilities: FloatArray,
        customProbabilities: FloatArray?,
        labels: List<String>,
        strategy: CombinationStrategy
    ): CombinedEmotionResult {
        return when (strategy) {
            CombinationStrategy.ADAPTIVE -> combine(baseProbabilities, customProbabilities, labels)
            CombinationStrategy.WEIGHTED_AVERAGE -> combineWeightedAverage(baseProbabilities, customProbabilities, labels)
            CombinationStrategy.CUSTOM_ONLY -> combineCustomOnly(baseProbabilities, customProbabilities, labels)
            CombinationStrategy.BASE_ONLY -> combineBaseOnly(baseProbabilities, customProbabilities, labels)
        }
    }

    private fun combineWeightedAverage(
        baseProbabilities: FloatArray,
        customProbabilities: FloatArray?,
        labels: List<String>
    ): CombinedEmotionResult {
        if (customProbabilities == null) {
            return combine(baseProbabilities, null, labels)
        }

        val weights = Pair(0.6f, 0.4f)
        val combined = FloatArray(baseProbabilities.size) { i ->
            baseProbabilities[i] * weights.first + customProbabilities[i] * weights.second
        }

        val maxIndex = combined.indices.maxByOrNull { combined[it] } ?: 0

        return CombinedEmotionResult(
            emotion = labels[maxIndex],
            confidence = combined[maxIndex],
            source = PredictionSource.HYBRID,
            baseProbabilities = baseProbabilities,
            customProbabilities = customProbabilities,
            combinedProbabilities = combined,
            weights = weights
        )
    }

    private fun combineCustomOnly(
        baseProbabilities: FloatArray,
        customProbabilities: FloatArray?,
        labels: List<String>
    ): CombinedEmotionResult {
        if (customProbabilities == null) {
            return combine(baseProbabilities, null, labels)
        }

        val maxIndex = customProbabilities.indices.maxByOrNull { customProbabilities[it] } ?: 0

        return CombinedEmotionResult(
            emotion = labels[maxIndex],
            confidence = customProbabilities[maxIndex],
            source = PredictionSource.CUSTOM,
            baseProbabilities = baseProbabilities,
            customProbabilities = customProbabilities,
            combinedProbabilities = customProbabilities,
            weights = Pair(0f, 1f)
        )
    }

    private fun combineBaseOnly(
        baseProbabilities: FloatArray,
        customProbabilities: FloatArray?,
        labels: List<String>
    ): CombinedEmotionResult {
        val maxIndex = baseProbabilities.indices.maxByOrNull { baseProbabilities[it] } ?: 0

        return CombinedEmotionResult(
            emotion = labels[maxIndex],
            confidence = baseProbabilities[maxIndex],
            source = PredictionSource.BASE,
            baseProbabilities = baseProbabilities,
            customProbabilities = customProbabilities,
            combinedProbabilities = baseProbabilities,
            weights = Pair(1f, 0f)
        )
    }
}

enum class CombinationStrategy {
    ADAPTIVE,
    WEIGHTED_AVERAGE,
    CUSTOM_ONLY,
    BASE_ONLY
}
