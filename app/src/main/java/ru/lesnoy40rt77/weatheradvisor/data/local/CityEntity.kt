package ru.lesnoy40rt77.weatheradvisor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
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