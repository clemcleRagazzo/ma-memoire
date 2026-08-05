package com.moulis.mamemoire.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.moulis.mamemoire.ui.theme.MaMemoireTheme
import com.moulis.mamemoire.StatItem
import com.moulis.mamemoire.models.GameEntry
import com.moulis.mamemoire.repositories.GameRepository

private data class HistoryStats(
    val weightedScore: Double,
    val weightedTotal: Double,
    val weightedWins: Double,
    val weightedCount: Double,
    val maxStreak: Int,
    val bestStreakMap: Map<GameEntry, Int>
)

private val historyDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

private fun computeHistoryStats(answered: List<GameEntry>): HistoryStats {
    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val fullLimit = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -29) }.time
    val halfLimit = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }.time

    var weightedScore = 0.0
    var weightedTotal = 0.0
    var weightedWins  = 0.0
    var weightedCount = 0.0

    answered.forEach { entry ->
        try {
            val d = historyDateFormat.parse(entry.date)
            if (d != null) {
                val weight = when {
                    !d.before(fullLimit) -> 1.0
                    !d.before(halfLimit) -> 0.5
                    else -> 0.0
                }
                if (weight > 0.0) {
                    weightedScore += entry.score * weight
                    weightedTotal += entry.total * weight
                    weightedCount += weight
                    if (entry.score == entry.total.toDouble()) {
                        weightedWins += weight
                    }
                }
            }
        } catch (_: Exception) { }
    }

    var maxStreak = 0
    var currentStreak = 0
    var bestStreakEndIndex = -1

    val answeredOldestFirst = answered.reversed()
    answeredOldestFirst.forEachIndexed { index, entry ->
        if (entry.score == entry.total.toDouble()) {
            currentStreak++
            if (currentStreak >= maxStreak) {
                maxStreak = currentStreak
                bestStreakEndIndex = index
            }
        } else {
            currentStreak = 0
        }
    }

    val bestStreakMap = mutableMapOf<GameEntry, Int>()
    if (maxStreak > 0 && bestStreakEndIndex != -1) {
        for (i in 0 until maxStreak) {
            val entry = answeredOldestFirst[bestStreakEndIndex - i]
            bestStreakMap[entry] = maxStreak - i
        }
    }

    return HistoryStats(weightedScore, weightedTotal, weightedWins, weightedCount, maxStreak, bestStreakMap)
}

@Composable
fun HistoryScreen(modifier: Modifier, history: List<GameEntry>) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(modifier = Modifier.height(46.dp))

        Text(
            text       = "Historique",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune partie enregistrée.", color = Color.Gray)
            }
        } else {
            // Statistiques des 30.5 derniers jours
            val answered = remember(history) { history.filter { it.playerAnswer != null } }
            val stats = remember(answered) { computeHistoryStats(answered) }

                val monthlyRatio = if (stats.weightedTotal > 0) ((stats.weightedScore * 100) / stats.weightedTotal).toInt() else 0
                val displayCount = if (stats.weightedCount % 1.0 == 0.0) stats.weightedCount.toInt().toString() else "%.1f".format(stats.weightedCount)
                val displayWins  = if (stats.weightedWins % 1.0 == 0.0) stats.weightedWins.toInt().toString() else "%.1f".format(stats.weightedWins)
//                Text(
//                    text = "sur les 30 derniers jours",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Défis 🎯", value = displayCount)
                    StatItem(label = "Succès 🏆", value = "$displayWins")
                    StatItem(label = "Ratio 📊", value = "$monthlyRatio%")
                    StatItem(label = "Série 🔥", value = "${stats.maxStreak}")
                }
            }

            // Liste
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entry ->
                HistoryCard(entry, streakNumber = stats.bestStreakMap[entry])
            }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun HistoryCard(entry: GameEntry, streakNumber: Int? = null) {
    val ratio = if (entry.playerAnswer != null) {
        GameRepository.calculateRatio(entry.score, entry.total)
    } else 0

    val correctCount = if (entry.playerAnswer != null) {
        GameRepository.countCorrectDigits(entry.morningNumber, entry.playerAnswer, entry.total)
    } else 0

    val bgColor = when {
        entry.playerAnswer == null -> Color(0xFF9E9E9E).copy(alpha = 0.08f)
        ratio == 100  -> Color(0xFF4CAF50).copy(alpha = 0.10f)
        ratio >= 50   -> Color(0xFFFF9800).copy(alpha = 0.10f)
        else          -> Color(0xFFF44336).copy(alpha = 0.10f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(text = entry.date, fontSize = 12.sp, color = Color.Gray)
                Text(
                    text       = "Nombre : ${entry.morningNumber.toString().padStart(entry.total, '0')}",
                    fontWeight = FontWeight.Medium
                )
                if (entry.playerAnswer != null) {
                    Text(
                        text  = "Réponse : ${entry.playerAnswer.toString().padStart(entry.total, '0')}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(text = "Sans réponse", fontSize = 13.sp, color = Color.Gray)
                }
            }

            if (entry.playerAnswer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (streakNumber != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = streakNumber.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722)
                            )
                            Text(text = "🔥", fontSize = 20.sp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "$correctCount/${entry.total}",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                        Text(
                            text     = "$ratio%",
                            fontSize = 13.sp,
                            color    = when {
                                ratio == 100 -> Color(0xFF4CAF50)
                                ratio >= 50  -> Color(0xFFFF9800)
                                else         -> Color(0xFFF44336)
                            }
                        )
                    }
                }
            } else {
                Text(text = "—", color = Color.Gray, fontSize = 20.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    val sampleHistory = listOf(
        GameEntry("02/07/2026", 9012, 5877, 0.0),
        GameEntry("01/07/2026", 9012, 9012, 4.0),
        GameEntry("31/06/2026", 123482, 123000, 3.0),
        GameEntry("30/06/2026", 12345678, 12345678, 8.0),
        GameEntry("29/06/2026", 12345678, 12345678, 8.0),
        GameEntry("28/06/2026", 12345678, 12345678, 8.0),
    )
    MaMemoireTheme {
        HistoryScreen(modifier = Modifier, history = sampleHistory)
    }
}
