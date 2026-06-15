package com.example.utils

import androidx.compose.ui.graphics.Color

object WeatherUtils {

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Foggy"
            48 -> "Depositing Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            56 -> "Light Freezing Drizzle"
            57 -> "Dense Freezing Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66 -> "Light Freezing Rain"
            67 -> "Heavy Freezing Rain"
            71 -> "Slight Snowfall"
            73 -> "Moderate Snowfall"
            75 -> "Heavy Snowfall"
            77 -> "Snow Grains"
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            85 -> "Slight Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with Slight Hail"
            99 -> "Thunderstorm with Heavy Hail"
            else -> "Unknown Weather"
        }
    }

    /**
     * Returns a dynamic background gradient color pair depending on the weather conditions.
     */
    fun getWeatherGradient(code: Int, isDark: Boolean = false): List<Color> {
        val baseColors = when (code) {
            // Clear and sunny
            0, 1 -> if (isDark) {
                listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            } else {
                listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))
            }
            // Cloudy
            2, 3 -> if (isDark) {
                listOf(Color(0xFF1F1C2C), Color(0xFF2C3E50))
            } else {
                listOf(Color(0xFF757F9A), Color(0xFFD7DDE8))
            }
            // Foggy
            45, 48 -> if (isDark) {
                listOf(Color(0xFF3E5151), Color(0xFF435B5B))
            } else {
                listOf(Color(0xFFACB6E5), Color(0xFF86FDE8))
            }
            // Rain and Drizzle
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> if (isDark) {
                listOf(Color(0xFF141E30), Color(0xFF243B55))
            } else {
                listOf(Color(0xFF3A6073), Color(0xFF3A7BD5))
            }
            // Snow
            56, 57, 66, 67, 71, 73, 75, 77, 85, 86 -> if (isDark) {
                listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
            } else {
                listOf(Color(0xFFE6DADA), Color(0xFF274046))
            }
            // Thunderstorm
            95, 96, 99 -> if (isDark) {
                listOf(Color(0xFF0F0C20), Color(0xFF15102A), Color(0xFF06040A))
            } else {
                listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            }
            else -> if (isDark) {
                listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
            } else {
                listOf(Color(0xFF4B6CB7), Color(0xFF182848))
            }
        }
        return baseColors
    }
}
