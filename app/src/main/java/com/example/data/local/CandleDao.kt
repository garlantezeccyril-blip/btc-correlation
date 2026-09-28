package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CandleDao {

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY timestamp ASC")
    suspend fun getCandles(symbol: String, timeframe: String): List<CandleEntity>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeframe = :timeframe AND timestamp >= :minTimestamp ORDER BY timestamp ASC")
    suspend fun getCandlesSince(symbol: String, timeframe: String, minTimestamp: Long): List<CandleEntity>

    @Query("SELECT * FROM candles WHERE timeframe = :timeframe AND timestamp >= :minTimestamp ORDER BY timestamp ASC")
    suspend fun getAllCandlesSince(timeframe: String, minTimestamp: Long): List<CandleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)

    @Query("SELECT COUNT(*) FROM candles WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun getCandleCount(symbol: String, timeframe: String): Int

    @Query("SELECT COUNT(*) FROM candles")
    suspend fun getTotalCandleCount(): Int

    @Query("DELETE FROM candles")
    suspend fun clearAllCandles()

    @Query("SELECT MAX(timestamp) FROM candles WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun getLatestTimestamp(symbol: String, timeframe: String): Long?

    // Assets
    @Query("SELECT * FROM crypto_assets ORDER BY orderRank ASC, symbol ASC")
    fun getAllAssetsFlow(): Flow<List<CryptoAssetEntity>>

    @Query("SELECT * FROM crypto_assets ORDER BY orderRank ASC, symbol ASC")
    suspend fun getAllAssets(): List<CryptoAssetEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssets(assets: List<CryptoAssetEntity>)

    @Query("UPDATE crypto_assets SET isFavorite = :isFav WHERE symbol = :symbol")
    suspend fun setFavorite(symbol: String, isFav: Boolean)
}
