package com.takumi.drivecare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEfficiencyTest {

    private var nextId = 1

    private fun record(
        odometer: Double,
        liters: Double,
        isFullTank: Boolean = true,
        timestamp: Long,
        vehicleId: Int = 1
    ) = FuelRecord(
        id = nextId++,
        vehicleId = vehicleId,
        odometer = odometer,
        liters = liters,
        price = 0,
        unitPrice = 0.0,
        isFullTank = isFullTank,
        timestamp = timestamp
    )

    @Test
    fun fullTankToFullTank() {
        val records = listOf(
            record(99620.0, 30.0, timestamp = 1),
            record(100155.0, 29.0, timestamp = 2)
        )
        val entries = calculateFuelEfficiencyEntries(records)
        assertEquals(1, entries.size)
        assertEquals(535.0 / 29.0, entries[0].kmPerLiter, 1e-9)
    }

    @Test
    fun partialFillLitersAreIncluded() {
        // 満タン → 10L継ぎ足し → 満タン20L、合計600km → 600 / 30 = 20 km/L
        val records = listOf(
            record(1000.0, 30.0, timestamp = 1),
            record(1300.0, 10.0, isFullTank = false, timestamp = 2),
            record(1600.0, 20.0, timestamp = 3)
        )
        val entries = calculateFuelEfficiencyEntries(records)
        assertEquals(1, entries.size)
        assertEquals(600.0, entries[0].distance, 1e-9)
        assertEquals(30.0, entries[0].liters, 1e-9)
        assertEquals(20.0, entries[0].kmPerLiter, 1e-9)
    }

    @Test
    fun partialFillBeforeFirstFullTankIsIgnored() {
        val records = listOf(
            record(900.0, 15.0, isFullTank = false, timestamp = 1),
            record(1000.0, 5.0, timestamp = 2),
            record(1400.0, 20.0, timestamp = 3)
        )
        val entries = calculateFuelEfficiencyEntries(records)
        assertEquals(1, entries.size)
        assertEquals(20.0, entries[0].kmPerLiter, 1e-9)
    }

    @Test
    fun decreasingOdometerIsSkipped() {
        val records = listOf(
            record(99620.0, 30.0, timestamp = 1),
            record(100155.0, 29.0, timestamp = 2),
            record(10747.0, 35.0, timestamp = 3) // 0 が1つ抜けた入力ミス
        )
        val entries = calculateFuelEfficiencyEntries(records)
        assertEquals(1, entries.size)
        assertEquals(records[1].id, entries[0].record.id)
    }

    @Test
    fun efficiencyTextForEachRecord() {
        val records = listOf(
            record(1000.0, 30.0, timestamp = 1),
            record(1300.0, 10.0, isFullTank = false, timestamp = 2),
            record(1600.0, 20.0, timestamp = 3),
            record(1500.0, 20.0, timestamp = 4)
        )
        assertEquals("前回満タン記録なし", calculateFuelEfficiencyTextForVehicle(records, records[0]))
        assertEquals("計算対象外", calculateFuelEfficiencyTextForVehicle(records, records[1]))
        assertEquals("20.00 km/L", calculateFuelEfficiencyTextForVehicle(records, records[2]))
        assertEquals("計算不可", calculateFuelEfficiencyTextForVehicle(records, records[3]))
    }

    @Test
    fun seriesAndAverageUseSameCalculation() {
        val records = listOf(
            record(1000.0, 30.0, timestamp = 1),
            record(1300.0, 10.0, isFullTank = false, timestamp = 2),
            record(1600.0, 20.0, timestamp = 3),
            record(2000.0, 25.0, timestamp = 4)
        )
        assertEquals(listOf(3L to 20.0, 4L to 16.0), buildEfficiencySeriesRaw(records))
        assertEquals("18.00 km/L", calculateAverageFuelEfficiencyForVehicle(records))
    }

    @Test
    fun multiVehicleIsCalculatedPerVehicle() {
        val records = listOf(
            record(1000.0, 30.0, timestamp = 1, vehicleId = 1),
            record(50000.0, 40.0, timestamp = 2, vehicleId = 2),
            record(1500.0, 25.0, timestamp = 3, vehicleId = 1),
            record(50400.0, 40.0, timestamp = 4, vehicleId = 2)
        )
        assertEquals(listOf(3L to 20.0, 4L to 10.0), buildEfficiencySeriesMultiVehicleRaw(records))
    }

    @Test
    fun odometerCheck() {
        val records = listOf(
            record(99620.0, 30.0, timestamp = 10),
            record(100155.0, 29.0, timestamp = 20)
        )
        // 正常
        assertNull(checkOdometerInput(records, 30, 100747.0, 40.0))
        // 前回より小さい（桁抜け）
        assertNotNull(checkOdometerInput(records, 30, 10747.0, 40.0))
        // 1タンクで走れない距離（桁の打ちすぎ）
        assertNotNull(checkOdometerInput(records, 30, 1007470.0, 40.0))
        // 過去の日付に入れたとき、後の記録より大きい
        assertNotNull(checkOdometerInput(records, 15, 100200.0, 40.0))
        // 編集中の自分自身とは比較しない
        val edited = records[1]
        assertNull(checkOdometerInput(records, 20, 100100.0, 40.0, excludeId = edited.id))
        assertTrue(checkOdometerInput(records, 20, 99000.0, 40.0, excludeId = edited.id)!!.contains("小さく"))
    }

    @Test
    fun litersFormatting() {
        assertEquals("31.71", formatLiters(31.71))
        assertEquals("35", formatLiters(35.0))
        assertEquals("30.8", formatLiters(30.8))
        assertEquals("31.71", formatForEdit(31.71))
        assertEquals("100155", formatForEdit(100155.0))
    }
}
