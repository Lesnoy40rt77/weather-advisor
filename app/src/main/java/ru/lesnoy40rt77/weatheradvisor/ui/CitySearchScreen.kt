package ru.lesnoy40rt77.weatheradvisor.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import ru.lesnoy40rt77.weatheradvisor.data.CityRepository
import ru.lesnoy40rt77.weatheradvisor.data.LocationRepository
import ru.lesnoy40rt77.weatheradvisor.data.WeatherRepository
import ru.lesnoy40rt77.weatheradvisor.data.local.AppDatabase
import ru.lesnoy40rt77.weatheradvisor.data.remote.OpenWeatherApiService
import ru.lesnoy40rt77.weatheradvisor.domain.AdviceGenerator
import ru.lesnoy40rt77.weatheradvisor.model.City
import ru.lesnoy40rt77.weatheradvisor.model.WeatherAssessment
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

    val locationRepository = remember {
        LocationRepository(context)
    }

    var query by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }

    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    var assessment by remember { mutableStateOf<WeatherAssessment?>(null) }
    var isLoadingWeather by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var locationRequestId by remember { mutableIntStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            locationRequestId++
        } else {
            errorMessage = "Без разрешения на местоположение нельзя получить погоду рядом с тобой."
        }
    }

    LaunchedEffect(query, selectedCity) {
        if (selectedCity != null) {
            cities = emptyList()
            return@LaunchedEffect
        }

        val trimmedQuery = query.trim()

        if (trimmedQuery.length < 2) {
            cities = emptyList()
            return@LaunchedEffect
        }

        delay(250)

        cities = cityRepository.searchCities(trimmedQuery)
    }

    LaunchedEffect(selectedCity) {
        val city = selectedCity ?: return@LaunchedEffect

        isLoadingWeather = true
        errorMessage = null
        weatherInfo = null
        assessment = null

        try {
            val loadedWeather = weatherRepository.getWeather(city)
            weatherInfo = loadedWeather
            assessment = AdviceGenerator.assess(loadedWeather)
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Не удалось загрузить погоду"
        } finally {
            isLoadingWeather = false
        }
    }

    LaunchedEffect(locationRequestId) {
        if (locationRequestId == 0) return@LaunchedEffect

        isLoadingWeather = true
        errorMessage = null
        selectedCity = null
        query = ""
        cities = emptyList()
        weatherInfo = null
        assessment = null

        try {
            val location = locationRepository.getCurrentLocation()

            val loadedWeather = weatherRepository.getWeatherByCoordinates(
                latitude = location.latitude,
                longitude = location.longitude,
                displayCityName = "Моё местоположение"
            )

            weatherInfo = loadedWeather
            assessment = AdviceGenerator.assess(loadedWeather)
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Не удалось получить погоду по местоположению"
        } finally {
            isLoadingWeather = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "Погодный советник",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Выбери город из локальной базы или получи погоду по местоположению.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { newValue ->
                    query = newValue
                    selectedCity = null
                    errorMessage = null
                    weatherInfo = null
                    assessment = null
                },
                label = { Text("Введите город") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val fineGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    val coarseGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (fineGranted || coarseGranted) {
                        locationRequestId++
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Погода по моему местоположению")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        selectedCity?.let { city ->
            item {
                SelectedCityCard(
                    city = city,
                    onClear = {
                        selectedCity = null
                        query = ""
                        weatherInfo = null
                        assessment = null
                        errorMessage = null
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (isLoadingWeather) {
            item {
                LoadingCard()
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        errorMessage?.let { message ->
            item {
                ErrorCard(message = message)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        val currentWeather = weatherInfo
        val currentAssessment = assessment

        if (currentWeather != null && currentAssessment != null) {
            item {
                WeatherCard(
                    weather = currentWeather,
                    assessment = currentAssessment
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (cities.isNotEmpty()) {
            item {
                Text(
                    text = "Найденные города",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            items(cities) { city ->
                CityItem(
                    city = city,
                    onClick = {
                        selectedCity = city
                        query = city.name
                        cities = emptyList()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Данные о городах: GeoNames. Данные о погоде: OpenWeather.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SelectedCityCard(
    city: City,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Выбран город",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = city.displayName,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выбрать другой город")
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Загружаем актуальную погоду...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ошибка",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
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
    assessment: WeatherAssessment
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weatherEmoji(weather),
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = weather.cityName,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "${weather.temperature.roundToInt()}°C, ${weather.description}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Оценка: ${assessment.score}/100 — ${assessment.title}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = assessment.summary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Показатели",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ощущается как: ${weather.feelsLike.roundToInt()}°C",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Ветер: ${weather.windSpeed.roundToInt()} м/с",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Дождь: ${formatVolume(weather.rainVolume)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Снег: ${formatVolume(weather.snowVolume)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Что надеть или взять",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            assessment.outfitTips.forEach { tip ->
                Text(
                    text = "— $tip",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Советы",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            assessment.importantAdvices.forEach { advice ->
                Text(
                    text = "— $advice",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun weatherEmoji(weather: WeatherInfo): String {
    val description = weather.description.lowercase()

    return when {
        weather.snowVolume > 0.0 || "снег" in description -> "❄️"
        weather.rainVolume > 0.0 || "дожд" in description -> "☔"
        "гроза" in description -> "⛈️"
        "обла" in description || "пасмур" in description -> "☁️"
        "ясно" in description || "солн" in description -> "☀️"
        else -> "🌤️"
    }
}

private fun formatVolume(value: Double): String {
    return if (value <= 0.0) {
        "нет"
    } else {
        "${value} мм"
    }
}
