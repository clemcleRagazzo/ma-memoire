package com.moulis.mamemoire.models

data class GameEntry(
    val date: String,
    val morningNumber: Int,
    val playerAnswer: Int?,
    val score: Int,
    val total: Int = 4,
    val isRead: Boolean = false
) {
    val ratioPercent: Int get() = if (total > 0) (score * 100) / total else 0
}
