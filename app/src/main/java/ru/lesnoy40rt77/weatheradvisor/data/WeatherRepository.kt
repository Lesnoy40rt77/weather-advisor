package ru.lesnoy40rt77.weatheradvisor.data

import ru.lesnoy40rt77.weatheradvisor.BuildConfig
import ru.lesnoy40rt77.weatheradvisor.data.remote.OpenWeatherApiService
import ru.lesnoy40rt77.weatheradvisor.model.City
import ru.lesnoy40rt77.weatheradvisor.model.WeatherInfo

class WeatherRepository(
    private val apiService: OpenWeatherApiService
) {

    suspend fun getWeather(city: City): WeatherInfo {
        return getWeatherByCoordinates(
            latitude = city.latitude,
            longitude = city.longitude,
            displayCityName = city.name
        )
    }

    suspend fun getWeatherByCoordinates(
        latitude: Double,
        longitude: Double,
        displayCityName: String? = null
    ): WeatherInfo {
        val apiKey = BuildConfig.OPENWEATHER_API_KEY

        if (apiKey.isBlank()) {
            error("OpenWeather API key is empty. Add OPENWEATHER_API_KEY to local.properties")
        }

        val response = apiService.getCurrentWeather(
            latitude = latitude,
            longitude = longitude,
            apiKey = apiKey
        )

        val cityName = displayCityName
            ?: response.name.takeIf { it.isNotBlank() }
            ?: "Моё местоположение"

        return WeatherInfo(
            cityName = cityName,
            temperature = response.main.temp,
            feelsLike = response.main.feelsLike,
            description = response.weather.firstOrNull()?.description ?: "Нет описания",
            windSpeed = response.wind.speed,
            rainVolume = response.rain?.oneHour ?: response.rain?.threeHours ?: 0.0,
            snowVolume = response.snow?.oneHour ?: response.snow?.threeHours ?: 0.0
        )
    }
}