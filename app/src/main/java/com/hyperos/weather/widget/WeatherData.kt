package com.hyperos.weather.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

internal data class WeatherDay(
    val day: String,
    val min: Int,
    val max: Int,
    val description: String
)

internal data class WeatherSnapshot(
    val temperature: Int,
    val city: String,
    val description: String,
    val days: List<WeatherDay>
)

internal object WeatherRepository {
    suspend fun load(context: Context): WeatherSnapshot = withContext(Dispatchers.IO) {
        val location = findLocation(context) ?: return@withContext fallback()
        val lat = location.latitude
        val lon = location.longitude
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=4"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }
        val json = try {
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }

        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val max = daily.getJSONArray("temperature_2m_max")
        val min = daily.getJSONArray("temperature_2m_min")
        val codes = daily.getJSONArray("weather_code")
        val days = (0 until minOf(4, dates.length())).map { i ->
            WeatherDay(
                day = if (i == 0) "Hôm nay" else shortDay(dates.getString(i)),
                min = min.getDouble(i).toInt(),
                max = max.getDouble(i).toInt(),
                description = weatherText(codes.getInt(i))
            )
        }
        WeatherSnapshot(
            temperature = current.getDouble("temperature_2m").toInt(),
            city = cityName(context, lat, lon),
            description = weatherText(current.getInt("weather_code")),
            days = days.drop(1).take(3)
        )
    }

    private fun findLocation(context: Context): android.location.Location? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val candidates = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
        val last = candidates.maxByOrNull { it.time }
        if (last != null) return last

        val provider = when {
            coarse && runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            fine && runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
            else -> null
        } ?: return null

        return runCatching {
            kotlinx.coroutines.runBlocking {
                suspendCancellableCoroutine { cont ->
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            if (cont.isActive) cont.resume(location)
                            runCatching { lm.removeUpdates(this) }
                        }
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    runCatching {
                        lm.requestLocationUpdates(provider, 0L, 0f, listener, context.mainLooper)
                    }.onFailure { if (cont.isActive) cont.resume(null) }
                    cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                        kotlinx.coroutines.delay(7000)
                        if (cont.isActive) {
                            runCatching { lm.removeUpdates(listener) }
                            cont.resume(null)
                        }
                    }
                }
            }
        }.getOrNull()
    }

    private fun cityName(context: Context, lat: Double, lon: Double): String {
        return runCatching {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
            val a = addresses?.firstOrNull()
            a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: "Vị trí hiện tại"
        }.getOrDefault("Vị trí hiện tại")
    }

    private fun shortDay(iso: String): String {
        val parts = iso.split("-")
        if (parts.size != 3) return iso
        return try {
            val cal = java.util.Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            }
            java.text.SimpleDateFormat("EEE", Locale("vi", "VN")).format(cal.time)
                .replaceFirstChar { it.titlecase(Locale("vi", "VN")) }
        } catch (_: Exception) { iso }
    }

    private fun weatherText(code: Int): String = when (code) {
        0 -> "Trời quang"
        1, 2 -> "Có mây"
        3 -> "Nhiều mây"
        45, 48 -> "Sương mù"
        51, 53, 55, 56, 57 -> "Mưa phùn"
        61, 63, 65, 66, 67 -> "Mưa"
        71, 73, 75, 77 -> "Tuyết"
        80, 81, 82 -> "Mưa rào"
        85, 86 -> "Mưa tuyết"
        95, 96, 99 -> "Dông"
        else -> "Không xác định"
    }

    private fun fallback() = WeatherSnapshot(
        temperature = 0,
        city = "Cho phép vị trí",
        description = "Mở ứng dụng để cấp quyền",
        days = emptyList()
    )
}
