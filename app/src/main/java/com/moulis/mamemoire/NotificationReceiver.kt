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
        const val ACTION_YES     = "ACTION_YES"
        const val ACTION_NO      = "ACTION_NO"
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
                (context.applicationContext as? MemoryApp)?.scheduleNotifications()
            }
            MemoryApp.ACTION_EVENING -> {
                sendEveningNotification(context)
                (context.applicationContext as? MemoryApp)?.scheduleNotifications()
            }
            ACTION_REPLY           -> handleReply(context, intent)
            ACTION_YES, ACTION_NO  -> {
                // On peut logger l'engagement ou simplement fermer la notification
                NotificationManagerCompat.from(context).cancel(MemoryApp.MORNING_NOTIFICATION_ID)
            }
        }
    }

    // ─── Matin ────────────────────────────────────────────────────────────────

    private fun sendMorningNotification(context: Context) {
        val total = GameRepository.getDigitsCount(context)
        val min = Math.pow(10.0, (total - 1).toDouble()).toInt()
        val max = Math.pow(10.0, total.toDouble()).toInt() - 1
        val randomNumber = (min..max).random()
        GameRepository.saveMorningNumber(context, randomNumber)
        Log.d("MemoryApp", "Nombre généré : $randomNumber")

        val yesIntent = Intent(context, NotificationReceiver::class.java).apply { action = ACTION_YES }
        val noIntent  = Intent(context, NotificationReceiver::class.java).apply { action = ACTION_NO }

        val yesPending = PendingIntent.getBroadcast(context, 101, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val noPending  = PendingIntent.getBroadcast(context, 102, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val actionYes = NotificationCompat.Action.Builder(null, "Oui, c'est fait ✅", yesPending).build()
        val actionNo  = NotificationCompat.Action.Builder(null, "Pas encore ⏳", noPending).build()

        val messages = listOf(
            "Votre défi : $randomNumber. Prêt ?",
            "Mémorisez bien ce nombre : $randomNumber",
            "Nouveau défi : $randomNumber 🧠",
            "Gardez $randomNumber en tête !"
        )

        showNotificationWithActions(
            context,
            MemoryApp.MORNING_NOTIFICATION_ID,
            "As-tu mémorisé ? 🤔",
            messages.random(),
            listOf(actionYes, actionNo)
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
        val total = GameRepository.getDigitsCount(context)
        if (playerAnswer == null || replyText.length != total) {
            showNotification(
                context,
                MemoryApp.EVENING_NOTIFICATION_ID,
                "Réponse invalide ❌",
                "Merci d'entrer uniquement un nombre à $total chiffres."
            )
            return
        }

        GameRepository.saveAnswer(context, playerAnswer)

        val morningNumber = GameRepository.getMorningNumber(context)
        val score         = GameRepository.calculateScore(morningNumber, playerAnswer, total)
        val ratio         = (score * 100) / total
        val isWon         = score == total

        val resultMessage = buildResultMessage(morningNumber, playerAnswer, score, ratio, isWon, total)

        showNotification(
            context,
            MemoryApp.EVENING_NOTIFICATION_ID,
            if (isWon) "Bravo ! 🎉" else "Résultat du jour",
            resultMessage
        )
        Log.d("MemoryApp", "Réponse enregistrée : $playerAnswer | Score: $score/$total | Ratio: $ratio%")
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    fun buildResultMessage(
        morningNumber: Int,
        playerAnswer: Int,
        score: Int,
        ratio: Int,
        isWon: Boolean,
        total: Int
    ): String {
        val morningStr = morningNumber.toString().padStart(total, '0')
        val answerStr = playerAnswer.toString().padStart(total, '0')
        return if (isWon) {
            "Parfait ! Le nombre était bien $morningStr. 100% de réussite 🏆"
        } else {
            "Le nombre était $morningStr, vous avez répondu $answerStr. $score/$total chiffres corrects ($ratio%)"
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        showNotificationWithActions(context, id, title, text, emptyList())
    }

    private fun showNotificationWithAction(
        context: Context,
        id: Int,
        title: String,
        text: String,
        action: NotificationCompat.Action?
    ) {
        showNotificationWithActions(context, id, title, text, listOfNotNull(action))
    }

    private fun showNotificationWithActions(
        context: Context,
        id: Int,
        title: String,
        text: String,
        actions: List<NotificationCompat.Action>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // On peut passer un flag pour indiquer qu'on veut révéler le nombre
            putExtra("REVEAL_NUMBER", true)
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
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)

        actions.forEach { builder.addAction(it) }

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
