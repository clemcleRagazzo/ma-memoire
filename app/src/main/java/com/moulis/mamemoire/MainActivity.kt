package com.moulis.mamemoire

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moulis.mamemoire.models.GameEntry
import com.moulis.mamemoire.repositories.GameRepository
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as MemoryApp).scheduleNotifications()
        requestNotificationPermission()

        setContent {
            MaterialTheme {
                MemoryAppUI()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
}

// ─── Utilitaire heure ─────────────────────────────────────────────────────────

/** true si on est entre 18h00 et 23h59 */
private fun isEveningTime(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= 18
}

// ─── UI principale ────────────────────────────────────────────────────────────

@Composable
fun MemoryAppUI() {
    val context = LocalContext.current

    // Onglet actif : 0 = Accueil, 1 = Historique
    var selectedTab by remember { mutableIntStateOf(0) }

    // Recharge l'état à chaque recomposition
    var history by remember { mutableStateOf(GameRepository.getHistory(context)) }
    var answerInput by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val hasMorningNumber = GameRepository.todayHasMorningNumber(context)
    val alreadyAnswered  = GameRepository.todayAlreadyAnswered(context)
    val isEvening        = isEveningTime()

    // Champ dispo uniquement le soir + nombre reçu + pas encore répondu
    val fieldEnabled = isEvening && hasMorningNumber && !alreadyAnswered

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected  = selectedTab == 0,
                    onClick   = { selectedTab = 0 },
                    icon      = { Text("🧠") },
                    label     = { Text("Défi") }
                )
                NavigationBarItem(
                    selected  = selectedTab == 1,
                    onClick   = {
                        history = GameRepository.getHistory(context)
                        selectedTab = 1
                    },
                    icon      = { Text("📋") },
                    label     = { Text("Historique") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier        = Modifier.padding(padding),
                hasMorningNumber = hasMorningNumber,
                alreadyAnswered  = alreadyAnswered,
                isEvening        = isEvening,
                fieldEnabled     = fieldEnabled,
                answerInput      = answerInput,
                feedbackMessage  = feedbackMessage,
                onAnswerChange   = { answerInput = it },
                onSubmit         = {
                    val number = answerInput.toIntOrNull()
                    if (number == null || answerInput.length != 4) {
                        feedbackMessage = "⚠️ Entrez exactement 4 chiffres."
                    } else {
                        GameRepository.saveAnswer(context, number)
                        val morningNumber = GameRepository.getMorningNumber(context)
                        val score  = GameRepository.calculateScore(morningNumber, number)
                        val ratio  = (score * 100) / 4
                        val isWon  = score == 4
                        feedbackMessage = NotificationReceiver().buildResultMessage(
                            morningNumber, number, score, ratio, isWon
                        )
                        history = GameRepository.getHistory(context)
                        answerInput = ""
                    }
                }
            )
            1 -> HistoryScreen(
                modifier = Modifier.padding(padding),
                history  = history
            )
        }
    }
}

// ─── Écran Défi ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier,
    hasMorningNumber: Boolean,
    alreadyAnswered: Boolean,
    isEvening: Boolean,
    fieldEnabled: Boolean,
    answerInput: String,
    feedbackMessage: String?,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = "Entraînement\nde la mémoire",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        // ── Statut du jour ──
        StatusCard(hasMorningNumber, alreadyAnswered, isEvening)

        // ── Champ de réponse ──
        Text(
            text     = "Votre réponse :",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )

        OutlinedTextField(
            value         = answerInput,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) onAnswerChange(it) },
            label         = {
                Text(
                    if (!hasMorningNumber) "Aucun nombre reçu ce matin"
                    else if (!isEvening)   "Disponible à partir de 18h"
                    else if (alreadyAnswered) "Déjà répondu aujourd'hui ✓"
                    else "Entrez le nombre mémorisé"
                )
            },
            enabled              = fieldEnabled,
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth()
        )

        Button(
            onClick  = onSubmit,
            enabled  = fieldEnabled && answerInput.length == 4,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Valider ma réponse")
        }

        // ── Feedback ──
        feedbackMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = if (msg.contains("Parfait") || msg.contains("Bravo"))
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text     = msg,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    style    = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// ─── Carte de statut ──────────────────────────────────────────────────────────

@Composable
fun StatusCard(hasMorningNumber: Boolean, alreadyAnswered: Boolean, isEvening: Boolean) {
    val (emoji, text, bgColor) = when {
        alreadyAnswered  -> Triple("✅", "Défi du jour complété !", Color(0xFF4CAF50).copy(alpha = 0.12f))
        isEvening && hasMorningNumber -> Triple("🎯", "C'est l'heure ! Entrez votre réponse ci-dessous.", Color(0xFFFF9800).copy(alpha = 0.12f))
        hasMorningNumber && !isEvening -> Triple("⏳", "Nombre reçu ce matin. Revenez à 18h pour répondre !", Color(0xFF2196F3).copy(alpha = 0.12f))
        else -> Triple("😴", "Aucun nombre reçu aujourd'hui. La notification arrive à 8h demain !", Color(0xFF9E9E9E).copy(alpha = 0.12f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }

    }
}

// ─── Écran Historique ─────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(modifier: Modifier, history: List<GameEntry>) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = "Historique",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(bottom = 8.dp)
        )

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune partie enregistrée.", color = Color.Gray)
            }
        } else {
            // Statistiques globales
            val answered = history.filter { it.playerAnswer != null }
            if (answered.isNotEmpty()) {
                val avgRatio = answered.map { it.ratioPercent }.average().toInt()
                val wins     = answered.count { it.isWon }

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

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun HistoryCard(entry: GameEntry) {
    val bgColor = when {
        entry.playerAnswer == null -> Color(0xFF9E9E9E).copy(alpha = 0.08f)
        entry.isWon                -> Color(0xFF4CAF50).copy(alpha = 0.10f)
        entry.ratioPercent >= 50   -> Color(0xFFFF9800).copy(alpha = 0.10f)
        else                       -> Color(0xFFF44336).copy(alpha = 0.10f)
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
                    text       = "Nombre : ${entry.morningNumber}",
                    fontWeight = FontWeight.Medium
                )
                if (entry.playerAnswer != null) {
                    Text(
                        text  = "Réponse : ${entry.playerAnswer}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(text = "Sans réponse", fontSize = 13.sp, color = Color.Gray)
                }
            }

            if (entry.playerAnswer != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${entry.score}/4",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                    Text(
                        text     = "${entry.ratioPercent}%",
                        fontSize = 13.sp,
                        color    = when {
                            entry.isWon              -> Color(0xFF4CAF50)
                            entry.ratioPercent >= 50 -> Color(0xFFFF9800)
                            else                     -> Color(0xFFF44336)
                        }
                    )
                }
            } else {
                Text(text = "—", color = Color.Gray, fontSize = 20.sp)
            }
        }
    }
}