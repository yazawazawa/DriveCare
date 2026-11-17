package com.takumi.drivecare

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// 給油記録の計算
// ─────────────────────────────────────────────────────────────────────────────

/** 1件ごとの派生値（前回からの走行距離・満タン法による燃費）。 */
data class FuelRecordStats(
    val distanceFromPrevious: Double?,
    val efficiency: Double?
)

/**
 * 同一車両の給油記録について、各記録の「前回からの走行距離」と「燃費」を一括で計算する。
 * 燃費は満タン法: 直前の満タン記録からの距離 ÷ 今回の給油量（今回も満タン時のみ）。
 */
fun computeFuelStats(records: List<FuelRecord>): Map<Int, FuelRecordStats> {
    val result = HashMap<Int, FuelRecordStats>(records.size)
    records.groupBy { it.vehicleId }.forEach { (_, list) ->
        val sorted = list.sortedBy { it.timestamp }
        var lastFull: FuelRecord? = null
        sorted.forEachIndexed { i, current ->
            val distance = if (i > 0) {
                (current.odometer - sorted[i - 1].odometer).takeIf { it >= 0 }
            } else null
            val efficiency = if (current.isFullTank) {
                lastFull?.let { prev ->
                    val d = current.odometer - prev.odometer
                    if (d > 0 && current.liters > 0) d / current.liters else null
                }
            } else null
            result[current.id] = FuelRecordStats(distance, efficiency)
            if (current.isFullTank) lastFull = current
        }
    }
    return result
}

/** 平均燃費（km/L）。計算できる区間が無ければ null。 */
fun averageFuelEfficiency(records: List<FuelRecord>): Double? {
    val values = computeFuelStats(records).values.mapNotNull { it.efficiency }
    return if (values.isEmpty()) null else values.average()
}

// ─────────────────────────────────────────────────────────────────────────────
// フォーマット
// ─────────────────────────────────────────────────────────────────────────────

fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.JAPAN, "%.1f", value)
    }
}

/** 3桁区切りの数値（小数点以下は1桁まで）。 */
fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.JAPAN, "%,d", value.toLong())
    } else {
        String.format(Locale.JAPAN, "%,.1f", value)
    }
}

fun formatYen(value: Int): String = String.format(Locale.JAPAN, "¥%,d", value)
fun formatYen(value: Double): String = String.format(Locale.JAPAN, "¥%,d", Math.round(value))

fun formatUnitPrice(value: Double): String = String.format(Locale.JAPAN, "%.1f", value)

fun formatEfficiency(value: Double): String = String.format(Locale.JAPAN, "%.2f", value)

fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))

fun formatDateOnly(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(timestamp))

fun formatTimeOnly(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date(timestamp))

/** 例: 11月17日(月) */
fun formatMonthDayWeek(timestamp: Long): String =
    SimpleDateFormat("M月d日(E)", Locale.JAPAN).format(Date(timestamp))

/** 例: 2026年11月17日(月) */
fun formatFullDate(timestamp: Long): String =
    SimpleDateFormat("yyyy年M月d日(E)", Locale.JAPAN).format(Date(timestamp))

/** 例: 2026年11月 */
fun formatYearMonth(timestamp: Long): String =
    SimpleDateFormat("yyyy年M月", Locale.JAPAN).format(Date(timestamp))

// ─────────────────────────────────────────────────────────────────────────────
// 日付ピッカーとのタイムゾーン変換
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Material3 DatePicker は「UTC の 0 時」を選択値として扱う。
 * ローカル時刻のタイムスタンプを、その日付の UTC 0 時に変換する（初期値用）。
 */
fun localMillisToPickerMillis(localMillis: Long): Long {
    val date = Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

/**
 * DatePicker の選択値（UTC 0 時）を、ローカルタイムゾーンの同じ日付に変換する。
 * 時刻部分は [keepTimeOf] の時刻を引き継ぐ（記録の並び順が崩れないように）。
 * 以前はUTC値をそのまま保存していたため、UTCより西のタイムゾーンでは前日になる不具合があった。
 */
fun pickerMillisToLocalMillis(pickerMillis: Long, keepTimeOf: Long): Long {
    val zone = ZoneId.systemDefault()
    val date: LocalDate = Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val time = Instant.ofEpochMilli(keepTimeOf).atZone(zone).toLocalTime()
    return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}

// ─────────────────────────────────────────────────────────────────────────────
// 入力のサニタイズ
// ─────────────────────────────────────────────────────────────────────────────

fun sanitizeDecimalInput(input: String): String {
    val cleaned = input
        .replace('．', '.')
        .filter { it.isDigit() || it == '.' }
        .map { c -> if (c in '０'..'９') ('0' + (c - '０')) else c }
        .joinToString("")

    val firstDotIndex = cleaned.indexOf('.')
    return if (firstDotIndex >= 0) {
        val beforeDot = cleaned.substring(0, firstDotIndex + 1)
        val afterDot = cleaned.substring(firstDotIndex + 1).replace(".", "")
        beforeDot + afterDot
    } else {
        cleaned
    }
}

fun sanitizeIntInput(input: String): String {
    return input
        .filter { it.isDigit() }
        .map { c -> if (c in '０'..'９') ('0' + (c - '０')) else c }
        .joinToString("")
}

fun sanitizeTimeInput(input: String): String =
    input.filter { it.isDigit() || it == ':' }.take(5)

/** "HH:mm" を指定日付のタイムスタンプに合成する。形式が不正なら null。 */
fun combineDateAndTime(dateMillis: Long, timeText: String): Long? {
    val parts = timeText.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
    return date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
}
