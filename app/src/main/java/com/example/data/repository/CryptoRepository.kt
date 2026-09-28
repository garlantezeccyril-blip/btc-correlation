package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.CandleDao
import com.example.data.local.CandleEntity
import com.example.data.local.CryptoAssetEntity
import com.example.data.remote.CoinbaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

enum class Timeframe(val code: String, val label: String, val baseGranularity: Int, val aggregateFactor: Int) {
    M15("15m", "15m", 900, 1),
    H1("1h", "1H", 3600, 1),
    H4("4h", "4H", 3600, 4),
    D1("1d", "1D", 86400, 1),
    D3("3d", "3D", 86400, 3),
    D7("7d", "7D", 86400, 7);

    companion object {
        fun fromCode(code: String): Timeframe = values().find { it.code.equals(code, ignoreCase = true) } ?: H1
    }
}

enum class LookbackRange(val code: String, val label: String, val days: Int) {
    DAYS_30("30d", "30 J", 30),
    DAYS_90("90d", "90 J", 90),
    YEAR_1("1y", "1 AN", 365),
    YEAR_3("3y", "3 ANS", 1095);

    companion object {
        fun fromCode(code: String): LookbackRange = values().find { it.code.equals(code, ignoreCase = true) } ?: DAYS_90
    }
}

