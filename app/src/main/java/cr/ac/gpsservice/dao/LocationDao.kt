package cr.ac.gpsservice.dao

import androidx.room.*
import cr.ac.gpsservice.entity.Location

@Dao
interface LocationDao {
    @Insert
    fun insert(location: Location)

    @Query("select * from location_table")
    fun query(): List<Location>
}