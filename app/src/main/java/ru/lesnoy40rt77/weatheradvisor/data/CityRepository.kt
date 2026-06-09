package ru.lesnoy40rt77.weatheradvisor.data

import ru.lesnoy40rt77.weatheradvisor.model.City

class CityRepository {

    private val cities = listOf(
        City(
            name = "Санкт-Петербург",
            countryName = "Россия",
            countryCode = "RU",
            adminName = "Санкт-Петербург",
            latitude = 59.9386,
            longitude = 30.3141,
            population = 5600000
        ),
        City(
            name = "Москва",
            countryName = "Россия",
            countryCode = "RU",
            adminName = "Москва",
            latitude = 55.7558,
            longitude = 37.6173,
            population = 13000000
        ),
        City(
            name = "Казань",
            countryName = "Россия",
            countryCode = "RU",
            adminName = "Татарстан",
            latitude = 55.7961,
            longitude = 49.1064,
            population = 1300000
        ),
        City(
            name = "Сочи",
            countryName = "Россия",
            countryCode = "RU",
            adminName = "Краснодарский край",
            latitude = 43.5855,
            longitude = 39.7231,
            population = 460000
        ),
        City(
            name = "Сан-Паулу",
            countryName = "Бразилия",
            countryCode = "BR",
            adminName = "Сан-Паулу",
            latitude = -23.5505,
            longitude = -46.6333,
            population = 12300000
        ),
        City(
            name = "Сан-Диего",
            countryName = "США",
            countryCode = "US",
            adminName = "Калифорния",
            latitude = 32.7157,
            longitude = -117.1611,
            population = 1400000
        )
    )

    fun searchCities(query: String): List<City> {
        val normalizedQuery = normalize(query)

        if (normalizedQuery.length < 2) {
            return emptyList()
        }

        return cities
            .filter { city ->
                normalize(city.name).contains(normalizedQuery)
            }
            .sortedWith(
                compareBy<City> { if (it.countryCode == "RU") 0 else 1 }
                    .thenByDescending { it.population }
            )
            .take(30)
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