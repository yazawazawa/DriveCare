package com.takumi.drivecare

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val maker: String,
    val model: String,
    val tankCapacity: Double,
    val fuelType: FuelType
) {
    val displayName: String
        get() = "$maker $model"
}