package com.takumi.drivecare

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalculationsTest {

    private fun fuel(id: Int, odo: Double, liters: Double, full: Boolean, t: Long, vehicle: Int = 1) =
        FuelRecord(id, vehicle, odo, liters, 0, 170.0, full, t)

    @Test
    fun `満タンどうしの燃費は距離を給油量で割る`() {
        val stats = computeFuelStats(listOf(fuel(1, 1000.0, 40.0, true, 1), fuel(2, 1400.0, 40.0, true, 2)))
        assertEquals(10.0, stats.getValue(2).efficiency!!, 1e-9)
        assertNull(stats.getValue(1).efficiency)
    }

    @Test
    fun `間に部分給油があると、その給油量も消費量に含める`() {
        // 実データ: 19491km満タン → 19876km(10L部分給油) → 20157km満タン41.4L
        val stats = computeFuelStats(
            listOf(
                fuel(18, 19491.0, 47.81, true, 1),
                fuel(22, 19876.0, 10.0, false, 2),
                fuel(24, 20157.0, 41.4, true, 3)
            )
        )
        assertEquals(666.0 / 51.4, stats.getValue(24).efficiency!!, 1e-9)
        assertNull("部分給油自体は燃費を計算しない", stats.getValue(22).efficiency)
    }

    @Test
    fun `最初の満タンより前の部分給油は消費量に含めない`() {
        val stats = computeFuelStats(
            listOf(fuel(1, 100.0, 5.0, false, 1), fuel(2, 200.0, 30.0, true, 2), fuel(3, 500.0, 30.0, true, 3))
        )
        assertNull(stats.getValue(2).efficiency)
        assertEquals(10.0, stats.getValue(3).efficiency!!, 1e-9)
    }

    @Test
    fun `車両ごとに別々に計算する`() {
        val stats = computeFuelStats(
            listOf(
                fuel(1, 1000.0, 40.0, true, 1, vehicle = 1),
                fuel(2, 50.0, 10.0, true, 2, vehicle = 2),
                fuel(3, 1400.0, 40.0, true, 3, vehicle = 1),
                fuel(4, 200.0, 10.0, true, 4, vehicle = 2)
            )
        )
        assertEquals(10.0, stats.getValue(3).efficiency!!, 1e-9)
        assertEquals(15.0, stats.getValue(4).efficiency!!, 1e-9)
    }

    @Test
    fun `走行距離が戻っている記録は燃費を計算しない`() {
        val stats = computeFuelStats(listOf(fuel(1, 2000.0, 40.0, true, 1), fuel(2, 1500.0, 40.0, true, 2)))
        assertNull(stats.getValue(2).efficiency)
        assertNull(stats.getValue(2).distanceFromPrevious)
    }

    @Test
    fun `燃費の系列は計算できた記録だけを時刻順に返す`() {
        val series = buildEfficiencySeriesRaw(
            listOf(fuel(3, 1800.0, 40.0, true, 30), fuel(1, 1000.0, 40.0, true, 10), fuel(2, 1400.0, 40.0, true, 20))
        )
        assertEquals(listOf(20L, 30L), series.map { it.first })
    }

    @Test
    fun `全角の数字と小数点も受け付ける`() {
        assertEquals("12.5", sanitizeDecimalInput("１２．５"))
        assertEquals("1.53", sanitizeDecimalInput("1.5.3"))
        assertEquals("123", sanitizeIntInput("1a2 3"))
    }

    @Test
    fun `時刻の形式が不正なら null`() {
        assertNull(combineDateAndTime(0L, "25:00"))
        assertNull(combineDateAndTime(0L, "12:60"))
        assertNull(combineDateAndTime(0L, "abc"))
        assertNotNull(combineDateAndTime(0L, "07:05"))
    }

    @Test
    fun `日付ピッカーの値はどのタイムゾーンでも同じ日付になる`() {
        val original = TimeZone.getDefault()
        try {
            for (zone in listOf("Asia/Tokyo", "America/Los_Angeles", "Pacific/Auckland")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val local = java.time.LocalDateTime.of(2026, 11, 17, 21, 30)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                // ピッカーは UTC の 0 時で値を返す
                val picked = localMillisToPickerMillis(local)
                val back = pickerMillisToLocalMillis(picked, local)
                assertEquals("$zone: 日付も時刻も変わらない", local, back)
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `金額と距離の表示`() {
        assertEquals("¥7,946", formatYen(7946))
        assertEquals("1,234.5", formatNumber(1234.5))
        assertEquals("20", formatDouble(20.0))
    }
}
