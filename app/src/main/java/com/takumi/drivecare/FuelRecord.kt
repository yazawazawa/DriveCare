package com.takumi.drivecare

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val odometer: Double,
    val liters: Double,
    val price: Int,
    val unitPrice: Double,
    val isFullTank: Boolean,
    val timestamp: Long
)