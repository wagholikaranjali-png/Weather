package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM saved_cities ORDER BY timestamp DESC")
    fun getAllSavedCities(): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity)

    @Delete
    suspend fun deleteCity(city: CityEntity)

    @Query("SELECT EXISTS(SELECT * FROM saved_cities WHERE id = :id)")
    suspend fun isCitySaved(id: Long): Boolean

    @Query("SELECT * FROM saved_cities WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCity(): CityEntity?

    @Query("UPDATE saved_cities SET isDefault = 0")
    suspend fun clearDefaultCity()

    @Query("UPDATE saved_cities SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultCity(id: Long)
}
