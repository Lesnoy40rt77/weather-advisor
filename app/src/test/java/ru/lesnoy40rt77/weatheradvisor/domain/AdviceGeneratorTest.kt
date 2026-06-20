package ru.lesnoy40rt77.weatheradvisor.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.lesnoy40rt77.weatheradvisor.model.WeatherInfo

class AdviceGeneratorTest {

    @Test
    fun assessReturnsHighScoreForComfortableWeather() {
        val weather = WeatherInfo(
            cityName = "Санкт-Петербург",
            temperature = 21.0,
            feelsLike = 21.0,
            description = "ясно",
            windSpeed = 3.0,
            rainVolume = 0.0,
            snowVolume = 0.0
        )

        val assessment = AdviceGenerator.assess(weather)

        assertTrue("Комфортная погода должна получить высокий балл", assessment.score >= 80)
        assertTrue("Должна быть хотя бы одна рекомендация по одежде", assessment.outfitTips.isNotEmpty())
        assertTrue("Должен быть хотя бы один совет", assessment.importantAdvices.isNotEmpty())
    }

    @Test
    fun assessPenalizesBadWeather() {
        val weather = WeatherInfo(
            cityName = "Санкт-Петербург",
            temperature = -18.0,
            feelsLike = -25.0,
            description = "снег",
            windSpeed = 12.0,
            rainVolume = 0.0,
            snowVolume = 2.5
        )

        val assessment = AdviceGenerator.assess(weather)

        assertTrue("Плохая погода должна получить сниженный балл", assessment.score < 60)
        assertTrue(
            "В советах должны быть предупреждения о холоде, снеге или ветре",
            assessment.importantAdvices.any { advice ->
                advice.contains("холод", ignoreCase = true) ||
                    advice.contains("снег", ignoreCase = true) ||
                    advice.contains("ветер", ignoreCase = true)
            }
        )
    }
}
