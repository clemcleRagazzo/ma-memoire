package com.moulis.mamemoire.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moulis.mamemoire.MemoryApp
import com.moulis.mamemoire.RevealableNumberCard
import com.moulis.mamemoire.StatusCard
import com.moulis.mamemoire.repositories.GameRepository
import androidx.compose.ui.tooling.preview.Preview
import com.moulis.mamemoire.ui.theme.MaMemoireTheme
import com.moulis.mamemoire.models.GameEntry

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
    isAlreadyRevealed: Boolean? = null,
    digitsCount: Int,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val revealed = isAlreadyRevealed ?: remember { GameRepository.isRevealed(context) }
    
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
        val alreadyAnswered = todayEntry != null
        val morningNumber = GameRepository.getMorningNumber(context)
        val currentDigitsCount = digitsCount

        if (hasNumber && !alreadyAnswered && !revealed) {
            RevealableNumberCard(
                number = morningNumber,
                initiallyRevealed = initialReveal,
                total = currentDigitsCount
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
            onValueChange = { if (it.length <= currentDigitsCount && it.all(Char::isDigit)) onAnswerChange(it) },
            label         = {
                val eveningHour = GameRepository.getEveningHour(context)
                Text(
                    if (!hasNumber) "Aucun nombre reçu ce matin"
                    else if (!isEvening)   "Disponible à partir de ${eveningHour}h"
                    else if (alreadyAnswered) "Déjà répondu : $morningNumber"
                    else "Entrez les $currentDigitsCount chiffres"
                )
            },
            enabled              = fieldEnabled,
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth()
        )

        Button(
            onClick  = onSubmit,
            enabled  = fieldEnabled && answerInput.length == currentDigitsCount,
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

@Preview(showBackground = true, name = "7h - Pas de numéro")
@Composable
fun PreviewHome7h() {
    MaMemoireTheme {
        HomeScreen(
            modifier = Modifier,
            hasNumber = false,
            todayEntry = null,
            isEvening = false,
            fieldEnabled = false,
            answerInput = "",
            feedbackMessage = null,
            digitsCount = 4,
            onAnswerChange = {},
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true, name = "8h - Numéro à découvrir")
@Composable
fun PreviewHome8h() {
    MaMemoireTheme {
        HomeScreen(
            modifier = Modifier,
            hasNumber = true,
            todayEntry = null,
            isEvening = false,
            fieldEnabled = false,
            answerInput = "",
            feedbackMessage = null,
            initialReveal = false,
            isAlreadyRevealed = false,
            digitsCount = 4,
            onAnswerChange = {},
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true, name = "9h - Numéro découvert")
@Composable
fun PreviewHome9h() {
    MaMemoireTheme {
        HomeScreen(
            modifier = Modifier,
            hasNumber = true,
            todayEntry = null,
            isEvening = false,
            fieldEnabled = false,
            answerInput = "",
            feedbackMessage = null,
            initialReveal = true,
            isAlreadyRevealed = false,
            digitsCount = 4,
            onAnswerChange = {},
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true, name = "18h - En attente de réponse")
@Composable
fun PreviewHome18hPending() {
    MaMemoireTheme {
        HomeScreen(
            modifier = Modifier,
            hasNumber = true,
            todayEntry = null,
            isEvening = true,
            fieldEnabled = true,
            answerInput = "",
            feedbackMessage = null,
            digitsCount = 4,
            onAnswerChange = {},
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true, name = "18h - Réponse saisie")
@Composable
fun PreviewHome18h() {
    MaMemoireTheme {
        HomeScreen(
            modifier = Modifier,
            hasNumber = true,
            todayEntry = null,
            isEvening = true,
            fieldEnabled = true,
            answerInput = "12",
            feedbackMessage = null,
            digitsCount = 4,
            onAnswerChange = {},
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true, name = "19h - Défi réussi 50%")
@Composable
fun PreviewHome19h() {
    MaMemoireTheme {
        HomeScreen(
            modifier = Modifier,
            hasNumber = true,
            todayEntry = GameEntry("01/07/2026", 1234, 1256, 2.0),
            isEvening = true,
            fieldEnabled = false,
            answerInput = "1256",
            feedbackMessage = "Bravo ! Vous avez trouvé 2 chiffres sur 4.",
            digitsCount = 4,
            onAnswerChange = {},
            onSubmit = {}
        )
    }
}
