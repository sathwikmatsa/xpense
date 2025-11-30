package com.money.tracker.data.entity

enum class SplitShare(val numerator: Int, val denominator: Int, val label: String) {
    HALF(1, 2, "1/2"),
    ONE_THIRD(1, 3, "1/3"),
    ONE_QUARTER(1, 4, "1/4"),
    ONE_FIFTH(1, 5, "1/5"),
    ONE_SIXTH(1, 6, "1/6"),
    CUSTOM(0, 0, "Custom");

    companion object {
        fun fromFraction(numerator: Int, denominator: Int): SplitShare? {
            return entries.find { it.numerator == numerator && it.denominator == denominator }
        }

        fun presets(): List<SplitShare> = entries.filter { it != CUSTOM }
    }
}
