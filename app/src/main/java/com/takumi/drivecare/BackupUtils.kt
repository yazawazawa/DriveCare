package com.takumi.drivecare

import android.content.Context
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object BackupUtils {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportToLocalFile(
        context: Context,
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ): File {
        val backup = BackupData(
            vehicles = vehicles.map {
                BackupVehicle(
                    id = it.id,
                    maker = it.maker,
                    model = it.model,
                    tankCapacity = it.tankCapacity,
                    fuelType = it.fuelType.name
                )
            },
            fuelRecords = fuelRecords.map {
                BackupFuelRecord(
                    id = it.id,
                    vehicleId = it.vehicleId,
                    odometer = it.odometer,
                    liters = it.liters,
                    price = it.price,
                    unitPrice = it.unitPrice,
                    isFullTank = it.isFullTank,
                    timestamp = it.timestamp
                )
            },
            maintenanceRecords = maintenanceRecords.map {
                BackupMaintenanceRecord(
                    id = it.id,
                    vehicleId = it.vehicleId,
                    timestamp = it.timestamp,
                    odometer = it.odometer,
                    carWashDone = it.carWashDone,
                    engineOilDone = it.engineOilDone,
                    oilElementDone = it.oilElementDone,
                    wiperDone = it.wiperDone,
                    tireDone = it.tireDone,
                    airCleanerCleaningDone = it.airCleanerCleaningDone,
                    airCleanerReplacementDone = it.airCleanerReplacementDone
                )
            },
        )

        val text = json.encodeToString(backup)
        val file = File(context.filesDir, "drivecare_backup.json")
        file.writeText(text)
        return file
    }

    fun importFromLocalFile(context: Context): BackupData? {
        val file = File(context.filesDir, "drivecare_backup.json")
        if (!file.exists()) return null
        return json.decodeFromString(file.readText())
    }
}
