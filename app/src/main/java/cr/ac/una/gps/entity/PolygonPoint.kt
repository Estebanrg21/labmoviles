package cr.ac.una.gps.entity

import com.google.android.gms.maps.model.Marker
import cr.ac.una.roomdb.entity.Ubicacion
import java.util.*

class PolygonPoint(var marker: Marker?, var ubicacion: Ubicacion){
    constructor(marker: Marker) : this(marker,
        Ubicacion(null, marker.position.latitude,marker.position.longitude,
            fecha = Date(), isInPoligon = true, isPointPolygon = true))
}