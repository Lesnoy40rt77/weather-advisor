package ru.lesnoy40rt77.weatheradvisor.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.lesnoy40rt77.weatheradvisor.data.remote.OpenWeatherApiService

class WeatherRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getWeatherByCoordinatesMapsOpenWeatherResponse() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "name": "Saint Petersburg",
                      "weather": [
                        {
                          "main": "Rain",
                          "description": "небольшой дождь"
                        }
                      ],
                      "main": {
                        "temp": 12.4,
                        "feels_like": 10.1
                      },
                      "wind": {
                        "speed": 5.7
                      },
                      "rain": {
                        "1h": 0.8
                      }
                    }
                    """.trimIndent()
                )
        )

        val api = OpenWeatherApiService.create(server.url("/").toString())
        val repository = WeatherRepository(
            apiService = api,
            apiKey = "test_api_key"
        )

        val weather = repository.getWeatherByCoordinates(
            latitude = 59.9386,
            longitude = 30.3141,
            displayCityName = "Санкт-Петербург"
        )

        assertEquals("Санкт-Петербург", weather.cityName)
        assertEquals(12.4, weather.temperature, 0.001)
        assertEquals(10.1, weather.feelsLike, 0.001)
        assertEquals("небольшой дождь", weather.description)
        assertEquals(5.7, weather.windSpeed, 0.001)
        assertEquals(0.8, weather.rainVolume, 0.001)
        assertEquals(0.0, weather.snowVolume, 0.001)

        val request = server.takeRequest()
        assertTrue(request.path.orEmpty().contains("lat=59.9386"))
        assertTrue(request.path.orEmpty().contains("lon=30.3141"))
        assertTrue(request.path.orEmpty().contains("appid=test_api_key"))
        assertTrue(request.path.orEmpty().contains("units=metric"))
        assertTrue(request.path.orEmpty().contains("lang=ru"))
    }

    @Test
    fun getWeatherByCoordinatesFailsWhenApiKeyIsBlank() = runTest {
        val api = OpenWeatherApiService.create(server.url("/").toString())
        val repository = WeatherRepository(
            apiService = api,
            apiKey = ""
        )

        var failed = false

        try {
            repository.getWeatherByCoordinates(
                latitude = 59.9386,
                longitude = 30.3141
            )
        } catch (exception: IllegalStateException) {
            failed = true
        }

        assertTrue("Пустой API-ключ должен приводить к ошибке", failed)
    }
}
