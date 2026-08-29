package com.baobei.growthplanet.domain

import com.baobei.growthplanet.data.RecordSummaryRow
import kotlin.math.ceil
import kotlin.math.max

object GrowthCalculator {
    const val THREE_STAR_RATIO = 30.0 / 34.0
    const val TWO_STAR_RATIO = 25.0 / 34.0
    const val ONE_STAR_RATIO = 20.0 / 34.0

    fun finalScore(positive: Int, deduction: Int): Int = max(0, positive - deduction)

    fun starThresholds(maxScore: Int): List<Int> = listOf(
        ceil(maxScore * ONE_STAR_RATIO).toInt(),
        ceil(maxScore * TWO_STAR_RATIO).toInt(),
        ceil(maxScore * THREE_STAR_RATIO).toInt(),
    )

    fun stars(score: Int, maxScore: Int): Int {
        if (maxScore <= 0) return 0
        val (one, two, three) = starThresholds(maxScore)
        return when {
            score >= three -> 3
            score >= two -> 2
            score >= one -> 1
            else -> 0
        }
    }

    fun percent(score: Int, maxScore: Int): Float =
        if (maxScore <= 0) 0f else (score.toFloat() / maxScore).coerceIn(0f, 1f)

    fun totalScore(rows: List<RecordSummaryRow>) = rows.sumOf { finalScore(it.positiveScore, it.deduction) }
    fun totalMax(rows: List<RecordSummaryRow>) = rows.sumOf { it.maxScore }
    fun totalStars(rows: List<RecordSummaryRow>) = rows.sumOf { stars(finalScore(it.positiveScore, it.deduction), it.maxScore) }
}
