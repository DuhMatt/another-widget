package com.tommasoberlose.anotherwidget.shizuku

import androidx.annotation.Keep
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Runs inside Shizuku/Sui's UserService process. It deliberately exposes only
 * the exact alarm time needed by the widget instead of returning raw dumpsys
 * output to the normal app process.
 */
@Keep
class PrivilegedAlarmService : IPrivilegedAlarmService.Stub() {

    override fun destroy() {
        System.exit(0)
    }

    override fun getNextAlarmAlertTime(): Long {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/dumpsys", "alarm"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            AlarmDumpParser.findNextAlarmAlertTime(output) ?: -1L
        } catch (_: Throwable) {
            -1L
        }
    }
}

internal object AlarmDumpParser {
    private const val DESKCLOCK_ALERT_TAG = "tag=*walarm*:com.android.deskclock.ALARM_ALERT"
    private val blockPattern = Regex(
        "(?ms)^\\s*(?:RTC_WAKEUP|RTC)\\b.*?(?=^\\s*(?:RTC_WAKEUP|RTC)\\b|\\z)"
    )
    private val triggerPattern = Regex("^triggerTime=(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})$")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getDefault()
    }

    fun findNextAlarmAlertTime(output: String, now: Long = System.currentTimeMillis()): Long? {
        val pendingAlarms = output.substringAfter(" pending alarms:", output)
            .substringBefore("Past-due", missingDelimiterValue = output.substringAfter(" pending alarms:", output))

        return blockPattern.findAll(pendingAlarms)
            .mapNotNull { match ->
                val lines = match.value.lineSequence().map { it.trim() }.toList()
                if (DESKCLOCK_ALERT_TAG !in lines) return@mapNotNull null

                val triggerText = lines.firstNotNullOfOrNull { line ->
                    triggerPattern.matchEntire(line)?.groupValues?.get(1)
                } ?: return@mapNotNull null

                val parsed = dateFormat.parse(triggerText, ParsePosition(0))?.time
                    ?: return@mapNotNull null
                parsed.takeIf { it > now }
            }
            .minOrNull()
    }
}
