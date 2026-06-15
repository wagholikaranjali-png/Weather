package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.CurrentWeather
import com.example.data.api.DailyWeather
import com.example.data.api.ForecastResponse
import com.example.data.api.HourlyWeather
import com.example.data.db.CityEntity
import com.example.utils.WeatherUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSavedState.collectAsStateWithLifecycle()
    val forecastState by viewModel.forecastState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val savedCities by viewModel.savedCities.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    // Extract ambient background gradient depending on selected weather or standard sky
    val weatherCode = when (val state = forecastState) {
        is WeatherViewModel.ForecastState.Success -> state.forecast.current?.weatherCode ?: 0
        else -> 0
    }
    
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF111318) else Color(0xFFF7F9FF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Search Input Panel and Title
            SearchBarRow(
                query = searchQuery,
                onQueryChange = { viewModel.performSearch(it) },
                onSearchCleared = { viewModel.performSearch("") },
                selectedCity = selectedCity,
                isSaved = isSaved,
                onToggleBookmark = { viewModel.toggleBookmark() }
            )

            Box(modifier = Modifier.weight(1f)) {
                // If query is not empty, overlap search results
                if (searchQuery.trim().isNotEmpty()) {
                    SearchResultsList(
                        results = searchResults,
                        isSearching = isSearching,
                        onResultClick = {
                            viewModel.selectCityFromResult(it)
                            focusManager.clearFocus()
                        }
                    )
                } else {
                    // Normal Weather Content Presentation
                    NormalWeatherContent(
                        forecastState = forecastState,
                        aiState = aiState,
                        savedCities = savedCities,
                        selectedCityId = selectedCity?.id ?: 0L,
                        onCitySelect = { viewModel.selectCity(it) },
                        onCityDelete = { viewModel.deleteCity(it) },
                        onRefresh = { viewModel.refreshForecast() }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchCleared: () -> Unit,
    selectedCity: CityEntity?,
    isSaved: Boolean,
    onToggleBookmark: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderStrokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = contentColor.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.width(10.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search dynamic world cities...",
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    cursorColor = contentColor
                ),
                singleLine = true
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onSearchCleared) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                }
            } else if (selectedCity != null) {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_button")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark location",
                        tint = if (isSaved) Color(0xFFFFD200) else contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsList(
    results: List<com.example.data.api.GeocodingResult>,
    isSearching: Boolean,
    onResultClick: (com.example.data.api.GeocodingResult) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4))
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = "Not found",
                        tint = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching coordinates found.",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onResultClick(result) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Place",
                            tint = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.name,
                                color = contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = listOfNotNull(result.admin1, result.country).joinToString(", "),
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select",
                            tint = contentColor.copy(alpha = 0.5f)
                        )
                    }
                    HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun NormalWeatherContent(
    forecastState: WeatherViewModel.ForecastState,
    aiState: WeatherViewModel.AiRecommendationState,
    savedCities: List<CityEntity>,
    selectedCityId: Long,
    onCitySelect: (CityEntity) -> Unit,
    onCityDelete: (CityEntity) -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Saved locations horizontal carousel
        if (savedCities.isNotEmpty()) {
            SavedCitiesHorizontalCarousel(
                cities = savedCities,
                selectedId = selectedCityId,
                onSelect = onCitySelect,
                onDelete = onCityDelete
            )
        }

        when (forecastState) {
            is WeatherViewModel.ForecastState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Begin by searching a location.", color = Color.White)
                }
            }
            is WeatherViewModel.ForecastState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing atmosphere metrics...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            is WeatherViewModel.ForecastState.Success -> {
                val forecast = forecastState.forecast
                
                // Active weather snapshot card
                WeatherSnapshotCard(
                    cityName = savedCities.find { it.id == selectedCityId }?.name ?: "Current Location",
                    country = savedCities.find { it.id == selectedCityId }?.country,
                    current = forecast.current,
                    daily = forecast.daily
                )

                // Gemini AI Recommendations Smart Card
                GeminiAiAdvisorCard(aiState)

                // Hourly forecast horizontal card list
                HourlyForecastCard(forecast.hourly)

                // Canvas Dynamic Forecast Temperature Trend-line Chart
                HourlyTemperatureChart(forecast.hourly)

                // High-End Grid of weather metrics (wind, humidity, pressure, etc.)
                AtmosphereMetricsGrid(forecast.current)

                // 7-day extended forecasts
                DailyWeekForecastCard(forecast.daily)

                Spacer(modifier = Modifier.height(36.dp))
            }
            is WeatherViewModel.ForecastState.Error -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.22f)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Failed to sync weather matrix.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = forecastState.message,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Retry Sync", color = Color(0xFF1F2C3E))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedCitiesHorizontalCarousel(
    cities: List<CityEntity>,
    selectedId: Long,
    onSelect: (CityEntity) -> Unit,
    onDelete: (CityEntity) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.forEach { city ->
            val isSelected = city.id == selectedId
            val chipBg = if (isSelected) {
                if (isDark) Color(0xFF1F3D60) else Color(0xFFD3E3FD)
            } else {
                if (isDark) Color(0xFF1E2025) else Color.White
            }
            val chipBorder = if (isSelected) {
                if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)
            } else {
                if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
            }
            val contentColor = if (isSelected) {
                if (isDark) Color.White else Color(0xFF001D35)
            } else {
                if (isDark) Color(0xFFC4C6D0) else Color(0xFF44474E)
            }
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(chipBg)
                    .border(BorderStroke(1.dp, chipBorder), RoundedCornerShape(16.dp))
                    .clickable { onSelect(city) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) {
                        if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)
                    } else contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = city.name,
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
                
                if (cities.size > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete City",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onDelete(city) }
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherSnapshotCard(
    cityName: String,
    country: String?,
    current: CurrentWeather?,
    daily: DailyWeather?
) {
    if (current == null) return

    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1F3D60) else Color(0xFFD3E3FD)
    val contentColor = if (isDark) Color.White else Color(0xFF001D35)
    val subContentColor = if (isDark) Color(0xFFBAC7DB) else Color(0xFF44474E)

    val conditionText = WeatherUtils.getWeatherDescription(current.weatherCode)
    val maxTemp = daily?.maxTemperatures?.firstOrNull()?.roundToInt() ?: current.temperature.roundToInt()
    val minTemp = daily?.minTemperatures?.firstOrNull()?.roundToInt() ?: current.temperature.roundToInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // City metadata
            Column {
                Text(
                    text = cityName,
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                if (country != null) {
                    Text(
                        text = country.uppercase(),
                        color = subContentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Temperature numeric metric scale
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${current.temperature.roundToInt()}°",
                            color = contentColor,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp
                        )
                    }
                    Text(
                        text = conditionText,
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "H: $maxTemp°  L: $minTemp°",
                        color = subContentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Custom dynamic canvas graphic icon
                WeatherIcon(
                    code = current.weatherCode,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun GeminiAiAdvisorCard(aiState: WeatherViewModel.AiRecommendationState) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini AI",
                    tint = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini AI Advisor",
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFD3E3FD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = if (isDark) Color.White else Color(0xFF0061A4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (aiState) {
                is WeatherViewModel.AiRecommendationState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Synthesizing dynamic custom itinerary advice...",
                            color = contentColor.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
                is WeatherViewModel.AiRecommendationState.Success -> {
                    Text(
                        text = aiState.advice,
                        color = contentColor,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                is WeatherViewModel.AiRecommendationState.Error -> {
                    Text(
                        text = "Unable to contact advisor context. Displaying backup offline meteorology summaries instead.",
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun HourlyForecastCard(hourly: HourlyWeather?) {
    if (hourly == null) return

    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hourly Forecast",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show next 12 hours
                for (i in 0 until minOf(hourly.time.size, 12)) {
                    val rawTime = hourly.time[i]
                    val temp = hourly.temperatures[i].roundToInt()
                    val code = hourly.weatherCodes[i]

                    // Parse hour string safely e.g. "2026-06-15T10:00" -> "10:00"
                    val hourText = try {
                        val localDateTime = LocalDateTime.parse(rawTime)
                        localDateTime.format(DateTimeFormatter.ofPattern("h a"))
                    } catch (e: Exception) {
                        rawTime.substringAfter("T")
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = hourText,
                            color = contentColor.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        WeatherIcon(
                            code = code,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$temp°",
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyTemperatureChart(hourly: HourlyWeather?) {
    if (hourly == null || hourly.temperatures.size < 8) return

    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Temperature Trend (Next 8 Hours)",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Render beautiful interactive trend-line on Canvas
            val temps = hourly.temperatures.take(8)
            val times = hourly.time.take(8).map { raw ->
                try {
                    val localDateTime = LocalDateTime.parse(raw)
                    localDateTime.format(DateTimeFormatter.ofPattern("h a"))
                } catch (e: Exception) {
                    raw.substringAfter("T")
                }
            }

            val chartColor1 = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val path = Path()
                val minT = temps.minOrNull() ?: 0.0
                val maxT = temps.maxOrNull() ?: 100.0
                val range = if (maxT - minT == 0.0) 1.0 else (maxT - minT)

                val width = size.width
                val height = size.height
                val pointCount = temps.size
                val stepX = width / (pointCount - 1)
                
                // Graph coordinates mapped
                val points = temps.mapIndexed { index, t ->
                    val x = index * stepX
                    // Normalized to lie between 20dp and max-20dp of canvas height
                    val y = height - 25.dp.toPx() - (((t - minT) / range) * (height - 50.dp.toPx())).toFloat()
                    Offset(x, y)
                }

                // Draw gradient filled area under graph path
                val areaPath = Path().apply {
                    moveTo(0f, height)
                    points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(chartColor1.copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Draw trend curve
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    // Use clean cubic coordinates to make curved bezier joints
                    val cp1 = Offset((points[i - 1].x + points[i].x) / 2, points[i - 1].y)
                    val cp2 = Offset((points[i - 1].x + points[i].x) / 2, points[i].y)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, points[i].x, points[i].y)
                }

                drawPath(
                    path = path,
                    color = chartColor1,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight values & draws
                points.forEachIndexed { i, point ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = chartColor1,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }

            // Timeline labels below Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                times.forEach { time ->
                    Text(
                        text = time,
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(42.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AtmosphereMetricsGrid(current: CurrentWeather?) {
    if (current == null) return

    val isDark = isSystemInDarkTheme()
    val progressColor = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)
    val progressTrackColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE1E2E8)
    val subTextColor = if (isDark) Color(0xFFBAC7DB) else Color(0xFF44474E)

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "Wind Speed",
                    value = "${current.windSpeed} km/h",
                    subtitle = "SW Atmosphere draft",
                    icon = Icons.Default.WindPower,
                    extraContent = {
                        Text(
                            text = if (current.windSpeed > 20) "💨 Strong breeze" else "🍃 Mild airflow",
                            color = subTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "Air Humidity",
                    value = "${current.humidity.roundToInt()}%",
                    subtitle = "Vapor concentration",
                    icon = Icons.Default.WaterDrop,
                    extraContent = {
                        Column {
                            LinearProgressIndicator(
                                progress = { current.humidity.toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = progressColor,
                                trackColor = progressTrackColor,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "The dew point is healthy.",
                                color = subTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "UV Index",
                    value = "${current.uvIndex}",
                    subtitle = "Solar radiation",
                    icon = Icons.Default.WbSunny,
                    extraContent = {
                        val uvCategory = when {
                            current.uvIndex < 3 -> "🟢 Low risk"
                            current.uvIndex < 6 -> "🟡 Moderate"
                            else -> "🔴 Very High"
                        }
                        Text(text = uvCategory, color = subTextColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricCard(
                    title = "Air Pressure",
                    value = "${current.pressure.roundToInt()} hPa",
                    subtitle = "Barometric force",
                    icon = Icons.Default.Compress,
                    extraContent = {
                        Text(
                            text = if (current.pressure > 1013) "📈 Dry High pressure" else "📉 Wet Low pressure",
                            color = subTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    extraContent: @Composable () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)
    val titleColor = if (isDark) Color(0xFFC4C6D0) else Color(0xFF44474E)
    val subTextColor = if (isDark) Color(0xFF8E9099) else Color(0xFF74777F)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title.uppercase(),
                    color = titleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = contentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = subTextColor,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            extraContent()
        }
    }
}

@Composable
fun DailyWeekForecastCard(daily: DailyWeather?) {
    if (daily == null) return

    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E2025) else Color.White
    val borderColor = if (isDark) Color(0xFF2D3035) else Color(0xFFE1E2E8)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.82f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "7-Day Forecast",
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            for (i in 0 until minOf(daily.time.size, 7)) {
                val rawDate = daily.time[i]
                val maxTemp = daily.maxTemperatures[i].roundToInt()
                val minTemp = daily.minTemperatures[i].roundToInt()
                val code = daily.weatherCodes[i]
                val rainSum = daily.precipitationSums[i]

                val dayOfWeek = try {
                    val localDate = java.time.LocalDate.parse(rawDate)
                    if (java.time.LocalDate.now() == localDate) {
                        "Today"
                    } else {
                        localDate.format(DateTimeFormatter.ofPattern("EEEE"))
                    }
                } catch (e: Exception) {
                    rawDate
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayOfWeek,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.width(100.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Brief Precipitation summary tag
                    if (rainSum > 0.0) {
                        Surface(
                            color = (if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4)).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", rainSum)}mm",
                                color = if (isDark) Color(0xFF6DD5ED) else Color(0xFF0061A4),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    WeatherIcon(
                        code = code,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "$minTemp°",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$maxTemp°",
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp)
                    )
                }

                if (i < minOf(daily.time.size, 7) - 1) {
                    HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
                }
            }
        }
    }
}

/**
 * HIGH-FIDELITY CUSTOM CANVAS DRAWN WEATHER ICONS
 * Avoids plain gray unstyled symbols or loading bitmaps. Displays fully responsive curves/spheres.
 */
@Composable
fun WeatherIcon(code: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        when (code) {
            // Clear sky (Sunny) -> glowing sphere with radial dash lines
            0, 1 -> {
                val center = Offset(width / 2, height / 2)
                val radius = width * 0.28f
                // Glowing background aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF200).copy(alpha = 0.45f), Color.Transparent),
                        center = center,
                        radius = width * 0.45f
                    )
                )
                // Center sun
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFD200), Color(0xFFFF9000))
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Mainly clear / partly cloudy -> cloud overlapping a glowing Sun
            2, 3 -> {
                val sunCenter = Offset(width * 0.65f, height * 0.35f)
                val sunRadius = width * 0.18f
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFD200), Color(0xFFFF9000))
                    ),
                    radius = sunRadius,
                    center = sunCenter
                )

                // Cloudy Overlap
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(20 * scaleX, 70 * scaleY)
                    lineTo(80 * scaleX, 70 * scaleY)
                    cubicTo(95 * scaleX, 70 * scaleY, 95 * scaleX, 50 * scaleY, 80 * scaleX, 50 * scaleY)
                    cubicTo(80 * scaleX, 30 * scaleY, 55 * scaleX, 25 * scaleY, 50 * scaleX, 35 * scaleY)
                    cubicTo(45 * scaleX, 20 * scaleY, 25 * scaleX, 25 * scaleY, 25 * scaleX, 40 * scaleY)
                    cubicTo(10 * scaleX, 40 * scaleY, 5 * scaleX, 55 * scaleY, 20 * scaleX, 70 * scaleY)
                    close()
                }

                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFCFD8DC))
                    )
                )
            }

            // Overcast, Fog -> Fog/Mist lines with layered clouds
            45, 48 -> {
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(15 * scaleX, 60 * scaleY)
                    lineTo(85 * scaleX, 60 * scaleY)
                    cubicTo(98 * scaleX, 60 * scaleY, 98 * scaleX, 42 * scaleY, 82 * scaleX, 42 * scaleY)
                    cubicTo(80 * scaleX, 20 * scaleY, 55 * scaleX, 15 * scaleY, 48 * scaleX, 28 * scaleY)
                    cubicTo(43 * scaleX, 12 * scaleY, 22 * scaleX, 18 * scaleY, 22 * scaleX, 32 * scaleY)
                    cubicTo(5 * scaleX, 32 * scaleY, 2 * scaleX, 48 * scaleY, 15 * scaleX, 60 * scaleY)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5))
                    )
                )

                // Fog lines
                val scaleX = width / 100f
                val scaleY = height / 100f
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(25 * scaleX, 72 * scaleY),
                    end = Offset(75 * scaleX, 72 * scaleY),
                    strokeWidth = 3f * density,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(35 * scaleX, 81 * scaleY),
                    end = Offset(65 * scaleX, 81 * scaleY),
                    strokeWidth = 3f * density,
                    cap = StrokeCap.Round
                )
            }

            // Rain & showers -> Dark Cloud + dynamic falling diagonal raindrops
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> {
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(15 * scaleX, 55 * scaleY)
                    lineTo(85 * scaleX, 55 * scaleY)
                    cubicTo(98 * scaleX, 55 * scaleY, 91 * scaleX, 38 * scaleY, 80 * scaleX, 38 * scaleY)
                    cubicTo(78 * scaleX, 18 * scaleY, 50 * scaleX, 12 * scaleY, 48 * scaleX, 24 * scaleY)
                    cubicTo(43 * scaleX, 8 * scaleY, 20 * scaleX, 14 * scaleY, 22 * scaleX, 28 * scaleY)
                    cubicTo(5 * scaleX, 28 * scaleY, 2 * scaleX, 42 * scaleY, 15 * scaleX, 55 * scaleY)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFCFD8DC), Color(0xFF546E7A))
                    )
                )

                // Rain drops sliding down (diagonal slopes)
                val scaleX = width / 100f
                val scaleY = height / 100f
                val drops = listOf(
                    Offset(30 * scaleX, 64 * scaleY) to Offset(26 * scaleX, 78 * scaleY),
                    Offset(46 * scaleX, 66 * scaleY) to Offset(42 * scaleX, 80 * scaleY),
                    Offset(62 * scaleX, 64 * scaleY) to Offset(58 * scaleX, 78 * scaleY),
                    Offset(74 * scaleX, 68 * scaleY) to Offset(70 * scaleX, 82 * scaleY)
                )

                drops.forEach { drop ->
                    drawLine(
                        color = Color(0xFF6DD5ED),
                        start = drop.first,
                        end = drop.second,
                        strokeWidth = 2.5f * density,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Snow -> Snow cloud + micro crystal circles
            56, 57, 66, 67, 71, 73, 75, 77, 85, 86 -> {
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(15 * scaleX, 55 * scaleY)
                    lineTo(85 * scaleX, 55 * scaleY)
                    cubicTo(98 * scaleX, 55 * scaleY, 91 * scaleX, 38 * scaleY, 80 * scaleX, 38 * scaleY)
                    cubicTo(78 * scaleX, 18 * scaleY, 50 * scaleX, 12 * scaleY, 48 * scaleX, 24 * scaleY)
                    cubicTo(43 * scaleX, 8 * scaleY, 20 * scaleX, 14 * scaleY, 22 * scaleX, 28 * scaleY)
                    cubicTo(5 * scaleX, 28 * scaleY, 2 * scaleX, 42 * scaleY, 15 * scaleX, 55 * scaleY)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC))
                    )
                )

                // White fuzzy snowflakes drawn as circular coordinates
                val scaleX = width / 100f
                val scaleY = height / 100f
                drawCircle(color = Color.White, radius = 2.5f * density, center = Offset(32 * scaleX, 70 * scaleY))
                drawCircle(color = Color.White, radius = 3.5f * density, center = Offset(48 * scaleX, 78 * scaleY))
                drawCircle(color = Color.White, radius = 2.0f * density, center = Offset(64 * scaleX, 71 * scaleY))
                drawCircle(color = Color.White, radius = 3.0f * density, center = Offset(52 * scaleX, 66 * scaleY))
            }

            // Thunderstorm -> Charcoal Cloud + energetic lightning bolt
            95, 96, 99 -> {
                val cloudPath = Path().apply {
                    val scaleX = width / 100f
                    val scaleY = height / 100f
                    moveTo(15 * scaleX, 55 * scaleY)
                    lineTo(85 * scaleX, 55 * scaleY)
                    cubicTo(98 * scaleX, 55 * scaleY, 91 * scaleX, 35 * scaleY, 80 * scaleX, 35 * scaleY)
                    cubicTo(78 * scaleX, 12 * scaleY, 50 * scaleX, 8 * scaleY, 48 * scaleX, 21 * scaleY)
                    cubicTo(43 * scaleX, 6 * scaleY, 20 * scaleX, 10 * scaleY, 22 * scaleX, 24 * scaleY)
                    cubicTo(5 * scaleX, 24 * scaleY, 2 * scaleX, 38 * scaleY, 15 * scaleX, 55 * scaleY)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF455A64), Color(0xFF263238))
                    )
                )

                // Lightning bold drawn down
                val scaleX = width / 100f
                val scaleY = height / 100f
                val boltPath = Path().apply {
                    moveTo(50 * scaleX, 50 * scaleY)
                    lineTo(40 * scaleX, 70 * scaleY)
                    lineTo(52 * scaleX, 70 * scaleY)
                    lineTo(45 * scaleX, 90 * scaleY)
                    lineTo(62 * scaleX, 64 * scaleY)
                    lineTo(50 * scaleX, 64 * scaleY)
                    close()
                }
                drawPath(
                    path = boltPath,
                    color = Color(0xFFFFD600)
                )
            }

            // Unknown condition fallback icon: simple sun cloud combination
            else -> {
                val center = Offset(width / 2, height / 2)
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = width * 0.3f,
                    center = center
                )
            }
        }
    }
}
