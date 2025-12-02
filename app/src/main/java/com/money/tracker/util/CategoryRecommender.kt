package com.money.tracker.util

import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import java.util.Calendar

/**
 * Recommends categories for new transactions based on:
 * - Merchant name matching (highest priority)
 * - Description keyword matching
 * - Transaction source patterns
 * - Time of day patterns
 * - Amount range patterns
 * - Most frequently used categories
 */
class CategoryRecommender {

    data class ScoredCategory(
        val category: Category,
        val score: Double,
        val reason: String
    )

    /**
     * Get recommended categories sorted by relevance score.
     * Returns top 5 recommendations followed by remaining categories alphabetically.
     */
    fun getRecommendedCategories(
        allCategories: List<Category>,
        historicalTransactions: List<Transaction>,
        merchant: String?,
        description: String,
        amount: Double,
        source: TransactionSource,
        type: TransactionType,
        hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): List<Category> {
        if (allCategories.isEmpty()) return emptyList()

        // Score all categories (parents and children)
        val scoredCategories = allCategories.map { category ->
            val score = calculateScore(
                category = category,
                allCategories = allCategories,
                transactions = historicalTransactions,
                merchant = merchant,
                description = description,
                amount = amount,
                source = source,
                type = type,
                hour = hour
            )
            ScoredCategory(category, score.first, score.second)
        }

        // Get top 5 recommendations (score > 0)
        val recommendations = scoredCategories
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(5)
            .map { it.category }

        // Get remaining parent categories sorted alphabetically (children shown via hierarchy in picker)
        val recommendedIds = recommendations.map { it.id }.toSet()
        val parentCategories = allCategories.filter { it.parentId == null }
        val remaining = parentCategories
            .filter { it.id !in recommendedIds }
            .sortedBy { it.name.lowercase() }

        return recommendations + remaining
    }

