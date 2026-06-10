package ru.lesnoy40rt77.weatheradvisor.model

data class WeatherInfo(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val windSpeed: Double,
    val rainVolume: Double,
    val snowVolume: Double
)