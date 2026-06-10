package ru.lesnoy40rt77.weatheradvisor.data

import ru.lesnoy40rt77.weatheradvisor.data.local.CityDao
import ru.lesnoy40rt77.weatheradvisor.data.local.CityEntity
import ru.lesnoy40rt77.weatheradvisor.model.City

class CityRepository(
    private val cityDao: CityDao
) {
    suspend fun searchCities(query: String): List<City> {
        val normalizedQuery = normalize(query)

        if (normalizedQuery.length < 2) {
            return emptyList()
        }

        return cityDao.searchCities(
            prefixQuery = "$normalizedQuery%",
            wordQuery = "% $normalizedQuery%"
        ).map { it.toCity() }
    }

    private fun createCity(
        geonameId: Int,
        name: String,
        countryName: String,
        countryCode: String,
        adminName: String?,
        latitude: Double,
        longitude: Double,
        population: Int
    ): CityEntity {
        return CityEntity(
            geonameId = geonameId,
            name = name,
            countryName = countryName,
            countryCode = countryCode,
            adminName = adminName,
            latitude = latitude,
            longitude = longitude,
            population = population,
            searchName = normalize(name)
        )
    }

    private fun CityEntity.toCity(): City {
        return City(
            name = name,
            countryName = countryName,
            countryCode = countryCode,
            adminName = adminName,
            latitude = latitude,
            longitude = longitude,
            population = population
        )
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace("ё", "е")
            .replace("-", " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}