package cr.ac.una.roomdb.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*


@Entity
data class Ubicacion(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val latitud: Double,
    val longitud: Double,
    val fecha: Date,
    val isInPoligon: Boolean,
    @ColumnInfo(defaultValue = false.toString()) val isPointPolygon: Boolean
)