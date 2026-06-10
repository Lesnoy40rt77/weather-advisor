package ru.lesnoy40rt77.weatheradvisor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.lesnoy40rt77.weatheradvisor.data.CityRepository
import ru.lesnoy40rt77.weatheradvisor.data.WeatherRepository
import ru.lesnoy40rt77.weatheradvisor.data.local.AppDatabase
import ru.lesnoy40rt77.weatheradvisor.data.remote.OpenWeatherApiService
import ru.lesnoy40rt77.weatheradvisor.domain.AdviceGenerator
import ru.lesnoy40rt77.weatheradvisor.model.City
import ru.lesnoy40rt77.weatheradvisor.model.WeatherInfo
import kotlin.math.roundToInt

@Composable
fun CitySearchScreen() {
    val context = LocalContext.current

    val cityRepository = remember {
        val database = AppDatabase.getDatabase(context)
        CityRepository(database.cityDao())
    }

    val weatherRepository = remember {
        WeatherRepository(OpenWeatherApiService.create())
    }

    var query by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }

    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    var advices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingWeather by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        cities = cityRepository.searchCities(query)
    }

    LaunchedEffect(selectedCity) {
        val city = selectedCity ?: return@LaunchedEffect

        isLoadingWeather = true
        errorMessage = null
        weatherInfo = null
        advices = emptyList()

        try {
            val loadedWeather = weatherRepository.getWeather(city)
            weatherInfo = loadedWeather
            advices = AdviceGenerator.generate(loadedWeather)
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Не удалось загрузить погоду"
        } finally {
            isLoadingWeather = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Погодный советник",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Введите город") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        selectedCity?.let { city ->
            Text(
                text = "Выбран город: ${city.displayName}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoadingWeather) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
        }

        errorMessage?.let { message ->
            Text(
                text = "Ошибка: $message",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        weatherInfo?.let { weather ->
            WeatherCard(
                weather = weather,
                advices = advices
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn {
            items(cities) { city ->
                CityItem(
                    city = city,
                    onClick = {
                        selectedCity = city
                        query = city.name
                    }
                )
            }
        }
    }
}

@Composable
private fun CityItem(
    city: City,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = city.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = city.displayName,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Координаты: ${city.latitude}, ${city.longitude}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherInfo,
    advices: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = weather.cityName,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${weather.temperature.roundToInt()}°C, ${weather.description}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Ощущается как: ${weather.feelsLike.roundToInt()}°C",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Ветер: ${weather.windSpeed.roundToInt()} м/с",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Дождь: ${weather.rainVolume} мм",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Снег: ${weather.snowVolume} мм",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Советы:",
                style = MaterialTheme.typography.titleMedium
            )

            advices.forEach { advice ->
                Text(
                    text = "— $advice",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}