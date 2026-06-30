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
import com.moulis.mamemoire.HistoryCard
import com.moulis.mamemoire.StatItem
import com.moulis.mamemoire.models.GameEntry

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
            val answered = history.filter { it.playerAnswer != null }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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
                    val d = dateFormat.parse(entry.date)
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
                            if (entry.score == entry.total) {
                                weightedWins += weight
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            if (weightedCount > 0) {
                val monthlyRatio = if (weightedTotal > 0) ((weightedScore * 100) / weightedTotal).toInt() else 0
                val displayCount = if (weightedCount % 1.0 == 0.0) weightedCount.toInt().toString() else "%.1f".format(weightedCount)
                val displayWins  = if (weightedWins % 1.0 == 0.0) weightedWins.toInt().toString() else "%.1f".format(weightedWins)

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
                        StatItem(label = "Défis", value = displayCount)
                        StatItem(label = "Succès", value = "$displayWins 🏆")
                        StatItem(label = "Ratio", value = "$monthlyRatio%")
                    }
                }
            }

            // Liste
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entry ->
                    HistoryCard(entry)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    val sampleHistory = listOf(
        GameEntry("30/06/2026", 12345678, 12345678, 8, 8),
        GameEntry("31/06/2026", 123482, 123000, 3, 6),
        GameEntry("01/07/2026", 9012, 1111, 0, 4),
    )
    MaMemoireTheme {
        HistoryScreen(modifier = Modifier, history = sampleHistory)
    }
}
