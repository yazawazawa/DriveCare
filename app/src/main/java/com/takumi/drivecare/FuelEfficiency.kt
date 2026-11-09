package com.takumi.drivecare

import java.math.BigDecimal
import java.util.Locale

/**
 * 満タン法で求めた1区間分の燃費。
 * [record] は区間の終わり（今回の満タン給油）、[distance] は前回満タンからの走行距離、
 * [liters] は前回満タンの次の給油から今回の満タン給油までに入れた燃料の合計。
 */
data class FuelEfficiencyEntry(
    val record: FuelRecord,
    val distance: Double,
    val liters: Double
) {
    val kmPerLiter: Double get() = distance / liters
}

/**
 * 1台分の給油記録から満タン法で燃費を計算する（燃費計算はすべてここを通す）。
 *
 * 満タン → 継ぎ足し → 満タン のように途中に満タンでない給油がある場合、
 * 走行距離は満タン間の距離、燃料は継ぎ足し分も含めた合計で割る。
 * 走行距離が0以下の区間（入力ミスなど）は結果に含めない。
 */
fun calculateFuelEfficiencyEntries(records: List<FuelRecord>): List<FuelEfficiencyEntry> {
    val sorted = records.sortedBy { it.timestamp }
    val result = mutableListOf<FuelEfficiencyEntry>()
    var lastFull: FuelRecord? = null
    var litersSinceLastFull = 0.0

    for (record in sorted) {
        // 最初の満タン給油より前の給油は、どれだけ入っていたか分からないので数えない
        if (lastFull != null) litersSinceLastFull += record.liters
        if (!record.isFullTank) continue

        val previousFull = lastFull
        if (previousFull != null) {
            val distance = record.odometer - previousFull.odometer
            if (distance > 0 && litersSinceLastFull > 0) {
                result.add(FuelEfficiencyEntry(record, distance, litersSinceLastFull))
            }
        }
        lastFull = record
        litersSinceLastFull = 0.0
    }
    return result
}

/** 複数車両分の記録を車両ごとに計算し、日付順にまとめる。 */
fun calculateFuelEfficiencyEntriesMultiVehicle(records: List<FuelRecord>): List<FuelEfficiencyEntry> =
    records.groupBy { it.vehicleId }
        .flatMap { (_, recs) -> calculateFuelEfficiencyEntries(recs) }
        .sortedBy { it.record.timestamp }

fun formatFuelEfficiency(kmPerLiter: Double): String =
    String.format(Locale.JAPAN, "%.2f km/L", kmPerLiter)

/** 給油量の表示用。小数2桁まで（末尾の0は省く）。例: 31.71 → "31.71", 35.0 → "35" */
fun formatLiters(value: Double): String =
    BigDecimal(String.format(Locale.US, "%.2f", value)).stripTrailingZeros().toPlainString()

/** 編集欄の初期値用。保存されている値を丸めずにそのまま出す。 */
fun formatForEdit(value: Double): String =
    BigDecimal(value.toString()).stripTrailingZeros().toPlainString()

/**
 * 給油記録を保存する前の走行距離チェック。問題がなければ null。
 * - 前の記録より小さい（桁の入力漏れなど）
 * - 後の記録より大きい（過去の日付で記録した場合）
 * - 前の記録から1タンクでは走れない距離が増えている（桁の入力しすぎなど）
 *
 * @param records 同じ車両の給油記録
 * @param excludeId 編集中の記録のID（自分自身とは比較しない）
 */
fun checkOdometerInput(
    records: List<FuelRecord>,
    timestamp: Long,
    odometer: Double,
    tankCapacity: Double?,
    excludeId: Int? = null
): String? {
    val others = records.filter { it.id != excludeId }.sortedBy { it.timestamp }
    val previous = others.lastOrNull { it.timestamp <= timestamp }
    val next = others.firstOrNull { it.timestamp > timestamp }

    if (previous != null && odometer < previous.odometer) {
        return "走行距離 ${formatDouble(odometer)} km が、前の記録" +
            "（${formatDateOnly(previous.timestamp)}）の ${formatDouble(previous.odometer)} km より小さくなっています。" +
            "桁が抜けていないか確認してください。"
    }
    if (next != null && odometer > next.odometer) {
        return "走行距離 ${formatDouble(odometer)} km が、後の記録" +
            "（${formatDateOnly(next.timestamp)}）の ${formatDouble(next.odometer)} km より大きくなっています。"
    }
    if (previous != null && tankCapacity != null && tankCapacity > 0) {
        val distance = odometer - previous.odometer
        // 1タンクで走れる距離の目安（燃費50km/L相当）を超えたら桁の打ち間違いを疑う
        if (distance > tankCapacity * MAX_PLAUSIBLE_KM_PER_LITER) {
            return "前の記録（${formatDateOnly(previous.timestamp)}）から ${formatDouble(distance)} km 増えています。" +
                "桁が多くないか確認してください。"
        }
    }
    return null
}

private const val MAX_PLAUSIBLE_KM_PER_LITER = 50.0
