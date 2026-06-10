package ru.lesnoy40rt77.weatheradvisor.domain

import ru.lesnoy40rt77.weatheradvisor.model.WeatherInfo
import kotlin.math.roundToInt

object AdviceGenerator {

    fun generate(weather: WeatherInfo): List<String> {
        val advices = mutableListOf<String>()

        if (weather.rainVolume > 0.0) {
            advices.add("Возьми зонт: ожидается дождь.")
        }

        if (weather.snowVolume > 0.0) {
            advices.add("На улице снег: лучше надеть теплую обувь.")
        }

        if (weather.temperature <= 0) {
            advices.add("Надень шапку: температура ${weather.temperature.roundToInt()}°C.")
        }

        if (weather.temperature <= -10) {
            advices.add("Очень холодно: лучше одеться теплее обычного.")
        }

        if (weather.windSpeed >= 10) {
            advices.add("Будь осторожен: сильный ветер ${weather.windSpeed.roundToInt()} м/с.")
        }

        if (
            weather.temperature in 15.0..25.0 &&
            weather.rainVolume == 0.0 &&
            weather.snowVolume == 0.0 &&
            weather.windSpeed < 8.0
        ) {
            advices.add("Погода комфортная, можно спокойно идти гулять.")
        }

        if (advices.isEmpty()) {
            advices.add("Критичных погодных условий нет, можно выходить.")
        }

        return advices
    }
}