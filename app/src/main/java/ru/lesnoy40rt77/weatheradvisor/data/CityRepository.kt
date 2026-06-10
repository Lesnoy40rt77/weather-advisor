package ru.lesnoy40rt77.weatheradvisor.data

import ru.lesnoy40rt77.weatheradvisor.data.local.CityDao
import ru.lesnoy40rt77.weatheradvisor.data.local.CityEntity
import ru.lesnoy40rt77.weatheradvisor.model.City

class CityRepository(
    private val cityDao: CityDao
) {

    suspend fun seedIfEmpty() {
        if (cityDao.getCount() > 0) return

        cityDao.insertCities(
            listOf(
                createCity(
                    geonameId = 498817,
                    name = "Санкт-Петербург",
                    countryName = "Россия",
                    countryCode = "RU",
                    adminName = "Санкт-Петербург",
                    latitude = 59.9386,
                    longitude = 30.3141,
                    population = 5600000
                ),
                createCity(
                    geonameId = 524901,
                    name = "Москва",
                    countryName = "Россия",
                    countryCode = "RU",
                    adminName = "Москва",
                    latitude = 55.7558,
                    longitude = 37.6173,
                    population = 13000000
                ),
                createCity(
                    geonameId = 551487,
                    name = "Казань",
                    countryName = "Россия",
                    countryCode = "RU",
                    adminName = "Татарстан",
                    latitude = 55.7961,
                    longitude = 49.1064,
                    population = 1300000
                ),
                createCity(
                    geonameId = 491422,
                    name = "Сочи",
                    countryName = "Россия",
                    countryCode = "RU",
                    adminName = "Краснодарский край",
                    latitude = 43.5855,
                    longitude = 39.7231,
                    population = 460000
                ),
                createCity(
                    geonameId = 3448439,
                    name = "Сан-Паулу",
                    countryName = "Бразилия",
                    countryCode = "BR",
                    adminName = "Сан-Паулу",
                    latitude = -23.5505,
                    longitude = -46.6333,
                    population = 12300000
                ),
                createCity(
                    geonameId = 5391811,
                    name = "Сан-Диего",
                    countryName = "США",
                    countryCode = "US",
                    adminName = "Калифорния",
                    latitude = 32.7157,
                    longitude = -117.1611,
                    population = 1400000
                )
            )
        )
    }

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