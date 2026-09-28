package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "candles",
    primaryKeys = ["symbol", "timeframe", "timestamp"],
    indices = [
        Index(value = ["symbol", "timeframe", "timestamp"]),
        Index(value = ["symbol", "timeframe"])
    ]
)
data class CandleEntity(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

@Entity(tableName = "crypto_assets")
data class CryptoAssetEntity(
    @PrimaryKey val symbol: String,
    val baseCurrency: String,
    val displayName: String,
    val isFavorite: Boolean = false,
    val orderRank: Int = 0
)
