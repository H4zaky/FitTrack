package pt.ipp.estg.fittrack.core.weather

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherSnapshot(
    val tempC: Double?,
    val windKmh: Double?,
    val code: Int?,
    val ts: Long = System.currentTimeMillis()
)

object WeatherClient {
    fun fetchCurrent(lat: Double, lon: Double): WeatherSnapshot {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            requestMethod = "GET"
        }
        return conn.inputStream.use { stream ->
            val text = stream.bufferedReader().readText()
            val root = JSONObject(text)
            val cur = root.optJSONObject("current_weather")
            WeatherSnapshot(
                tempC = cur?.optDouble("temperature")?.takeIf { !it.isNaN() },
                windKmh = cur?.optDouble("windspeed")?.takeIf { !it.isNaN() },
                code = cur?.optInt("weathercode")
            )
        }
    }
}
