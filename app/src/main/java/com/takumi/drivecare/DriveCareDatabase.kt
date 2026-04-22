package com.takumi.drivecare

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter

@ProvidedTypeConverter
class FuelTypeConverter {
    @TypeConverter
    fun fromFuelType(value: FuelType): String = value.name

    @TypeConverter
    fun toFuelType(value: String): FuelType = FuelType.valueOf(value)
}

@Database(
    entities = [Vehicle::class, FuelRecord::class],
    version = 1,
    exportSchema = false
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
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}