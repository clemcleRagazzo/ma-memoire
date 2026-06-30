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
            // Statistiques globales
            val answered = history.filter { it.playerAnswer != null }
            if (answered.isNotEmpty()) {
                val avgRatio = answered.map { it.ratioPercent }.average().toInt()
                val wins     = answered.count { it.ratioPercent == 100 }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                        StatItem(label = "Parties", value = "${answered.size}")
                        StatItem(label = "Victoires", value = "$wins 🏆")
                        StatItem(label = "Ratio moyen", value = "$avgRatio%")
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
        GameEntry("01/07/2026", 9012, 1111, 0, 4)
         )
    MaMemoireTheme {
        HistoryScreen(modifier = Modifier, history = sampleHistory)
    }
}
