package com.moulis.mamemoire

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MemoryApp", "Broadcast reçu ! Action: ${intent.action}")
        when (intent.action) {
            "MORNING_NOTIFICATION" -> sendMorningNotification(context)
            "EVENING_NOTIFICATION" -> sendEveningNotification(context)
        }
    }

    private fun sendMorningNotification(context: Context) {
        val randomNumber = (1000..9999).random()
        context.getSharedPreferences("MemoryApp", Context.MODE_PRIVATE).edit {
            putInt("morning_number", randomNumber)
        }
        Log.d("MemoryApp", "Nombre généré : $randomNumber")

        val messages = listOf(
            "Bonjour ! Votre défi du jour est le : $randomNumber",
            "C'est l'heure ! Mémorisez bien ce nombre : $randomNumber",
            "Nouvelle journée, nouveau nombre : $randomNumber. Bonne chance !",
            "Gardez ce nombre en tête pour ce soir : $randomNumber"
        )
        showNotification(context, MemoryApp.MORNING_NOTIFICATION_ID, "Nombre du jour 🧠", messages.random())
    }

    private fun sendEveningNotification(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MemoryApp", Context.MODE_PRIVATE)
        val storedNumber = sharedPreferences.getInt("morning_number", -1)
        Log.d("MemoryApp", "Nombre récupéré : $storedNumber")

        val messages = listOf(
            "Alors, quel était le nombre de ce matin ? 🤔",
            "C'est l'heure du test ! Vous souvenez-vous du nombre ?",
            "La journée se termine. Le nombre était-il bien $storedNumber ?"
        )
        showNotification(context, MemoryApp.EVENING_NOTIFICATION_ID, "Souvenez-vous 🧐", messages.random())
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        // La permission n'est obligatoire qu'à partir d'Android 13 (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("MemoryApp", "ERREUR : Permission POST_NOTIFICATIONS non accordée !")
                return
            }
        }

        val notification = NotificationCompat.Builder(context, MemoryApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Utilise une icône système par défaut pour être sûr
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
            Log.d("MemoryApp", "Notification envoyée avec succès : $title")
        } catch (e: Exception) {
            Log.e("MemoryApp", "Erreur lors de l'envoi : ${e.message}")
        }
    }
}
