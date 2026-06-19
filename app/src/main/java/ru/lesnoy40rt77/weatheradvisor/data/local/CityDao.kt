package ru.lesnoy40rt77.weatheradvisor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CityDao {

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<CityEntity>)

    @Query(
        """
        SELECT * FROM cities
        WHERE searchName LIKE :prefixQuery
        ORDER BY
            CASE WHEN countryCode = 'RU' THEN 0 ELSE 1 END,
            population DESC
        LIMIT 20
        """
    )
    suspend fun searchCities(
        prefixQuery: String
    ): List<CityEntity>
}