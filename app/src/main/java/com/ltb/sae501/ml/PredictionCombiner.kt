package com.ltb.sae501.ml

object PredictionCombiner {

    /**
     * Combine les prédictions du modèle de base et du modèle personnalisé
     * en utilisant une stratégie adaptive basée sur la confiance
     */
    fun combine(
        baseProbabilities: FloatArray,
        customProbabilities: FloatArray?,
        labels: List<String>
    ): CombinedEmotionResult {

        // Cas 1: Aucun modèle personnalisé - utiliser uniquement le modèle de base
        if (customProbabilities == null) {
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

        // Cas 2: Haute confiance du modèle de base (>0.8) - faire confiance au modèle de base
        if (baseMaxProb > 0.8f) {
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

        // Cas 3 & 4: Combiner les modèles avec pondération adaptive
        val weights = if (baseMaxProb < 0.5f) {
            // Basse confiance du modèle de base - privilégier le modèle personnalisé
            Pair(0.3f, 0.7f) // 30% base, 70% custom
        } else {
            // Confiance moyenne du modèle de base
            Pair(0.6f, 0.4f) // 60% base, 40% custom
        }

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

    /**
     * Variante permettant de spécifier une stratégie de combinaison explicite
     */
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
    ADAPTIVE,           // Stratégie adaptive basée sur la confiance (recommandée)
    WEIGHTED_AVERAGE,   // Moyenne pondérée fixe (60% base, 40% custom)
    CUSTOM_ONLY,        // Utiliser uniquement le modèle personnalisé
    BASE_ONLY           // Utiliser uniquement le modèle de base
}
