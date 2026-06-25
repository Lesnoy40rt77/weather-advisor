package ru.lesnoy40rt77.weatheradvisor.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import ru.lesnoy40rt77.weatheradvisor.data.CityRepository
import ru.lesnoy40rt77.weatheradvisor.data.LocationRepository
import ru.lesnoy40rt77.weatheradvisor.data.WeatherRepository
import ru.lesnoy40rt77.weatheradvisor.data.local.AppDatabase
import ru.lesnoy40rt77.weatheradvisor.data.remote.OpenWeatherApiService
import ru.lesnoy40rt77.weatheradvisor.domain.AdviceGenerator
import ru.lesnoy40rt77.weatheradvisor.model.City
import ru.lesnoy40rt77.weatheradvisor.model.ComfortLevel
import ru.lesnoy40rt77.weatheradvisor.model.WeatherAssessment
import ru.lesnoy40rt77.weatheradvisor.model.WeatherInfo
import java.text.NumberFormat
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
    var isSearchingCities by remember { mutableStateOf(false) }

    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    var assessment by remember { mutableStateOf<WeatherAssessment?>(null) }
    var isLoadingWeather by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var locationRequestId by remember { mutableIntStateOf(0) }

    fun clearWeatherState() {
        weatherInfo = null
        assessment = null
        errorMessage = null
    }

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
            isSearchingCities = false
            return@LaunchedEffect
        }

        val trimmedQuery = query.trim()

        if (trimmedQuery.length < 2) {
            cities = emptyList()
            isSearchingCities = false
            return@LaunchedEffect
        }

        isSearchingCities = true
        try {
            delay(180)
            cities = cityRepository.searchCities(trimmedQuery)
        } finally {
            isSearchingCities = false
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroHeader()
            }

            item {
                SearchPanel(
                    query = query,
                    isSearchingCities = isSearchingCities,
                    isLoadingWeather = isLoadingWeather,
                    onQueryChange = { newValue ->
                        query = newValue
                        selectedCity = null
                        clearWeatherState()
                    },
                    onClearQuery = {
                        query = ""
                        selectedCity = null
                        cities = emptyList()
                        clearWeatherState()
                    },
                    onLocationClick = {
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
                    }
                )
            }

            selectedCity?.let { city ->
                item {
                    SelectedCityCard(
                        city = city,
                        onClear = {
                            selectedCity = null
                            query = ""
                            cities = emptyList()
                            clearWeatherState()
                        }
                    )
                }
            }

            if (isLoadingWeather) {
                item {
                    LoadingCard()
                }
            }

            errorMessage?.let { message ->
                item {
                    ErrorCard(message = message)
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
                }
            }

            if (cities.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Найденные города",
                        subtitle = "${cities.size} вариантов из локальной базы"
                    )
                }

                items(
                    items = cities,
                    key = { city -> "${city.name}_${city.countryCode}_${city.latitude}_${city.longitude}" }
                ) { city ->
                    CityItem(
                        city = city,
                        onClick = {
                            selectedCity = city
                            query = city.name
                            cities = emptyList()
                            errorMessage = null
                        }
                    )
                }
            } else if (query.trim().length >= 2 && !isSearchingCities && selectedCity == null) {
                item {
                    EmptySearchCard(query = query)
                }
            } else if (currentWeather == null && !isLoadingWeather && selectedCity == null && errorMessage == null) {
                item {
                    StartTipsCard()
                }
            }

        }
    }
}

@Composable
private fun HeroHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(100.dp)
        ) {
            Text(
                text = "🌦️ Weather Advisor",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Погодный советник",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Подскажет комфортность погоды, одежду и важные советы для прогулки.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    }
}

@Composable
private fun SearchPanel(
    query: String,
    isSearchingCities: Boolean,
    isLoadingWeather: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLocationClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Где смотрим погоду?",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Начни вводить город или включи определение по GPS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Введите город") },
                placeholder = { Text("Например: Санкт-Петербург") },
                leadingIcon = { Text("🔎") },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = onClearQuery) {
                            Text("✕")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )

            if (isSearchingCities) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Ищу города...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onLocationClick,
                enabled = !isLoadingWeather,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("📍 Погода по моему местоположению")
            }
        }
    }
}

