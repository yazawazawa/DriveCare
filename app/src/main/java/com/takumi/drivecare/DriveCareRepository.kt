package com.takumi.drivecare

class DriveCareRepository(
    private val dao: DriveCareDao
) {
    suspend fun getAllVehicles(): List<Vehicle> = dao.getAllVehicles()
    suspend fun getAllFuelRecords(): List<FuelRecord> = dao.getAllFuelRecords()
    suspend fun getAllMaintenanceRecords(): List<MaintenanceRecord> = dao.getAllMaintenanceRecords()
    suspend fun getFuelRecordsByVehicle(vehicleId: Int): List<FuelRecord> =
        dao.getFuelRecordsByVehicle(vehicleId)

    suspend fun getMaintenanceRecordsByVehicle(vehicleId: Int): List<MaintenanceRecord> =
        dao.getMaintenanceRecordsByVehicle(vehicleId)


    suspend fun insertVehicle(vehicle: Vehicle): Long = dao.insertVehicle(vehicle)
    suspend fun insertFuelRecord(record: FuelRecord): Long = dao.insertFuelRecord(record)

    suspend fun updateVehicle(vehicle: Vehicle) = dao.updateVehicle(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.deleteFuelRecordsByVehicleId(vehicle.id)
        dao.deleteMaintenanceRecordByVehicleId(vehicle.id)
        dao.deleteVehicle(vehicle)
    }

    suspend fun updateFuelRecord(record: FuelRecord) = dao.updateFuelRecord(record)
    suspend fun deleteFuelRecord(record: FuelRecord) = dao.deleteFuelRecord(record)
    suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long = dao.insertMaintenanceRecord(record)


    suspend fun countVehicles(): Int = dao.countVehicles()
    suspend fun countFuelRecords(): Int = dao.countFuelRecords()

    suspend fun replaceAllData(
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ) {
        dao.deleteAllFuelRecords()
        dao.deleteAllMaintenanceRecords()
        dao.deleteAllVehicles()
        dao.insertVehicles(vehicles)
        dao.insertFuelRecords(fuelRecords)
        dao.insertMaintenanceRecords(maintenanceRecords)

    }
}
