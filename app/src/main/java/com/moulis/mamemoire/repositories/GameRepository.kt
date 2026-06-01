package com.moulis.mamemoire.repositories

import android.content.Context
import androidx.core.content.edit
import com.moulis.mamemoire.models.GameEntry
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object GameRepository {

    private const val PREFS_NAME = "MemoryApp"
    private const val KEY_HISTORY = "game_history"
    private const val KEY_MORNING_NUMBER = "morning_number"
    private const val KEY_MORNING_DATE = "morning_date"
    private const val KEY_REVEALED_DATE = "revealed_date"
    private const val KEY_DIGITS_COUNT = "digits_count"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_EVENING_HOUR = "evening_hour"
    private const val KEY_THEME_MODE = "theme_mode"

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ─── Theme ────────────────────────────────────────────────────────────────

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun saveThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_THEME_MODE, mode)
        }
    }

    // ─── Nombre du matin ──────────────────────────────────────────────────────

    fun saveMorningNumber(context: Context, number: Int) {
        val today = dateFormat.format(Date())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_MORNING_NUMBER, number)
            putString(KEY_MORNING_DATE, today)
            remove(KEY_REVEALED_DATE)
        }
    }

    fun getMorningNumber(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MORNING_NUMBER, -1)
    }

    fun getMorningDate(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MORNING_DATE, null)
    }

    fun todayHasMorningNumber(context: Context): Boolean {
        val today = dateFormat.format(Date())
        return getMorningDate(context) == today && getMorningNumber(context) != -1
    }

    fun todayAlreadyAnswered(context: Context): Boolean {
        return getTodayEntry(context) != null
    }

    fun getTodayEntry(context: Context): GameEntry? {
        val today = dateFormat.format(Date())
        return getHistory(context).find { it.date == today && it.playerAnswer != null }
    }

    fun isRevealed(context: Context): Boolean {
        val today = dateFormat.format(Date())
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REVEALED_DATE, null) == today
    }

    fun setRevealed(context: Context) {
        val today = dateFormat.format(Date())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_REVEALED_DATE, today)
        }
    }

    // ─── Paramètres ───────────────────────────────────────────────────────────

    fun getDigitsCount(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DIGITS_COUNT, 4)
    }

    fun saveDigitsCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_DIGITS_COUNT, count)
        }
    }

    fun getMorningHour(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MORNING_HOUR, 8)
    }

    fun saveMorningHour(context: Context, hour: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_MORNING_HOUR, hour)
        }
    }

    fun getEveningHour(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_EVENING_HOUR, 18)
    }

    fun saveEveningHour(context: Context, hour: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_EVENING_HOUR, hour)
        }
    }

    // ─── Calcul du ratio ──────────────────────────────────────────────────────

    fun calculateScore(morningNumber: Int, playerAnswer: Int, total: Int): Int {
        val expected = morningNumber.toString().padStart(total, '0')
        val given    = playerAnswer.toString().padStart(total, '0')
        return expected.zip(given).count { (e, g) -> e == g }
    }

    // ─── Sauvegarde d'une réponse ─────────────────────────────────────────────

    fun saveAnswer(context: Context, playerAnswer: Int) {
        if (todayAlreadyAnswered(context)) return // Sécurité : pas de double réponse

        val morningNumber = getMorningNumber(context)
        if (morningNumber == -1) return

        val today = dateFormat.format(Date())
        val total = getDigitsCount(context)
        val score = calculateScore(morningNumber, playerAnswer, total)

        val entry = GameEntry(
            date          = today,
            morningNumber = morningNumber,
            playerAnswer  = playerAnswer,
            score         = score,
            total         = total
        )

        val history = getHistory(context).toMutableList()
        // Remplace si une entrée du jour existe déjà sans réponse
        history.removeAll { it.date == today }
        history.add(0, entry) // plus récent en premier

        saveHistory(context, history)
    }

    // ─── Historique ───────────────────────────────────────────────────────────

    fun getHistory(context: Context): List<GameEntry> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                GameEntry(
                    date          = obj.getString("date"),
                    morningNumber = obj.getInt("morningNumber"),
                    playerAnswer  = if (obj.isNull("playerAnswer")) null else obj.getInt("playerAnswer"),
                    score         = obj.getInt("score"),
                    total         = obj.optInt("total", 4)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(context: Context, history: List<GameEntry>) {
        val array = JSONArray()
        history.forEach { entry ->
            val obj = JSONObject().apply {
                put("date",          entry.date)
                put("morningNumber", entry.morningNumber)
                if (entry.playerAnswer != null) put("playerAnswer", entry.playerAnswer) else put("playerAnswer", JSONObject.NULL)
                put("score",         entry.score)
                put("total",         entry.total)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_HISTORY, array.toString())
        }
    }
}
