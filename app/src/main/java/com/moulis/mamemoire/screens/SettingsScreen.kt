package com.moulis.mamemoire.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.moulis.mamemoire.ui.theme.MaMemoireTheme
import com.moulis.mamemoire.repositories.GameRepository
import com.moulis.mamemoire.models.GameEntry
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import com.moulis.mamemoire.MemoryApp


@Composable
fun SettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    var digitsCount by remember { mutableFloatStateOf(GameRepository.getDigitsCount(context).toFloat()) }
    var morningHour by remember { mutableIntStateOf(GameRepository.getMorningHour(context)) }
    var eveningHour by remember { mutableIntStateOf(GameRepository.getEveningHour(context)) }

    val scrollState = rememberScrollState()

    val duration = eveningHour - morningHour
    val difficultyColor = when {
        duration > 12 -> Color(0xFF5D0000) // très difficile
        duration > 10 -> Color(0xFFD32F2F) // difficile
        duration > 8  -> Color(0xFFFF9800) // moyen+
        duration > 6  -> Color(0xFFFFC107) // moyen
        duration > 4  -> Color(0xFF8BC34A) // facile
        duration > 2  -> Color(0xFF4CAF50) // très facile
        else          -> Color(0xFF2196F3) // trivial
    }
    val difficultyText = when {
        duration > 12 -> "Difficulté : Extrême"
        duration > 10 -> "Difficulté : Difficile"
        duration > 8 -> "Difficulté : Attendu"
        duration > 6 -> "Difficulté : Moyen"
        duration > 4 -> "Difficulté : Facile"
        duration > 2 -> "Difficulté : Très facile"
        else -> "Sérieusement ?"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = "Paramètres",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Nombre de chiffres : ${digitsCount.toInt()}",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = digitsCount,
                    onValueChange = {
                        digitsCount = it
                        GameRepository.saveDigitsCount(context, it.toInt())
                    },
                    valueRange = 2f..8f,
                    steps = 5
                )
                Text(
                    text = "Ce paramètre s'appliquera au prochain défi généré.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // --- Notification Time Pickers ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Horaires des notifications",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Matin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(context, { _, hour, _ ->
                                if (hour < eveningHour) {
                                    morningHour = hour
                                    GameRepository.saveMorningHour(context, hour)
                                    (context.applicationContext as? MemoryApp)?.scheduleNotifications()
                                }
                            }, morningHour, 0, true).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Matin (Nombre envoyé)")
                    Text(
                        text = "${morningHour}h00",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Soir
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(context, { _, hour, _ ->
                                if (hour > morningHour) {
                                    eveningHour = hour
                                    GameRepository.saveEveningHour(context, hour)
                                    (context.applicationContext as? MemoryApp)?.scheduleNotifications()
                                }
                            }, eveningHour, 0, true).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Soir (Réponse attendue)")
                    Text(
                        text = "${eveningHour}h00",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Intervalle : ${duration}h",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = difficultyText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = difficultyColor
                )

                Text(
                    text = "L'heure du matin doit être antérieure à celle du soir.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Text(
            text = "Version 1.1.0",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaMemoireTheme {
        SettingsScreen(modifier = Modifier)
    }
}
