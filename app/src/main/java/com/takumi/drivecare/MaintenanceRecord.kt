package com.takumi.drivecare

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_records")
data class MaintenanceRecord(
    @PrimaryKey val vehicleId: Int,
    val carWashDate: String? = null,
    val engineOilChangeDate: String? = null,
    val oilElementChangeDate: String? = null,
    val wiperChangeDate: String? = null,
    val tireChangeDate: String? = null,
    val airCleanerDate: String? = null,
    val airCleanerService: String? = null
)