    private fun calculateScore(
        category: Category,
        allCategories: List<Category>,
        transactions: List<Transaction>,
        merchant: String?,
        description: String,
        amount: Double,
        source: TransactionSource,
        type: TransactionType,
        hour: Int
    ): Pair<Double, String> {
        var totalScore = 0.0
        var reason = ""

        // For parent categories, include children in scoring
        // For child categories, only score that specific category
        val categoryIds = if (category.parentId == null) {
            val childIds = allCategories
                .filter { it.parentId == category.id }
                .map { it.id }
            setOf(category.id) + childIds
        } else {
            setOf(category.id)
        }

        // Filter relevant transactions (same type, has category)
        val relevantTransactions = transactions.filter {
            it.type == type && it.categoryId != null && !it.isPending
        }

        // 1. Merchant matching (highest priority - 100 points)
        if (!merchant.isNullOrBlank()) {
            val merchantLower = merchant.lowercase().trim()
            val merchantMatches = relevantTransactions.filter { txn ->
                txn.categoryId in categoryIds &&
                txn.merchant?.lowercase()?.trim() == merchantLower
            }
            if (merchantMatches.isNotEmpty()) {
                totalScore += 100.0
                reason = "Same merchant"
            } else {
                // Partial merchant match (50 points)
                val partialMatches = relevantTransactions.filter { txn ->
                    txn.categoryId in categoryIds &&
                    (txn.merchant?.lowercase()?.contains(merchantLower) == true ||
                     merchantLower.contains(txn.merchant?.lowercase() ?: "---"))
                }
                if (partialMatches.isNotEmpty()) {
                    totalScore += 50.0
                    reason = "Similar merchant"
                }
            }
        }

        // 2. Description keyword matching (40 points)
        if (description.isNotBlank()) {
            val descWords = description.lowercase().split(" ", ",", "-", "/")
                .filter { it.length > 2 }
                .toSet()

            val descriptionMatches = relevantTransactions.filter { txn ->
                txn.categoryId in categoryIds &&
                descWords.any { word ->
                    txn.description.lowercase().contains(word) ||
                    txn.merchant?.lowercase()?.contains(word) == true
                }
            }
            if (descriptionMatches.isNotEmpty()) {
                val matchScore = minOf(40.0, descriptionMatches.size * 10.0)
                totalScore += matchScore
                if (reason.isEmpty()) reason = "Similar description"
            }
        }

        // 3. Category name/emoji matching with description (30 points)
        val categoryNameLower = category.name.lowercase()
        val descLower = description.lowercase()
        if (descLower.contains(categoryNameLower) || categoryNameLower.contains(descLower.take(5))) {
            totalScore += 30.0
            if (reason.isEmpty()) reason = "Name match"
        }

        // 4. Frequency score - most used categories (25 points max)
        val categoryUsageCount = relevantTransactions.count { it.categoryId in categoryIds }
        val totalTransactions = relevantTransactions.size.coerceAtLeast(1)
        val frequencyRatio = categoryUsageCount.toDouble() / totalTransactions
        val frequencyScore = frequencyRatio * 25.0
        totalScore += frequencyScore
        if (frequencyScore > 15 && reason.isEmpty()) reason = "Frequently used"

        // 5. Source pattern matching (20 points)
        val sourceMatches = relevantTransactions.filter { txn ->
            txn.categoryId in categoryIds && txn.source == source
        }
        if (sourceMatches.size >= 3) {
            val sourceRatio = sourceMatches.size.toDouble() /
                relevantTransactions.count { it.source == source }.coerceAtLeast(1)
            totalScore += sourceRatio * 20.0
            if (reason.isEmpty()) reason = "Common for ${source.name}"
        }

        // 6. Time of day pattern (15 points)
        val timeCategory = when (hour) {
            in 6..10 -> "morning"
            in 11..14 -> "lunch"
            in 15..18 -> "afternoon"
            in 19..22 -> "evening"
            else -> "night"
        }
        val timeMatches = relevantTransactions.filter { txn ->
            txn.categoryId in categoryIds &&
            getTimeCategory(txn.date) == timeCategory
        }
        if (timeMatches.size >= 3) {
            totalScore += 15.0
            if (reason.isEmpty()) reason = "Common at this time"
        }

        // 7. Amount range pattern (15 points)
        val amountMatches = relevantTransactions.filter { txn ->
            txn.categoryId in categoryIds &&
            isInSameAmountRange(txn.amount, amount)
        }
        if (amountMatches.size >= 2) {
            totalScore += 15.0
            if (reason.isEmpty()) reason = "Similar amount range"
        }

        // 8. Recency bonus - recent transactions get slight boost (10 points)
        val recentCutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 days
        val recentMatches = relevantTransactions.filter { txn ->
            txn.categoryId in categoryIds && txn.date > recentCutoff
        }
        if (recentMatches.isNotEmpty()) {
            totalScore += minOf(10.0, recentMatches.size * 2.0)
            if (reason.isEmpty()) reason = "Recently used"
        }

        return Pair(totalScore, reason)
    }

    private fun getTimeCategory(epochMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..10 -> "morning"
            in 11..14 -> "lunch"
            in 15..18 -> "afternoon"
            in 19..22 -> "evening"
            else -> "night"
        }
    }

    private fun isInSameAmountRange(amount1: Double, amount2: Double): Boolean {
        // Define amount ranges
        val range1 = getAmountRange(amount1)
        val range2 = getAmountRange(amount2)
        return range1 == range2
    }

    private fun getAmountRange(amount: Double): Int {
        return when {
            amount < 50 -> 0        // Micro
            amount < 200 -> 1       // Small
            amount < 500 -> 2       // Medium-small
            amount < 1000 -> 3      // Medium
            amount < 2500 -> 4      // Medium-large
            amount < 5000 -> 5      // Large
            amount < 10000 -> 6     // Very large
            else -> 7               // Huge
        }
    }
}
