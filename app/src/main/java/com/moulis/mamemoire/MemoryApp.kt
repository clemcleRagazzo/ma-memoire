package com.moulis.mamemoire

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
                this,
                hour,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
}
