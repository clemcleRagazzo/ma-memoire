package com.moulis.mamemoire

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

class MemoryApp : Application() { // On ajoute l'héritage ici

    companion object {
        const val CHANNEL_ID = "memory_channel"
        const val MORNING_NOTIFICATION_ID = 1
        const val EVENING_NOTIFICATION_ID = 2
    }

    override fun onCreate() {
        super.onCreate()
        // On initialise tout au lancement de l'application
        createNotificationChannel(this)
    }

    fun scheduleNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val hours = listOf(8, 18)

        hours.forEach { hour ->
            val intent = Intent(this, NotificationReceiver::class.java).apply {
                action = if (hour == 8) "MORNING_NOTIFICATION" else "EVENING_NOTIFICATION"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this, hour, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Version fiable mais pas "exacte" : pas de permission requise, pas de crash.
            // L'OS lancera la notif quand il peut (marge de 1 à 10 min max).
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mémoire - Rappels",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications pour l'entraînement de la mémoire"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
