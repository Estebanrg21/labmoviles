package cr.ac.una.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cr.ac.una.roomdb.entity.Ubicacion


@Dao
interface UbicacionDao {
    @Insert
    fun insert(entity: Ubicacion)

    @Query("SELECT * FROM ubicacion")
    fun getAll(): List<Ubicacion?>?
}