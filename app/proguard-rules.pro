# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# ── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.takumi.drivecare.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ── DriveCare data / backup classes ──────────────────────────────────────────
-keep class com.takumi.drivecare.BackupData { *; }
-keep class com.takumi.drivecare.BackupVehicle { *; }
-keep class com.takumi.drivecare.BackupFuelRecord { *; }
-keep class com.takumi.drivecare.BackupMaintenanceRecord { *; }

# ── Kotlin ───────────────────────────────────────────────────────────────────
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.Metadata { *; }

# ── Compose runtime ──────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── MaintenanceItemDef (Kotlin Serialization) ────────────────────────────────
-keep @kotlinx.serialization.Serializable class com.takumi.drivecare.MaintenanceItemDef { *; }
-keep class com.takumi.drivecare.IntervalTrigger { *; }
