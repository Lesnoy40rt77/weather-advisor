package ru.lesnoy40rt77.weatheradvisor.model

data class City(
    val name: String,
    val countryName: String,
    val countryCode: String,
    val adminName: String?,
    val latitude: Double,
    val longitude: Double,
    val population: Int
) {
    val displayName: String
        get() = if (adminName.isNullOrBlank()) {
            "$name, $countryName"
        } else {
            "$name, $adminName, $countryName"
        }
}