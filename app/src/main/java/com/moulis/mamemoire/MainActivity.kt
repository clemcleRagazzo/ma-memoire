package com.moulis.mamemoire

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moulis.mamemoire.models.GameEntry
import com.moulis.mamemoire.repositories.GameRepository
import java.util.Calendar
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val notificationPermissionRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as MemoryApp).scheduleNotifications()
        requestNotificationPermission()

        val shouldReveal = intent?.getBooleanExtra("REVEAL_NUMBER", false) ?: false
        val forceEvening = intent?.getBooleanExtra("FORCE_EVENING", false) ?: false

        setContent {
            MaterialTheme {
                MemoryAppUI(autoReveal = shouldReveal, forceEvening = forceEvening)
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
                    notificationPermissionRequestCode
                )
            }
        }
    }
}

// ─── Utilitaire heure ─────────────────────────────────────────────────────────

/** true si on est entre 18h00 et 23h59 */
private fun isEveningTime(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= MemoryApp.EVENING_HOUR
}

// ─── UI principale ────────────────────────────────────────────────────────────

@Composable
fun MemoryAppUI(autoReveal: Boolean = false, forceEvening: Boolean = false) {
    val context = LocalContext.current

    // Onglet actif : 0 = Accueil, 1 = Historique, 2 = Paramètres
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // État pour la révélation forcée via notification
    val initialReveal = remember { autoReveal }

    // Recharge l'état à chaque recomposition
    var history by remember { mutableStateOf(GameRepository.getHistory(context)) }
    var answerInput by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    val digitsCount = GameRepository.getDigitsCount(context)

    val todayEntry = history.find { 
        val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        it.date == today && it.playerAnswer != null 
    }
    val hasNumber = GameRepository.todayHasMorningNumber(context)
    val alreadyAnswered  = todayEntry != null
    val isEvening        = forceEvening || isEveningTime()

    // Champ dispo uniquement le soir + nombre reçu + pas encore répondu
    val fieldEnabled = isEvening && hasNumber && !alreadyAnswered

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
                NavigationBarItem(
                    selected  = selectedTab == 2,
                    onClick   = { selectedTab = 2 },
                    icon      = { Text("⚙️") },
                    label     = { Text("Paramètres") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier        = Modifier.padding(padding),
                hasNumber       = hasNumber,
                todayEntry      = todayEntry,
                isEvening       = isEvening,
                fieldEnabled    = fieldEnabled,
                answerInput     = answerInput,
                feedbackMessage = feedbackMessage,
                initialReveal   = initialReveal,
                digitsCount     = digitsCount,
                onAnswerChange  = { answerInput = it },
                onSubmit        = {
                    val playerAnswer = answerInput.toIntOrNull()
                    if (playerAnswer == null || answerInput.length != digitsCount) {
                        feedbackMessage = "⚠️ Entrez exactement $digitsCount chiffres."
                    } else {
                        GameRepository.saveAnswer(context, playerAnswer)
                        val number = GameRepository.getMorningNumber(context)
                        val score  = GameRepository.calculateScore(number, playerAnswer, digitsCount)
                        val ratio  = (score * 100) / digitsCount
                        val isWon  = score == digitsCount
                        feedbackMessage = NotificationReceiver().buildResultMessage(
                            number, playerAnswer, score, ratio, isWon, digitsCount
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
            2 -> SettingsScreen(
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ─── Écran Défi ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier,
    hasNumber: Boolean,
    todayEntry: GameEntry?,
    isEvening: Boolean,
    fieldEnabled: Boolean,
    answerInput: String,
    feedbackMessage: String?,
    initialReveal: Boolean = false,
    digitsCount: Int,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()
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
            text       = "Entraînement\nde la mémoire",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        // ── Statut du jour ──
        val context = LocalContext.current
        val isAlreadyRevealed = remember { GameRepository.isRevealed(context) }
        val alreadyAnswered = todayEntry != null

        if (hasNumber && !alreadyAnswered && !isAlreadyRevealed) {
            val morningNumber = GameRepository.getMorningNumber(context)
            RevealableNumberCard(
                number = morningNumber,
                initiallyRevealed = initialReveal,
                total = digitsCount
            )
        } else {
            StatusCard(hasNumber, todayEntry, isEvening)
        }

        // ── Champ de réponse ──
        Text(
            text     = "Votre réponse :",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )

        OutlinedTextField(
            value         = answerInput,
            onValueChange = { if (it.length <= digitsCount && it.all(Char::isDigit)) onAnswerChange(it) },
            label         = {
                Text(
                    if (!hasNumber) "Aucun nombre reçu ce matin"
                    else if (!isEvening)   "Disponible à partir de ${MemoryApp.EVENING_HOUR}h"
                    else if (alreadyAnswered) "Déjà répondu aujourd'hui ✓"
                    else "Entrez les $digitsCount chiffres"
                )
            },
            enabled              = fieldEnabled,
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth()
        )

        Button(
            onClick  = onSubmit,
            enabled  = fieldEnabled && answerInput.length == digitsCount,
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

// ─── Écran Paramètres ─────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    var digitsCount by remember { mutableFloatStateOf(GameRepository.getDigitsCount(context).toFloat()) }
    val scrollState = rememberScrollState()

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
        
        Text(
            text = "Version 1.1.0",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

// ─── Carte de statut ──────────────────────────────────────────────────────────

@Composable
fun RevealableNumberCard(number: Int, initiallyRevealed: Boolean = false, total: Int) {
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(initiallyRevealed) }
    
    LaunchedEffect(revealed) {
        if (revealed) {
            GameRepository.setRevealed(context)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "RevealProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!revealed) Modifier.clickable { revealed = true } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!revealed && animatedProgress == 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨ ", fontSize = 20.sp)
                    Text(
                        "Révéler le numéro du jour",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = number.toString().padStart(total, '0'),
                    fontSize = if (total > 6) 24.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = if (total > 6) 4.sp else 6.sp,
                    modifier = Modifier.blur(if (revealed) 0.dp else 10.dp)
                )

                if (animatedProgress < 1f) {
                    GrainDissolveOverlay(progress = animatedProgress)
                }
            }
        }
    }
}

@Composable
fun GrainDissolveOverlay(progress: Float) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val particles = remember {
        List(250) {
            Offset(Random.nextFloat(), Random.nextFloat())
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val sandColor = Color.Gray.copy(alpha = (1f - progress).coerceIn(0f, 1f))
        val radiusPx = with(density) { 1.5.dp.toPx() }
        
        particles.forEach { dot ->
            if (Random.nextFloat() > progress) {
                val noiseX = (Random.nextFloat() - 0.5f) * progress * 60f
                val noiseY = (Random.nextFloat() - 0.5f) * progress * 60f
                
                val x = dot.x * w + noiseX
                val y = dot.y * h + (progress * h * 0.3f) + noiseY
                
                drawCircle(
                    color = sandColor,
                    radius = radiusPx,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun StatusCard(hasMorningNumber: Boolean, todayEntry: GameEntry?, isEvening: Boolean) {
    val (emoji, text, bgColor) = when {
        todayEntry != null -> {
            val score = todayEntry.score
            val total = todayEntry.total
            val msg = when {
                score == total -> "Parfait ! $score/$total chiffres trouvés 🏆"
                score >= total / 2 -> "Bien joué ! $score/$total chiffres trouvés 🥈"
                score > 0 -> "Un peu juste... $score/$total chiffre(s) trouvé(s) 🧱"
                else -> "Échec ! 0/$total chiffre trouvé 😅"
            }
            val color = when {
                score == total -> Color(0xFF4CAF50) // Vert
                score >= total / 2 -> Color(0xFFFF9800) // Orange
                else       -> Color(0xFFF44336) // Rouge
            }
            Triple("✅", "Défi complété : $msg", color.copy(alpha = 0.12f))
        }
        isEvening && hasMorningNumber -> Triple("🎯", "C'est l'heure ! Entrez votre réponse ci-dessous.", Color(0xFFFF9800).copy(alpha = 0.12f))
        hasMorningNumber -> Triple("⏳", "Nombre reçu ce matin. Revenez à ${MemoryApp.EVENING_HOUR}h pour répondre !", Color(0xFF2196F3).copy(alpha = 0.12f))
        else -> Triple("😴", "Aucun nombre reçu aujourd'hui. La notification arrive à ${MemoryApp.MORNING_HOUR}h demain !", Color(0xFF9E9E9E).copy(alpha = 0.12f))
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
        entry.ratioPercent == 100  -> Color(0xFF4CAF50).copy(alpha = 0.10f)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${entry.score}/${entry.total}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                    Text(
                        text     = "${entry.ratioPercent}%",
                        fontSize = 13.sp,
                        color    = when {
                            entry.ratioPercent == 100 -> Color(0xFF4CAF50)
                            entry.ratioPercent >= 50  -> Color(0xFFFF9800)
                            else                      -> Color(0xFFF44336)
                        }
                    )
                }
            } else {
                Text(text = "—", color = Color.Gray, fontSize = 20.sp)
            }
        }
    }
}
