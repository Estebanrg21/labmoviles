package cr.ac.una.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cr.ac.una.roomdb.entity.Ubicacion


@Dao
interface UbicacionDao {
    @Insert
    fun insert(entity: Ubicacion)

    @Update
    fun update(entity: Ubicacion)

    fun upsert(entity: Ubicacion) {
        if (entity.id == null) {
            insert(entity)
        } else {
            println(entity)
            update(entity)
        }
    }

    @Delete
    fun delete(entity: Ubicacion)

    @Query("DELETE FROM ubicacion WHERE isPointPolygon=1")
    fun deleteAllPolygonPoints()

    @Query("SELECT * FROM ubicacion WHERE isPointPolygon=1")
    fun getAllPolygonPoints(): List<Ubicacion?>?
    @Query("SELECT * FROM ubicacion")
    fun getAll(): List<Ubicacion?>?
}