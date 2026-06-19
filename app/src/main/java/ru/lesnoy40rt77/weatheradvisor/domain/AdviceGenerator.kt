package ru.lesnoy40rt77.weatheradvisor.domain

import ru.lesnoy40rt77.weatheradvisor.model.ComfortLevel
import ru.lesnoy40rt77.weatheradvisor.model.WeatherAssessment
import ru.lesnoy40rt77.weatheradvisor.model.WeatherInfo
import kotlin.math.roundToInt

object AdviceGenerator {

    fun generate(weather: WeatherInfo): List<String> {
        return assess(weather).importantAdvices
    }

    fun assess(weather: WeatherInfo): WeatherAssessment {
        val advices = mutableListOf<String>()
        val outfitTips = mutableListOf<String>()
        var score = 100

        val temperature = weather.temperature
        val feelsLike = weather.feelsLike
        val windSpeed = weather.windSpeed
        val hasRain = weather.rainVolume > 0.0
        val hasSnow = weather.snowVolume > 0.0

        when {
            temperature <= -20 -> {
                score -= 50
                advices.add("Сильный мороз: без необходимости лучше долго не гулять.")
                outfitTips.add("Теплая зимняя куртка, шапка, шарф и перчатки обязательны.")
            }
            temperature <= -10 -> {
                score -= 35
                advices.add("Очень холодно: лучше одеться теплее обычного.")
                outfitTips.add("Зимняя куртка, шапка, шарф и перчатки.")
            }
            temperature <= 0 -> {
                score -= 22
                advices.add("Надень шапку: температура около нуля или ниже.")
                outfitTips.add("Теплая куртка, шапка и закрытая обувь.")
            }
            temperature <= 8 -> {
                score -= 12
                outfitTips.add("Куртка или плотная толстовка будут к месту.")
            }
            temperature <= 15 -> {
                score -= 5
                outfitTips.add("Легкая куртка, ветровка или худи.")
            }
            temperature <= 25 -> {
                outfitTips.add("Обычная легкая одежда подойдет.")
            }
            temperature <= 30 -> {
                score -= 8
                advices.add("Тепло: возьми воду, особенно если планируешь долго гулять.")
                outfitTips.add("Легкая одежда, кепка или очки по желанию.")
            }
            else -> {
                score -= 25
                advices.add("Жара: лучше избегать долгих прогулок под солнцем.")
                outfitTips.add("Максимально легкая одежда, головной убор и вода.")
            }
        }

        if (feelsLike <= temperature - 5) {
            score -= 8
            advices.add("По ощущениям холоднее, чем показывает термометр: оденься теплее.")
        }

        if (hasRain) {
            score -= 20
            advices.add("Возьми зонт: ожидается дождь.")
            outfitTips.add("Зонт или непромокаемая куртка.")
        }

        if (hasSnow) {
            score -= 18
            advices.add("На улице снег: лучше надеть теплую нескользящую обувь.")
            outfitTips.add("Теплая обувь с нормальной подошвой.")
        }

        when {
            windSpeed >= 18 -> {
                score -= 35
                advices.add("Опасный ветер ${windSpeed.roundToInt()} м/с: будь осторожен на улице.")
                outfitTips.add("Одежда без свободных деталей, которые будет сильно продувать.")
            }
            windSpeed >= 10 -> {
                score -= 18
                advices.add("Сильный ветер ${windSpeed.roundToInt()} м/с: лучше застегнуть куртку.")
                outfitTips.add("Ветровка или непродуваемая куртка.")
            }
            windSpeed >= 6 -> {
                score -= 6
                outfitTips.add("Может быть ветрено, лучше взять верхний слой.")
            }
        }

        if (advices.isEmpty()) {
            advices.add("Критичных погодных условий нет, можно выходить.")
        }

        val finalScore = score.coerceIn(0, 100)

        val comfortLevel = when {
            finalScore >= 85 -> ComfortLevel.GREAT
            finalScore >= 70 -> ComfortLevel.GOOD
            finalScore >= 50 -> ComfortLevel.NORMAL
            finalScore >= 30 -> ComfortLevel.BAD
            else -> ComfortLevel.DANGEROUS
        }

        val title = when (comfortLevel) {
            ComfortLevel.GREAT -> "Отличная погода"
            ComfortLevel.GOOD -> "Хорошая погода"
            ComfortLevel.NORMAL -> "Нормальная погода"
            ComfortLevel.BAD -> "Не самая приятная погода"
            ComfortLevel.DANGEROUS -> "Погода требует осторожности"
        }

        val summary = when (comfortLevel) {
            ComfortLevel.GREAT -> "Можно спокойно планировать прогулку."
            ComfortLevel.GOOD -> "В целом комфортно, но проверь советы ниже."
            ComfortLevel.NORMAL -> "Выходить можно, но лучше подготовиться."
            ComfortLevel.BAD -> "Для долгой прогулки условия не лучшие."
            ComfortLevel.DANGEROUS -> "Лучше сократить время на улице и одеться внимательно."
        }

        return WeatherAssessment(
            score = finalScore,
            title = title,
            summary = summary,
            comfortLevel = comfortLevel,
            importantAdvices = advices.distinct(),
            outfitTips = outfitTips.distinct().ifEmpty {
                listOf("Одевайся по сезону, дополнительных вещей не требуется.")
            }
        )
    }
}