package pt.ipp.estg.fittrack.core.weather

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object WeatherClient {

    private const val BASE_URL = "https://api.open-meteo.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val api: OpenMeteoApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    suspend fun getSnapshot(latitude: Double, longitude: Double): WeatherSnapshot {
        val res = api.getCurrentWeather(lat = latitude, lon = longitude)
        val cw = res.currentWeather
        return WeatherSnapshot(
            tempC = cw?.temperature,
            windKmh = cw?.windspeed,
            code = cw?.weathercode,
            isoTime = cw?.time
        )
    }
}
