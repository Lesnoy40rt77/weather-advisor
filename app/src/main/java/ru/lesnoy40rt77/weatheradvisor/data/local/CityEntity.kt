package ru.lesnoy40rt77.weatheradvisor.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cities",
    indices = [
        Index(value = ["searchName"], name = "index_cities_searchName"),
        Index(value = ["countryCode"], name = "index_cities_countryCode"),
        Index(value = ["population"], name = "index_cities_population")
    ]
)
data class CityEntity(
    @PrimaryKey val geonameId: Int,
    val name: String,
    val countryName: String,
    val countryCode: String,
    val adminName: String?,
    val latitude: Double,
    val longitude: Double,
    val population: Int,
    val searchName: String
)