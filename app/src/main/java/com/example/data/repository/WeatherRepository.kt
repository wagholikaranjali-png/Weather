package com.example.data.repository

import com.example.data.api.ForecastResponse
import com.example.data.api.GeocodingResult
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.RetrofitClient
import com.example.data.db.CityDao
import com.example.data.db.CityEntity
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(private val cityDao: CityDao) {

    val savedCities: Flow<List<CityEntity>> = cityDao.getAllSavedCities()

    suspend fun searchCity(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.geocodingService.searchCity(query)
            response.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getForecast(latitude: Double, longitude: Double): ForecastResponse = withContext(Dispatchers.IO) {
        RetrofitClient.forecastService.getForecast(latitude, longitude)
    }

    suspend fun insertCity(city: CityEntity) = withContext(Dispatchers.IO) {
        cityDao.insertCity(city)
    }

    suspend fun deleteCity(city: CityEntity) = withContext(Dispatchers.IO) {
        cityDao.deleteCity(city)
    }

    suspend fun isCitySaved(id: Long): Boolean = withContext(Dispatchers.IO) {
        cityDao.isCitySaved(id)
    }

    suspend fun setDefaultCity(id: Long) = withContext(Dispatchers.IO) {
        cityDao.clearDefaultCity()
        cityDao.setDefaultCity(id)
    }

    suspend fun getSavedDefaultCity(): CityEntity? = withContext(Dispatchers.IO) {
        cityDao.getDefaultCity()
    }

    suspend fun requestGeminiAdvice(
        cityName: String,
        temp: Double,
        condition: String,
        wind: Double,
        humidity: Double,
        uv: Double
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackAdvice(temp, condition, wind, humidity, uv)
        }

        val prompt = """
            You are an expert meteorological assistant writing for a modern weather app.
            Analyze the weather conditions for $cityName:
            - Temperature: $temp°C
            - Condition: $condition
            - Wind Speed: $wind km/h
            - Humidity: $humidity%
            - UV Index: $uv

            Generate a concise, elegant briefing (exactly 3 short, friendly sentences):
            1. Suggest perfect activities for this weather.
            2. Give exact outfit recommendations.
            3. Highlight a smart safety or planning tip (sunscreen, hydration, wind-shields).
            
            Use clean spacing, beautiful formatting, and a professional yet inviting tone.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.5f)
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: generateFallbackAdvice(temp, condition, wind, humidity, uv)
        } catch (e: Exception) {
            generateFallbackAdvice(temp, condition, wind, humidity, uv)
        }
    }

    private fun generateFallbackAdvice(
        temp: Double,
        condition: String,
        wind: Double,
        humidity: Double,
        uv: Double
    ): String {
        val activity = when {
            temp > 28 -> "☀️ Perfect for swimming, beach walks, or hitting air-conditioned venues."
            temp in 18.0..28.0 && !condition.contains("rain", ignoreCase = true) -> "🏃 Great day for jogging, outdoor picnics, or cycling!"
            condition.contains("rain", ignoreCase = true) -> "📚 Perfect weather for indoor reading, museums, or cozy cafe visits."
            temp < 10 -> "☕ Ideal for warm hot-chocolate dates and quiet indoor plans."
            else -> "⛅ Fully comfortable weather for a lovely stroll in the local park."
        }
        
        val clothing = when {
            temp > 28 -> "👕 Wear light linen shirts, breathable shorts, and sandals."
            temp in 18.0..28.0 -> "👕 A light t-shirt and jeans or comfy chinos should be highly comfortable."
            temp in 10.0..18.0 -> "🧥 Go for a light layers combo like a denim jacket, knit sweater, or utility windbreaker."
            temp < 10 -> "🧥 Put on heavy sweaters, cozy cardigans, and a thermal winter coat."
            else -> "👕 Dress in comfortable casual wear with an easy extra layer in case."
        }

        val tip = when {
            uv > 6 -> "🧴 Note: High UV index today! Generously apply SPF 30+ sunscreen and wear sunglasses."
            wind > 25 -> "💨 Wind warning: Expect strong drafts. Bring windproof apparel."
            condition.contains("rain", ignoreCase = true) -> "☔ Umbrella alert: Carry an umbrella or high quality waterproof rain jacket."
            humidity > 80 -> "💧 High humidity levels make it feel slightly warmer. Keep well hydrated!"
            else -> "🌱 Standard comfortable day. Grab your water flask and enjoy!"
        }

        return "$activity\n\n👔 Outfit: $clothing\n\n💡 Tip: $tip"
    }
}