class CryptoRepository(
    private val database: AppDatabase
) {
    private val candleDao: CandleDao = database.candleDao()
    private val api = CoinbaseClient.api

    companion object {
        const val BTC_SYMBOL = "BTC-USD"
        const val TAG = "CryptoRepo"

        val DEFAULT_ASSETS = listOf(
            CryptoAssetEntity("BTC-USD", "BTC", "Bitcoin", true, 0),
            CryptoAssetEntity("SOL-USD", "SOL", "Solana", true, 1),
            CryptoAssetEntity("ETH-USD", "ETH", "Ethereum", true, 2),
            CryptoAssetEntity("XRP-USD", "XRP", "XRP", false, 3),
            CryptoAssetEntity("ADA-USD", "ADA", "Cardano", false, 4),
            CryptoAssetEntity("DOGE-USD", "DOGE", "Dogecoin", false, 5),
            CryptoAssetEntity("AVAX-USD", "AVAX", "Avalanche", false, 6),
            CryptoAssetEntity("LINK-USD", "LINK", "Chainlink", false, 7),
            CryptoAssetEntity("SUI-USD", "SUI", "Sui", false, 8),
            CryptoAssetEntity("NEAR-USD", "NEAR", "NEAR Protocol", false, 9),
            CryptoAssetEntity("DOT-USD", "DOT", "Polkadot", false, 10),
            CryptoAssetEntity("APT-USD", "APT", "Aptos", false, 11),
            CryptoAssetEntity("SHIB-USD", "SHIB", "Shiba Inu", false, 12),
            CryptoAssetEntity("LTC-USD", "LTC", "Litecoin", false, 13),
            CryptoAssetEntity("BCH-USD", "BCH", "Bitcoin Cash", false, 14),
            CryptoAssetEntity("UNI-USD", "UNI", "Uniswap", false, 15),
            CryptoAssetEntity("ICP-USD", "ICP", "Internet Computer", false, 16),
            CryptoAssetEntity("AAVE-USD", "AAVE", "Aave", false, 17),
            CryptoAssetEntity("RENDER-USD", "RENDER", "Render", false, 18),
            CryptoAssetEntity("PEPE-USD", "PEPE", "Pepe", false, 19)
        )
    }

    val assetsFlow: Flow<List<CryptoAssetEntity>> = candleDao.getAllAssetsFlow()

    suspend fun initializeCatalog() = withContext(Dispatchers.IO) {
        val existing = candleDao.getAllAssets()
        if (existing.isEmpty()) {
            candleDao.insertAssets(DEFAULT_ASSETS)
        }
        // Try refreshing products from Coinbase
        try {
            val products = api.getProducts()
            val usdProducts = products.filter {
                it.quoteCurrency == "USD" && (it.status == "online" || it.status == null)
            }.take(100) // Top 100 USD spot pairs

            if (usdProducts.isNotEmpty()) {
                val entities = usdProducts.mapIndexed { index, p ->
                    CryptoAssetEntity(
                        symbol = p.id,
                        baseCurrency = p.baseCurrency,
                        displayName = p.displayName ?: p.baseCurrency,
                        isFavorite = p.id == BTC_SYMBOL || p.id == "SOL-USD" || p.id == "ETH-USD",
                        orderRank = when (p.id) {
                            BTC_SYMBOL -> 0
                            "SOL-USD" -> 1
                            "ETH-USD" -> 2
                            else -> 10 + index
                        }
                    )
                }
                candleDao.insertAssets(entities)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch dynamic products from Coinbase: ${e.message}")
        }
    }

    suspend fun getAllAssets(): List<CryptoAssetEntity> = withContext(Dispatchers.IO) {
        val list = candleDao.getAllAssets()
        if (list.isEmpty()) {
            candleDao.insertAssets(DEFAULT_ASSETS)
            DEFAULT_ASSETS
        } else list
    }

    suspend fun getCandles(
        symbol: String,
        timeframe: Timeframe,
        lookbackRange: LookbackRange,
        forceRefresh: Boolean = false
    ): List<CandleEntity> = withContext(Dispatchers.IO) {
        val minTimestamp = Instant.now()
            .minus(lookbackRange.days.toLong(), ChronoUnit.DAYS)
            .epochSecond

        // Check local cache
        val cached = candleDao.getCandlesSince(symbol, timeframe.code, minTimestamp)
        val expectedMinCandles = when (timeframe) {
            Timeframe.M15 -> 50
            Timeframe.H1 -> 40
            Timeframe.H4 -> 20
            Timeframe.D1 -> 15
            Timeframe.D3 -> 8
            Timeframe.D7 -> 4
        }

        if (!forceRefresh && cached.size >= expectedMinCandles) {
            return@withContext cached
        }

        // Fetch from Coinbase
        try {
            val fetchedCandles = fetchFromCoinbase(symbol, timeframe)
            if (fetchedCandles.isNotEmpty()) {
                candleDao.insertCandles(fetchedCandles)
                return@withContext candleDao.getCandlesSince(symbol, timeframe.code, minTimestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching candles for $symbol $timeframe: ${e.message}")
        }

        // Return whatever cache exists
        return@withContext cached
    }

    private suspend fun fetchFromCoinbase(
        symbol: String,
        timeframe: Timeframe
    ): List<CandleEntity> {
        val rawCandles = api.getCandles(
            productId = symbol,
            granularity = timeframe.baseGranularity
        )

        // Raw candle format: [ time, low, high, open, close, volume ]
        val parsed = rawCandles.mapNotNull { row ->
            if (row.size >= 6) {
                CandleEntity(
                    symbol = symbol,
                    timeframe = timeframe.code,
                    timestamp = row[0].toLong(),
                    low = row[1],
                    high = row[2],
                    open = row[3],
                    close = row[4],
                    volume = row[5]
                )
            } else null
        }.sortedBy { it.timestamp }

        // If aggregation needed (e.g. 4H from 1H, or 3D/7D from 1D)
        if (timeframe.aggregateFactor > 1 && parsed.isNotEmpty()) {
            return aggregateCandles(parsed, timeframe)
        }

        return parsed
    }

    private fun aggregateCandles(
        source: List<CandleEntity>,
        targetTimeframe: Timeframe
    ): List<CandleEntity> {
        val factor = targetTimeframe.aggregateFactor
        val result = mutableListOf<CandleEntity>()

        for (i in 0 until source.size step factor) {
            val chunk = source.subList(i, min(i + factor, source.size))
            if (chunk.isEmpty()) continue

            val open = chunk.first().open
            val close = chunk.last().close
            val high = chunk.maxOf { it.high }
            val low = chunk.minOf { it.low }
            val volume = chunk.sumOf { it.volume }
            val timestamp = chunk.first().timestamp

            result.add(
                CandleEntity(
                    symbol = chunk.first().symbol,
                    timeframe = targetTimeframe.code,
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
        }
        return result
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        candleDao.clearAllCandles()
    }

    suspend fun getTotalCandlesStored(): Int = withContext(Dispatchers.IO) {
        candleDao.getTotalCandleCount()
    }
}
