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
    val vehicleId: Int,
    val carWashDate: String? = null,
    val engineOilChangeDate: String? = null,
    val oilElementChangeDate: String? = null,
    val wiperChangeDate: String? = null,
    val tireChangeDate: String? = null,
    val airCleanerDate: String? = null,
    val airCleanerService: String? = null
)

@Serializable
data class BackupData(
    val vehicles: List<BackupVehicle>,
    val fuelRecords: List<BackupFuelRecord>,
    val maintenanceRecords: List<BackupMaintenanceRecord> = emptyList()
)
