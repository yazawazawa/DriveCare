package com.takumi.drivecare

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_records")
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val timestamp: Long,
    val odometer: Double,
    val carWashDone: Boolean,
    val engineOilDone: Boolean,
    val oilElementDone: Boolean,
    val wiperDone: Boolean,
    val tireDone: Boolean,
    val airCleanerCleaningDone: Boolean,
    val airCleanerReplacementDone: Boolean

)
