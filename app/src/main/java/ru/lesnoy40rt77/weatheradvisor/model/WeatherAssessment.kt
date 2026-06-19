package ru.lesnoy40rt77.weatheradvisor.model

data class WeatherAssessment(
    val score: Int,
    val title: String,
    val summary: String,
    val comfortLevel: ComfortLevel,
    val importantAdvices: List<String>,
    val outfitTips: List<String>
)

enum class ComfortLevel {
    GREAT,
    GOOD,
    NORMAL,
    BAD,
    DANGEROUS
}