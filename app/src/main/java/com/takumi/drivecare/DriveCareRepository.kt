package com.takumi.drivecare

/** 端末内（Room）とクラウド（Firestore）で共通のデータ操作。 */
interface DriveCareDataSource {
    suspend fun getAllVehicles(): List<Vehicle>
    suspend fun getAllFuelRecords(): List<FuelRecord>
    suspend fun getAllMaintenanceRecords(): List<MaintenanceRecord>

    suspend fun insertVehicle(vehicle: Vehicle): Long
    suspend fun updateVehicle(vehicle: Vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle)

    suspend fun insertFuelRecord(record: FuelRecord): Long
    suspend fun updateFuelRecord(record: FuelRecord)
    suspend fun deleteFuelRecord(record: FuelRecord)

    suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long
    suspend fun updateMaintenanceRecord(record: MaintenanceRecord)
    suspend fun deleteMaintenanceRecord(record: MaintenanceRecord)

    suspend fun replaceAllData(
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    )
}

/** 端末内の Room データベースを使う実装。 */
class LocalDataSource(
    private val dao: DriveCareDao
) : DriveCareDataSource {
    override suspend fun getAllVehicles(): List<Vehicle> = dao.getAllVehicles()
    override suspend fun getAllFuelRecords(): List<FuelRecord> = dao.getAllFuelRecords()
    override suspend fun getAllMaintenanceRecords(): List<MaintenanceRecord> = dao.getAllMaintenanceRecords()

    override suspend fun insertVehicle(vehicle: Vehicle): Long = dao.insertVehicle(vehicle)
    override suspend fun updateVehicle(vehicle: Vehicle) = dao.updateVehicle(vehicle)
    override suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.deleteFuelRecordsByVehicleId(vehicle.id)
        dao.deleteMaintenanceRecordByVehicleId(vehicle.id)
        dao.deleteVehicle(vehicle)
    }

    override suspend fun insertFuelRecord(record: FuelRecord): Long = dao.insertFuelRecord(record)
    override suspend fun updateFuelRecord(record: FuelRecord) = dao.updateFuelRecord(record)
    override suspend fun deleteFuelRecord(record: FuelRecord) = dao.deleteFuelRecord(record)

    override suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long = dao.insertMaintenanceRecord(record)
    override suspend fun updateMaintenanceRecord(record: MaintenanceRecord) = dao.updateMaintenanceRecord(record)
    override suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) = dao.deleteMaintenanceRecord(record)

    override suspend fun replaceAllData(
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

/**
 * アプリが使うデータ窓口。クラウド共有中は [cloud] へ、それ以外は端末内 [local] へ処理を振り分ける。
 */
class DriveCareRepository(
    dao: DriveCareDao
) : DriveCareDataSource {
    val local = LocalDataSource(dao)

    @Volatile
    var cloud: DriveCareDataSource? = null
        private set

    val isCloudActive: Boolean get() = cloud != null

    fun attachCloud(source: DriveCareDataSource) { cloud = source }
    fun detachCloud() { cloud = null }

    private val active: DriveCareDataSource get() = cloud ?: local

    override suspend fun getAllVehicles() = active.getAllVehicles()
    override suspend fun getAllFuelRecords() = active.getAllFuelRecords()
    override suspend fun getAllMaintenanceRecords() = active.getAllMaintenanceRecords()

    override suspend fun insertVehicle(vehicle: Vehicle) = active.insertVehicle(vehicle)
    override suspend fun updateVehicle(vehicle: Vehicle) = active.updateVehicle(vehicle)
    override suspend fun deleteVehicle(vehicle: Vehicle) = active.deleteVehicle(vehicle)

    override suspend fun insertFuelRecord(record: FuelRecord) = active.insertFuelRecord(record)
    override suspend fun updateFuelRecord(record: FuelRecord) = active.updateFuelRecord(record)
    override suspend fun deleteFuelRecord(record: FuelRecord) = active.deleteFuelRecord(record)

    override suspend fun insertMaintenanceRecord(record: MaintenanceRecord) = active.insertMaintenanceRecord(record)
    override suspend fun updateMaintenanceRecord(record: MaintenanceRecord) = active.updateMaintenanceRecord(record)
    override suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) = active.deleteMaintenanceRecord(record)

    override suspend fun replaceAllData(
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ) = active.replaceAllData(vehicles, fuelRecords, maintenanceRecords)
}
