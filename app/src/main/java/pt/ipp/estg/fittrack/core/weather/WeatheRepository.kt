package pt.ipp.estg.fittrack.core.weather

object WeatherRepository {
    suspend fun fetchCurrent(lat: Double, lon: Double): WeatherSnapshot? {
        return runCatching {
            WeatherClient.getSnapshot(latitude = lat, longitude = lon)
        }.getOrNull()
    }
}
