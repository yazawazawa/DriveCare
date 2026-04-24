package com.takumi.drivecare

import kotlinx.serialization.Serializable

@Serializable
data class BackupVehicle(
    val id: Int,
    val maker: String,
    val model: String,
    val tankCapacity: Double,
    val fuelType: String
)

@Serializable
data class BackupFuelRecord(
    val id: Int,
    val vehicleId: Int,
    val odometer: Double,
    val liters: Double,
    val price: Int,
    val unitPrice: Double,
    val isFullTank: Boolean,
    val timestamp: Long
)

@Serializable
data class BackupMaintenanceRecord(

    val id: Int,
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

@Serializable
data class BackupData(
    val vehicles: List<BackupVehicle>,
    val fuelRecords: List<BackupFuelRecord>,
    val maintenanceRecords: List<BackupMaintenanceRecord> = emptyList()
)
