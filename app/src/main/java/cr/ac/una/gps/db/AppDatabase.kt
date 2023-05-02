package cr.ac.una.roomdb.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cr.ac.una.roomdb.UbicacionDao
import cr.ac.una.roomdb.converter.Converters
import cr.ac.una.roomdb.entity.Ubicacion

@Database(entities = [Ubicacion::class], version = 3)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ubicacionDao(): UbicacionDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                synchronized(AppDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ubicaciones-database"
                    ).fallbackToDestructiveMigration().build()
                }
            }
            return instance!!
        }
    }
}