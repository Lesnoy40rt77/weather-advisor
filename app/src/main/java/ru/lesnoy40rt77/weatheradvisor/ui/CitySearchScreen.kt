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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.lesnoy40rt77.weatheradvisor.data.CityRepository
import ru.lesnoy40rt77.weatheradvisor.model.City

@Composable
fun CitySearchScreen() {
    val cityRepository = remember { CityRepository() }

    var query by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<City?>(null) }

    val cities = remember(query) {
        cityRepository.searchCities(query)
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn {
            items(cities) { city ->
                CityItem(
                    city = city,
                    onClick = {
                        selectedCity = city
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