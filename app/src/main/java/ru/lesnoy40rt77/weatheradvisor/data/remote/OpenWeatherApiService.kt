package ru.lesnoy40rt77.weatheradvisor.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApiService {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): OpenWeatherResponse

    companion object {
        fun create(
            baseUrl: String = "https://api.openweathermap.org/"
        ): OpenWeatherApiService {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenWeatherApiService::class.java)
        }
    }
}

data class OpenWeatherResponse(
    val name: String,
    val weather: List<WeatherDescriptionDto>,
    val main: MainWeatherDto,
    val wind: WindDto,
    val rain: PrecipitationDto?,
    val snow: PrecipitationDto?
)

data class WeatherDescriptionDto(
    val main: String,
    val description: String
)

data class MainWeatherDto(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double
)

data class WindDto(
    val speed: Double
)

data class PrecipitationDto(
    @SerializedName("1h")
    val oneHour: Double? = null,

    @SerializedName("3h")
    val threeHours: Double? = null
)