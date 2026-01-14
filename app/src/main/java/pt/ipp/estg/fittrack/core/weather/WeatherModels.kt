package pt.ipp.estg.fittrack.core.weather

import com.squareup.moshi.Json

data class OpenMeteoResponse(
    @Json(name = "current_weather")
    val currentWeather: CurrentWeather? = null
)

data class CurrentWeather(
    val temperature: Double? = null,
    val windspeed: Double? = null,
    val weathercode: Int? = null,
    val time: String? = null
)

data class WeatherSnapshot(
    val tempC: Double?,
    val windKmh: Double?,
    val code: Int?,
    val isoTime: String?
)
