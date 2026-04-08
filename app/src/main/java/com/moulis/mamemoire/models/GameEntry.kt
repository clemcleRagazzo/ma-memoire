package com.moulis.mamemoire.models

data class GameEntry(
    val date: String,           // "2025-01-15"
    val morningNumber: Int,     // 4837
    val playerAnswer: Int?,     // null si pas encore répondu
    val score: Int,             // chiffres corrects en position (0-4)
    val total: Int = 4,         // toujours 4 chiffres
    val isWon: Boolean          // true si score == total
) {
    val ratioPercent: Int get() = if (total > 0) (score * 100) / total else 0
}