@Composable
private fun SelectedCityCard(
    city: City,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", style = MaterialTheme.typography.titleLarge)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Выбран город",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                )
                Text(
                    text = city.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Сменить")
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Загружаем погоду",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Берём свежие данные OpenWeather и считаем комфортность.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ Что-то пошло не так",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CityItem(
    city: City,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = city.countryCode, style = MaterialTheme.typography.labelLarge)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = city.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "👥 ${formatPopulation(city.population)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📍 ${formatCoordinate(city.latitude)}, ${formatCoordinate(city.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherInfo,
    assessment: WeatherAssessment
) {
    val comfortColors = comfortColors(assessment.comfortLevel)
    val scoreProgress = assessment.score.coerceIn(0, 100) / 100f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            comfortColors.container.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weatherEmoji(weather),
                            style = MaterialTheme.typography.displaySmall
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weather.cityName,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = weather.description.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${weather.temperature.roundToInt()}°",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ScoreBlock(
                    score = assessment.score,
                    progress = scoreProgress,
                    title = assessment.title,
                    summary = assessment.summary,
                    comfortLevel = assessment.comfortLevel,
                    containerColor = comfortColors.container,
                    contentColor = comfortColors.content
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Ощущается",
                        value = "${weather.feelsLike.roundToInt()}°C",
                        emoji = "🌡️",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Ветер",
                        value = "${weather.windSpeed.roundToInt()} м/с",
                        emoji = "💨",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Дождь",
                        value = formatVolume(weather.rainVolume),
                        emoji = "☔",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Снег",
                        value = formatVolume(weather.snowVolume),
                        emoji = "❄️",
                        modifier = Modifier.weight(1f)
                    )
                }

                AdviceSection(
                    title = "Что надеть или взять",
                    emoji = "🧥",
                    items = assessment.outfitTips
                )

                AdviceSection(
                    title = "Важные советы",
                    emoji = "💡",
                    items = assessment.importantAdvices
                )
            }
        }
    }
}

@Composable
private fun ScoreBlock(
    score: Int,
    progress: Float,
    title: String,
    summary: String,
    comfortLevel: ComfortLevel,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = comfortLevel.readableName(),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = containerColor,
                    contentColor = contentColor
                ) {
                    Text(
                        text = "$score/100",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = contentColor,
                trackColor = containerColor.copy(alpha = 0.35f)
            )

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 92.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdviceSection(
    title: String,
    emoji: String,
    items: List<String>
) {
    if (items.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "$emoji $title",
                style = MaterialTheme.typography.titleMedium
            )

            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchCard(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ничего не найдено",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "По запросу «${query.trim()}» нет совпадений. Попробуй другое написание или ближайший крупный город.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StartTipsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Как начать",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "1. Введи минимум 2 буквы города.\n2. Выбери город из списка.\n3. Получи оценку погоды и советы.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ComfortColors(
    val container: Color,
    val content: Color
)

@Composable
private fun comfortColors(level: ComfortLevel): ComfortColors {
    return when (level) {
        ComfortLevel.GREAT -> ComfortColors(
            container = Color(0xFFDDF8DF),
            content = Color(0xFF146C2E)
        )

        ComfortLevel.GOOD -> ComfortColors(
            container = Color(0xFFE0F2FE),
            content = Color(0xFF075985)
        )

        ComfortLevel.NORMAL -> ComfortColors(
            container = Color(0xFFFEF3C7),
            content = Color(0xFF92400E)
        )

        ComfortLevel.BAD -> ComfortColors(
            container = Color(0xFFFFE4D6),
            content = Color(0xFFB45309)
        )

        ComfortLevel.DANGEROUS -> ComfortColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.error
        )
    }
}

private fun ComfortLevel.readableName(): String {
    return when (this) {
        ComfortLevel.GREAT -> "Отлично для прогулки"
        ComfortLevel.GOOD -> "Хорошие условия"
        ComfortLevel.NORMAL -> "Нормально, но есть нюансы"
        ComfortLevel.BAD -> "Лучше подготовиться"
        ComfortLevel.DANGEROUS -> "Небезопасные условия"
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
        "${String.format(java.util.Locale.US, "%.1f", value)} мм"
    }
}

private fun formatCoordinate(value: Double): String {
    return String.format(java.util.Locale.US, "%.2f", value)
}

private fun formatPopulation(value: Int): String {
    if (value <= 0) return "н/д"
    return NumberFormat.getIntegerInstance().format(value)
}