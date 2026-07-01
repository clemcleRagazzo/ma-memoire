package com.moulis.mamemoire.models

data class GameEntry(
    val date: String,
    val morningNumber: Int,
    val playerAnswer: Int?,
    val score: Double,
) {
    val total: Int
        get() = morningNumber.toString().length
}