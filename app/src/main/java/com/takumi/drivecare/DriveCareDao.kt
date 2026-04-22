package com.takumi.drivecare

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DriveCareDao {

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getAllVehicles(): List<Vehicle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM fuel_records ORDER BY timestamp DESC")
    suspend fun getAllFuelRecords(): List<FuelRecord>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    suspend fun getFuelRecordsByVehicle(vehicleId: Int): List<FuelRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecord(record: FuelRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecords(records: List<FuelRecord>)

    @Update
    suspend fun updateFuelRecord(record: FuelRecord)

    @Delete
    suspend fun deleteFuelRecord(record: FuelRecord)

    @Query("DELETE FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun deleteFuelRecordsByVehicleId(vehicleId: Int)

    @Query("DELETE FROM fuel_records")
    suspend fun deleteAllFuelRecords()

    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun countVehicles(): Int

    @Query("SELECT COUNT(*) FROM fuel_records")
    suspend fun countFuelRecords(): Int
}