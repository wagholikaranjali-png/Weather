package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ForecastResponse
import com.example.data.api.GeocodingResult
import com.example.data.db.CityEntity
import com.example.data.db.WeatherDatabase
import com.example.data.repository.WeatherRepository
import com.example.utils.WeatherUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    // City state
    private val _selectedCity = MutableStateFlow<CityEntity?>(null)
    val selectedCity = _selectedCity.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Search results
    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // Is searching state
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Saving City Loading State
    private val _isSavedState = MutableStateFlow(false)
    val isSavedState = _isSavedState.asStateFlow()

    // Forecast State
    sealed interface ForecastState {
        object Idle : ForecastState
        object Loading : ForecastState
        data class Success(val forecast: ForecastResponse) : ForecastState
        data class Error(val message: String) : ForecastState
    }

    private val _forecastState = MutableStateFlow<ForecastState>(ForecastState.Idle)
    val forecastState: StateFlow<ForecastState> = _forecastState.asStateFlow()

    // Gemini AI recommendation state
    private val _aiState = MutableStateFlow<AiRecommendationState>(AiRecommendationState.Idle)
    val aiState: StateFlow<AiRecommendationState> = _aiState.asStateFlow()

    sealed interface AiRecommendationState {
        object Idle : AiRecommendationState
        object Loading : AiRecommendationState
        data class Success(val advice: String) : AiRecommendationState
        data class Error(val message: String) : AiRecommendationState
    }

    // Saved cities lists
    val savedCities: StateFlow<List<CityEntity>> = repository.savedCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load default city or first favorited city on start
        viewModelScope.launch {
            val defaultCity = repository.getSavedDefaultCity()
            if (defaultCity != null) {
                selectCity(defaultCity)
            } else {
                // If no default city exists, check if there are saved cities
                repository.savedCities.collect { list ->
                    if (list.isNotEmpty() && _selectedCity.value == null) {
                        selectCity(list.first())
                    } else if (list.isEmpty() && _selectedCity.value == null) {
                        // Load a standard default starting city (e.g. San Francisco or New York)
                        val startCity = CityEntity(
                            id = 5391959L,
                            name = "San Francisco",
                            latitude = 37.77493,
                            longitude = -122.41942,
                            country = "United States",
                            admin1 = "California",
                            isDefault = true
                        )
                        repository.insertCity(startCity)
                        selectCity(startCity)
                    }
                }
            }
        }
    }

    /**
     * Search city results from Geocoding API
     */
    fun performSearch(query: String) {
        _searchQuery.value = query
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val results = repository.searchCity(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun selectCity(city: CityEntity) {
        _selectedCity.value = city
        checkIfSaved(city.id)
        loadForecast(city.latitude, city.longitude, city.name)
    }

    fun selectCityFromResult(result: GeocodingResult) {
        val city = CityEntity(
            id = result.id,
            name = result.name,
            latitude = result.latitude,
            longitude = result.longitude,
            country = result.country,
            admin1 = result.admin1
        )
        // Set search query empty and results empty to close the search panel
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        
        selectCity(city)
    }

    private fun checkIfSaved(id: Long) {
        viewModelScope.launch {
            _isSavedState.value = repository.isCitySaved(id)
        }
    }

    fun toggleBookmark() {
        val city = _selectedCity.value ?: return
        viewModelScope.launch {
            if (_isSavedState.value) {
                repository.deleteCity(city)
                _isSavedState.value = false
            } else {
                repository.insertCity(city.copy(timestamp = System.currentTimeMillis()))
                _isSavedState.value = true
            }
        }
    }

    fun deleteCity(city: CityEntity) {
        viewModelScope.launch {
            repository.deleteCity(city)
            if (_selectedCity.value?.id == city.id) {
                // Select another city if we deleted the current one
                val remaining = savedCities.value.filter { it.id != city.id }
                if (remaining.isNotEmpty()) {
                    selectCity(remaining.first())
                }
            }
        }
    }

    private fun loadForecast(lat: Double, lon: Double, name: String) {
        viewModelScope.launch {
            _forecastState.value = ForecastState.Loading
            _aiState.value = AiRecommendationState.Loading
            try {
                val response = repository.getForecast(lat, lon)
                _forecastState.value = ForecastState.Success(response)
                
                // Fetch AI Weather Advice
                val currentWeather = response.current
                if (currentWeather != null) {
                    val desc = WeatherUtils.getWeatherDescription(currentWeather.weatherCode)
                    val advice = repository.requestGeminiAdvice(
                        cityName = name,
                        temp = currentWeather.temperature,
                        condition = desc,
                        wind = currentWeather.windSpeed,
                        humidity = currentWeather.humidity,
                        uv = currentWeather.uvIndex
                    )
                    _aiState.value = AiRecommendationState.Success(advice)
                } else {
                    _aiState.value = AiRecommendationState.Error("No current weather data for AI analysis.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _forecastState.value = ForecastState.Error(e.message ?: "Failed to load forecast data.")
                _aiState.value = AiRecommendationState.Error("Connection error while loading AI guidance.")
            }
        }
    }

    fun refreshForecast() {
        _selectedCity.value?.let { selectCity(it) }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = WeatherDatabase.getDatabase(application)
                    val repository = WeatherRepository(database.cityDao())
                    @Suppress("UNCHECKED_CAST")
                    return WeatherViewModel(repository) as T
                }
            }
        }
    }
}
