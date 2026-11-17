package com.takumi.drivecare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val data = BackupData(
        vehicles = listOf(BackupVehicle(1, "TOYOTA", "GRスープラ", 52.0, "HIGH_OCTANE")),
        fuelRecords = listOf(BackupFuelRecord(1, 1, 20805.0, 46.2, 7946, 172.0, true, 1793971148045)),
        maintenanceRecords = listOf(
            BackupMaintenanceRecord(1, 1, 1777017182019, 17230.0, true, false, false, false, false, true, true, "メモ", "")
        ),
        maintenanceItemDefs = mapOf("1" to MaintenanceItemDefaults.items)
    )

    private fun success(r: BackupReadResult) = (r as BackupReadResult.Success).data

    @Test
    fun `圧縮のみの往復`() {
        assertEquals(data, success(BackupCodec.decode(BackupCodec.encode(data, null), null)))
    }

    @Test
    fun `暗号化の往復`() {
        val bytes = BackupCodec.encode(data, "pass1234")
        assertEquals(data, success(BackupCodec.decode(bytes, "pass1234")))
    }

    @Test
    fun `暗号化の中身に平文が含まれない`() {
        val text = String(BackupCodec.encode(data, "pass1234"), Charsets.ISO_8859_1)
        assertFalse(text.contains("TOYOTA"))
    }

    @Test
    fun `パスワードが無い・違うと読めない`() {
        val bytes = BackupCodec.encode(data, "pass1234")
        assertEquals(BackupReadResult.NeedPassword, BackupCodec.decode(bytes, null))
        assertEquals(BackupReadResult.WrongPassword, BackupCodec.decode(bytes, "wrong"))
    }

    @Test
    fun `改ざんを検知する`() {
        val bytes = BackupCodec.encode(data, "pass1234")
        bytes[bytes.size - 5] = (bytes[bytes.size - 5].toInt() xor 1).toByte()
        assertEquals(BackupReadResult.WrongPassword, BackupCodec.decode(bytes, "pass1234"))
    }

    @Test
    fun `同じ内容でも暗号化のたびに結果が変わる`() {
        assertFalse(BackupCodec.encode(data, "pass1234").contentEquals(BackupCodec.encode(data, "pass1234")))
    }

    @Test
    fun `旧形式の JSON も読める`() {
        val legacy = """{"vehicles":[{"id":1,"maker":"A","model":"B","tankCapacity":40.0,"fuelType":"REGULAR"}],"fuelRecords":[]}"""
        val result = success(BackupCodec.decode(legacy.toByteArray(), null))
        assertEquals(1, result.vehicles.size)
        assertTrue(result.maintenanceRecords.isEmpty())
        assertTrue(result.maintenanceItemDefs.isEmpty())
    }

    @Test
    fun `壊れたデータや無関係なファイルは読み込み不可`() {
        assertEquals(BackupReadResult.Invalid, BackupCodec.decode(ByteArray(0), null))
        assertEquals(BackupReadResult.Invalid, BackupCodec.decode("hello".toByteArray(), null))
        assertEquals(BackupReadResult.Invalid, BackupCodec.decode("{broken".toByteArray(), null))
        val truncated = BackupCodec.encode(data, null).copyOf(12)
        assertEquals(BackupReadResult.Invalid, BackupCodec.decode(truncated, null))
    }
}
