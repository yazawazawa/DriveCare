package com.takumi.drivecare

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@ProvidedTypeConverter
class FuelTypeConverter {
    @TypeConverter
    fun fromFuelType(value: FuelType): String = value.name

    @TypeConverter
    fun toFuelType(value: String): FuelType = FuelType.valueOf(value)
}

/**
 * Migration from version 2 → 3:
 * Added MaintenanceRecord table (introduced in v3).
 * If users had v2 installed, this creates the table without losing data.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `maintenance_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `odometer` REAL NOT NULL,
                `carWashDone` INTEGER NOT NULL,
                `engineOilDone` INTEGER NOT NULL,
                `oilElementDone` INTEGER NOT NULL,
                `wiperDone` INTEGER NOT NULL,
                `tireDone` INTEGER NOT NULL,
                `airCleanerCleaningDone` INTEGER NOT NULL,
                `airCleanerReplacementDone` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * v4: MaintenanceRecord に comment / customItemsDone を追加。
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE maintenance_records ADD COLUMN comment TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "ALTER TABLE maintenance_records ADD COLUMN customItemsDone TEXT NOT NULL DEFAULT ''"
        )
    }
}

@Database(
    entities = [Vehicle::class, FuelRecord::class, MaintenanceRecord::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(FuelTypeConverter::class)
abstract class DriveCareDatabase : RoomDatabase() {
    abstract fun driveCareDao(): DriveCareDao

    companion object {
        @Volatile
        private var INSTANCE: DriveCareDatabase? = null

        fun getDatabase(context: Context): DriveCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DriveCareDatabase::class.java,
                    "drivecare_db"
                )
                    .addTypeConverter(FuelTypeConverter())
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // fallbackToDestructiveMigration() は削除。
                    // 未知のバージョンからのダウングレード時のみ許容する場合:
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
