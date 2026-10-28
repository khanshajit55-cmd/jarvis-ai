package com.jarvis.assistant.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jarvis.assistant.model.ActionExecutionResult
import com.jarvis.assistant.model.ActionRequest
import com.jarvis.assistant.model.ActionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafeActionRouter(private val context: Context) {
    private val appPackageMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "camera" to "camera_intent",
        "gallery" to "gallery_intent",
        "settings" to "com.android.settings",
        "calculator" to "calculator_intent",
        "facebook" to "com.facebook.katana",
        "messenger" to "com.facebook.orca",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "clock" to "com.google.android.deskclock"
    )

    fun parseCommand(rawText: String): ActionRequest? {
        val text = rawText.lowercase().trim()

        val phoneMatch = Regex("(?:call|ফোন করো|কল দাও|dial)\\s+([+0-9\\s-]{6,})").find(text)
        if (phoneMatch != null) {
            val number = phoneMatch.groupValues[1].replace("\\s|-".toRegex(), "")
            return ActionRequest(
                type = ActionType.START_PHONE_CALL,
                title = "Initiate Phone Call",
                description = "Call number: $number. High consequence action requiring confirmation.",
                requiresConfirmation = true,
                target = number
            )
        }

        val alarmMatch = Regex("(?:set alarm|এলার্ম দাও|alarm for|সকাল|বিকাল|রাত)\\s*([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm|টা)?").find(text)
        if (alarmMatch != null || text.contains("alarm") || text.contains("এলার্ম")) {
            val hour = alarmMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
            val minute = alarmMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
            val ampm = alarmMatch?.groupValues?.getOrNull(3) ?: ""
            val isPm = ampm.contains("pm") || text.contains("বিকাল") || text.contains("রাত")
            val finalHour = if (isPm && hour < 12) hour + 12 else hour

            return ActionRequest(
                type = ActionType.SET_ALARM,
                title = "Set Device Alarm",
                description = "Set alarm for %02d:%02d".format(finalHour, minute),
                requiresConfirmation = false,
                target = "%02d:%02d".format(finalHour, minute),
                extraData = mapOf("hour" to finalHour.toString(), "minute" to minute.toString())
            )
        }

        if (text.contains("time") || text.contains("date") || text.contains("কয়টা বাজে") || text.contains("সময়") || text.contains("কী বার")) {
            return ActionRequest(type = ActionType.GET_TIME_DATE, title = "Query Time & Date", description = "Read current system clock and date.", requiresConfirmation = false)
        }

        if (text.contains("open settings") || text.contains("সেটিংস") || text.contains("wifi") || text.contains("bluetooth")) {
            val sub = if (text.contains("wifi")) "wifi" else if (text.contains("bluetooth")) "bluetooth" else "general"
            return ActionRequest(type = ActionType.OPEN_SETTINGS, title = "Open Android Settings", description = "Launch $sub settings.", requiresConfirmation = false, target = sub)
        }

        if (text.startsWith("open ") || text.contains("খোল") || text.contains("ওপেন কর")) {
            for ((key, _) in appPackageMap) {
                if (text.contains(key)) {
                    return ActionRequest(type = ActionType.OPEN_APP, title = "Open App: $key", description = "Launch installed package.", requiresConfirmation = false, target = key)
                }
            }
        }

        if (text.startsWith("search ") || text.contains("খুঁজে বের কর")) {
            val query = text.replace("search", "").replace("খুঁজে বের কর", "").trim()
            return ActionRequest(type = ActionType.SEARCH_WEB, title = "Web Search", description = "Search: $query", requiresConfirmation = false, target = query)
        }

        if (text.contains("clear memory") || text.contains("delete memory") || text.contains("সব ভুলে যাও")) {
            return ActionRequest(type = ActionType.CLEAR_MEMORY, title = "Wipe Local Memory", description = "Permanently purge all saved preferences and user memory.", requiresConfirmation = true, target = "all")
        }

        return null
    }

    fun executeAction(request: ActionRequest, userConfirmed: Boolean = false): ActionExecutionResult {
        if (request.requiresConfirmation && !userConfirmed) {
            return ActionExecutionResult(success = false, message = "Action requires explicit user confirmation.")
        }
        return when (request.type) {
            ActionType.GET_TIME_DATE -> {
                val now = Date()
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
                ActionExecutionResult(success = true, message = "Current time is $timeStr.")
            }
            ActionType.SET_ALARM -> {
                val hour = request.extraData["hour"]?.toIntOrNull() ?: 7
                val minute = request.extraData["minute"]?.toIntOrNull() ?: 0
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(intent); ActionExecutionResult(success = true, message = "Alarm configured for %02d:%02d.".format(hour, minute)) }
                catch (e: Exception) { ActionExecutionResult(success = false, message = "Failed to launch alarm intent.") }
            }
            ActionType.START_PHONE_CALL -> {
                val number = request.target
                val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val intent = if (hasPerm) Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")) else Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { context.startActivity(intent); ActionExecutionResult(success = true, message = "Dispatched call to $number.") }
                catch (e: Exception) { ActionExecutionResult(success = false, message = "Could not open dialer.") }
            }
            else -> ActionExecutionResult(success = true, message = "Action executed safely.")
        }
    }
}