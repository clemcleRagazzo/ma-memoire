package com.moulis.mamemoire

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.moulis.mamemoire.models.GameEntry
import com.moulis.mamemoire.repositories.GameRepository
import com.moulis.mamemoire.screens.HistoryScreen
import com.moulis.mamemoire.screens.HomeScreen
import com.moulis.mamemoire.screens.SettingsScreen
import com.moulis.mamemoire.ui.theme.AppTheme
import com.moulis.mamemoire.ui.theme.MaMemoireTheme
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
            var themeModeStr by remember { mutableStateOf(GameRepository.getThemeMode(this)) }
            val themeMode = try { AppTheme.valueOf(themeModeStr) } catch (_: Exception) { AppTheme.SYSTEM }

            MaMemoireTheme(themeMode = themeMode) {
                MemoryAppUI(
                    autoReveal = shouldReveal,
                    forceEvening = forceEvening,
                    onThemeChanged = { themeModeStr = it }
                )
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

@Preview(showBackground = true)
@Composable
fun MemoryAppUIPreview() {
    MaMemoireTheme {
        MemoryAppUI()
    }
}

// ─── Utilitaire heure ─────────────────────────────────────────────────────────

private fun isEveningTime(context: Context): Boolean {
    val hour = java.time.LocalTime.now().hour
    return hour >= GameRepository.getEveningHour(context)
}

// ─── UI principale ────────────────────────────────────────────────────────────

@Composable
fun MemoryAppUI(
    autoReveal: Boolean = false,
    forceEvening: Boolean = false,
    onThemeChanged: (String) -> Unit = {}
) {
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
    val isEvening        = forceEvening || isEveningTime(context)

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
                        val ratio  = ((score * 100) / digitsCount).toInt()
                        val isWon  = score == digitsCount.toDouble()
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
                modifier = Modifier.padding(padding),
                onThemeChanged = onThemeChanged
            )
        }
    }
}

// ─── Écran Paramètres ─────────────────────────────────────────────────────────


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
    val context = LocalContext.current
    val (emoji, text, bgColor) = when {
        todayEntry != null -> {
            val score = todayEntry.score
            val total = todayEntry.total
            val scoreStr = if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
            val msg = when {
                score == total.toDouble() -> "Parfait ! \n $scoreStr/$total chiffres trouvés 🏆"
                score >= total.toDouble() / 2.0 -> "Bien joué ! \n $scoreStr/$total chiffres trouvés 🥈"
                score > 0 -> "Un peu juste... \n $scoreStr/$total chiffre(s) trouvé(s) 🧱"
                else -> "Échec ! \n 0/$total chiffre trouvé 😅"
            }
            val color = when {
                score == total.toDouble() -> Color(0xFF4CAF50) // Vert
                score >= total.toDouble() / 2.0 -> Color(0xFFFF9800) // Orange
                else       -> Color(0xFFF44336) // Rouge
            }
            Triple("✅", "Défi complété : $msg", color.copy(alpha = 0.12f))
        }
        isEvening && hasMorningNumber -> Triple("🎯", "C'est l'heure ! Entrez votre réponse ci-dessous.", Color(0xFFFF9800).copy(alpha = 0.12f))
        hasMorningNumber -> Triple("⏳", "Nombre reçu ce matin. Revenez à ${GameRepository.getEveningHour(context)}h pour répondre !", Color(0xFF2196F3).copy(alpha = 0.12f))
        else -> Triple("😴", "Aucun nombre reçu aujourd'hui. La notification arrive à ${GameRepository.getMorningHour(context)}h demain !", Color(0xFF9E9E9E).copy(alpha = 0.12f))
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

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}
