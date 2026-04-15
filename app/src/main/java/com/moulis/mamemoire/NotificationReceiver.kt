package com.moulis.mamemoire

import android.Manifest
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moulis.mamemoire.repositories.GameRepository
import androidx.core.app.RemoteInput as RemoteInputCompat

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_REPLY_TEXT = "key_reply_text"
        const val ACTION_REPLY   = "ACTION_REPLY_EVENING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MemoryApp", "Broadcast reçu ! Action: ${intent.action}")
        
        // On ne reprogramme que sur les événements système critiques
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            (context.applicationContext as? MemoryApp)?.scheduleNotifications()
            return
        }

        when (intent.action) {
            MemoryApp.ACTION_MORNING -> {
                sendMorningNotification(context)
                // Optionnel : on prépare la suivante pour demain
                (context.applicationContext as? MemoryApp)?.scheduleNotifications()
            }
            MemoryApp.ACTION_EVENING -> {
                sendEveningNotification(context)
                // Optionnel : on prépare la suivante pour demain
                (context.applicationContext as? MemoryApp)?.scheduleNotifications()
            }
            ACTION_REPLY           -> handleReply(context, intent)
        }
    }

    // ─── Matin ────────────────────────────────────────────────────────────────

    private fun sendMorningNotification(context: Context) {
        val randomNumber = (1000..9999).random()
        GameRepository.saveMorningNumber(context, randomNumber)
        Log.d("MemoryApp", "Nombre généré : $randomNumber")

        val messages = listOf(
            "Votre défi du jour : $randomNumber. Mémorisez-le bien !",
            "C'est l'heure ! Gravez ce nombre dans votre mémoire : $randomNumber",
            "Nouvelle journée, nouveau défi : $randomNumber 🧠",
            "Gardez ce nombre en tête jusqu'à ce soir : $randomNumber"
        )
        showNotification(
            context,
            MemoryApp.MORNING_NOTIFICATION_ID,
            "Nombre du jour 🧠",
            messages.random()
        )
    }

    // ─── Soir ─────────────────────────────────────────────────────────────────

    private fun sendEveningNotification(context: Context) {
        val morningNumber = GameRepository.getMorningNumber(context)
        Log.d("MemoryApp", "Nombre récupéré pour le soir : $morningNumber")

        if (morningNumber == -1) {
            Log.e("MemoryApp", "Aucun nombre du matin trouvé !")
            return
        }

        val messages = listOf(
            "Alors, quel était le nombre de ce matin ? 🤔",
            "C'est l'heure du test ! Vous souvenez-vous ?",
            "La journée se termine. Quel était le nombre ?"
        )

        // Intent pour la réponse inline
        val replyIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_REPLY
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            99,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val remoteInput = RemoteInputCompat.Builder(KEY_REPLY_TEXT)
            .setLabel("Votre réponse...")
            .build()

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Répondre",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        showNotificationWithAction(
            context,
            MemoryApp.EVENING_NOTIFICATION_ID,
            "Souvenez-vous 🧐",
            messages.random(),
            replyAction
        )
    }

    // ─── Traitement de la réponse inline ──────────────────────────────────────

    private fun handleReply(context: Context, intent: Intent) {
        val bundle = RemoteInputCompat.getResultsFromIntent(intent) ?: return
        val replyText = bundle.getCharSequence(KEY_REPLY_TEXT)?.toString()?.trim() ?: return

        val playerAnswer = replyText.toIntOrNull()
        if (playerAnswer == null) {
            showNotification(
                context,
                MemoryApp.EVENING_NOTIFICATION_ID,
                "Réponse invalide ❌",
                "Merci d'entrer uniquement un nombre à 4 chiffres."
            )
            return
        }

        GameRepository.saveAnswer(context, playerAnswer)

        val morningNumber = GameRepository.getMorningNumber(context)
        val score         = GameRepository.calculateScore(morningNumber, playerAnswer)
        val ratio         = (score * 100) / 4
        val isWon         = score == 4

        val resultMessage = buildResultMessage(morningNumber, playerAnswer, score, ratio, isWon)

        showNotification(
            context,
            MemoryApp.EVENING_NOTIFICATION_ID,
            if (isWon) "Bravo ! 🎉" else "Résultat du jour",
            resultMessage
        )
        Log.d("MemoryApp", "Réponse enregistrée : $playerAnswer | Score: $score/4 | Ratio: $ratio%")
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    fun buildResultMessage(
        morningNumber: Int,
        playerAnswer: Int,
        score: Int,
        ratio: Int,
        isWon: Boolean
    ): String {
        return if (isWon) {
            "Parfait ! Le nombre était bien $morningNumber. 100% de réussite 🏆"
        } else {
            "Le nombre était $morningNumber, vous avez répondu $playerAnswer. $score/4 chiffres corrects ($ratio%)"
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        showNotificationWithAction(context, id, title, text, null)
    }

    private fun showNotificationWithAction(
        context: Context,
        id: Int,
        title: String,
        text: String,
        action: NotificationCompat.Action?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("MemoryApp", "ERREUR : Permission POST_NOTIFICATIONS non accordée !")
                return
            }
        }

        // Ajout de l'intention pour ouvrir l'app au clic sur la notification
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MemoryApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(mainPendingIntent) // Ouvre l'app ici
            .setAutoCancel(true)

        action?.let { builder.addAction(it) }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
            Log.d("MemoryApp", "Notification envoyée : $title")
        } catch (e: Exception) {
            Log.e("MemoryApp", "Erreur lors de l'envoi : ${e.message}")
        }
    }
}
