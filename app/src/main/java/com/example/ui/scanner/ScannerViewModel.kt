package com.example.ui.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CandleEntity
import com.example.data.local.CryptoAssetEntity
import com.example.data.repository.CryptoRepository
import com.example.data.repository.LookbackRange
import com.example.data.repository.Timeframe
import com.example.engine.AnalysisEngine
import com.example.engine.ReliabilityConfig
import com.example.engine.WalkForwardEngine
import com.example.engine.WalkForwardReport
import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.CryptoAnalysisResult
import com.example.statistics.ReliabilityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class AnalysisMode(val label: String) {
    HISTORIC("MODE HISTORIQUE"),
    WALK_FORWARD("MODE WALK-FORWARD")
}

enum class ScannerSortBy(val label: String) {
    CORRELATION("Corrélation"),
    AMPLIFICATION_UP("Amplif. UP"),
    AMPLIFICATION_DOWN("Amplif. DOWN"),
    OUTPERFORMANCE("Surperformance"),
    UNDERPERFORMANCE("Sous-perf."),
    RELATIVE_PERFORMANCE("Relative Perf."),
    OBSERVATIONS("Observations"),
    VOLATILITY("Volatilité")
}

enum class RegimeFilter(val label: String) {
    ALL("TOUS"),
    BTC_UP("BTC UP (> +1%)"),
    BTC_DOWN("BTC DOWN (< -1%)"),
    BTC_NEUTRAL("BTC NEUTRE (±1%)")
}

data class ScannerUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val progressMessage: String = "",
    val timeframe: Timeframe = Timeframe.H1,
    val lookbackRange: LookbackRange = LookbackRange.DAYS_90,
    val mode: AnalysisMode = AnalysisMode.HISTORIC,
    val btcCurrentReturn: Double = 0.0,
    val btcLastPrice: Double = 0.0,
    val btcRegime: PrimaryBtcRegime = PrimaryBtcRegime.NEUTRE,
    val totalAssetsAnalyzed: Int = 0,
    val totalObservations: Int = 0,
    val allResults: List<CryptoAnalysisResult> = emptyList(),
    val filteredResults: List<CryptoAnalysisResult> = emptyList(),
    val selectedResult: CryptoAnalysisResult? = null,
    val walkForwardReport: WalkForwardReport? = null,
    val comparedSymbols: Set<String> = setOf("SOL-USD", "ETH-USD", "XRP-USD", "ADA-USD"),
    val searchQuery: String = "",
    val regimeFilter: RegimeFilter = RegimeFilter.ALL,
    val reliabilityFilter: ReliabilityLevel? = null,
    val sortBy: ScannerSortBy = ScannerSortBy.CORRELATION,
    val sortAscending: Boolean = false,
    val reliabilityConfig: ReliabilityConfig = ReliabilityConfig(30, 100, 300),
    val totalCandlesInDb: Int = 0,
    val errorMessage: String? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = CryptoRepository(db)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var btcCandlesCache: List<CandleEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.initializeCatalog()
            loadMarketData()
        }
    }

    fun setTimeframe(timeframe: Timeframe) {
        if (_uiState.value.timeframe == timeframe) return
        _uiState.update { it.copy(timeframe = timeframe) }
        loadMarketData()
    }

    fun setLookbackRange(range: LookbackRange) {
        if (_uiState.value.lookbackRange == range) return
        _uiState.update { it.copy(lookbackRange = range) }
        loadMarketData()
    }

    fun setMode(mode: AnalysisMode) {
        _uiState.update { it.copy(mode = mode) }
        // If an asset is selected, recalculate walk-forward report if needed
        _uiState.value.selectedResult?.let {
            selectCrypto(it)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    fun setRegimeFilter(filter: RegimeFilter) {
        _uiState.update { it.copy(regimeFilter = filter) }
        applyFiltersAndSort()
    }

    fun setReliabilityFilter(filter: ReliabilityLevel?) {
        _uiState.update { it.copy(reliabilityFilter = filter) }
        applyFiltersAndSort()
    }

    fun setSortBy(sortBy: ScannerSortBy) {
        _uiState.update {
            if (it.sortBy == sortBy) {
                it.copy(sortAscending = !it.sortAscending)
            } else {
                it.copy(sortBy = sortBy, sortAscending = false)
            }
        }
        applyFiltersAndSort()
    }

    fun selectCrypto(result: CryptoAnalysisResult?) {
        _uiState.update {
            val wfReport = if (result != null && result.observations.size >= 20) {
                WalkForwardEngine.runWalkForward(result.observations)
            } else null
            it.copy(selectedResult = result, walkForwardReport = wfReport)
        }
    }

    fun toggleComparedSymbol(symbol: String) {
        _uiState.update { state ->
            val set = state.comparedSymbols.toMutableSet()
            if (set.contains(symbol)) {
                if (set.size > 1) set.remove(symbol)
            } else {
                set.add(symbol)
            }
            state.copy(comparedSymbols = set)
        }
    }

    fun updateReliabilityConfig(limited: Int, correct: Int, important: Int) {
        val config = ReliabilityConfig(limited, correct, important)
        _uiState.update { it.copy(reliabilityConfig = config) }
        loadMarketData()
    }

    fun refreshAll() {
        loadMarketData(forceRefresh = true)
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            loadMarketData(forceRefresh = true)
        }
    }

    fun loadMarketData(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, isScanning = true, errorMessage = null) }

            try {
                val currentTf = _uiState.value.timeframe
                val currentRange = _uiState.value.lookbackRange
                val config = _uiState.value.reliabilityConfig

                // 1. Fetch BTC Candles
                _uiState.update { it.copy(progressMessage = "Synchronisation Bitcoin (BTC)...") }
                val btcCandles = repository.getCandles(
                    symbol = CryptoRepository.BTC_SYMBOL,
                    timeframe = currentTf,
                    lookbackRange = currentRange,
                    forceRefresh = forceRefresh
                )
                btcCandlesCache = btcCandles

                val btcLatestPrice = btcCandles.lastOrNull()?.close ?: 0.0
                val btcLatestReturn = if (btcCandles.size >= 2) {
                    val last = btcCandles.last()
                    ((last.close / last.open) - 1.0) * 100.0
                } else 0.0
                val btcRegime = PrimaryBtcRegime.fromReturn(btcLatestReturn)

                // 2. Fetch Assets Catalog
                val assets = repository.getAllAssets().filter { it.symbol != CryptoRepository.BTC_SYMBOL }

                _uiState.update {
                    it.copy(
                        btcCurrentReturn = btcLatestReturn,
                        btcLastPrice = btcLatestPrice,
                        btcRegime = btcRegime,
                        progressMessage = "Analyse de ${assets.size} actifs..."
                    )
                }

                val results = mutableListOf<CryptoAnalysisResult>()
                var totalObsAcc = 0

                // Prioritize SOL first to guarantee instant validation as requested!
                val sortedAssets = assets.sortedByDescending { it.symbol == "SOL-USD" }

                for ((idx, asset) in sortedAssets.withIndex()) {
                    if (idx % 3 == 0) {
                        _uiState.update {
                            it.copy(progressMessage = "Calcul : ${asset.displayName} (${idx + 1}/${assets.size})")
                        }
                    }

                    val candles = repository.getCandles(
                        symbol = asset.symbol,
                        timeframe = currentTf,
                        lookbackRange = currentRange,
                        forceRefresh = forceRefresh
                    )

                    val observations = AnalysisEngine.computeObservations(btcCandles, candles)
                    totalObsAcc += observations.size

                    val analysis = AnalysisEngine.analyzeCrypto(
                        symbol = asset.symbol,
                        baseCurrency = asset.baseCurrency,
                        displayName = asset.displayName,
                        observations = observations,
                        config = config
                    )
                    results.add(analysis)
                }

                val dbCandleCount = repository.getTotalCandlesStored()

                _uiState.update { state ->
                    val updatedSelected = results.find { it.symbol == state.selectedResult?.symbol }
                        ?: results.find { it.symbol == "SOL-USD" }

                    val wfReport = if (updatedSelected != null && updatedSelected.observations.size >= 20) {
                        WalkForwardEngine.runWalkForward(updatedSelected.observations)
                    } else null

                    state.copy(
                        isLoading = false,
                        isScanning = false,
                        progressMessage = "",
                        allResults = results,
                        totalAssetsAnalyzed = results.size,
                        totalObservations = totalObsAcc,
                        selectedResult = updatedSelected,
                        walkForwardReport = wfReport,
                        totalCandlesInDb = dbCandleCount
                    )
                }

                applyFiltersAndSort()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isScanning = false,
                        errorMessage = "Erreur de chargement des données Coinbase : ${e.localizedMessage ?: "Inconnue"}"
                    )
                }
            }
        }
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var list = state.allResults

        // Search query
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            list = list.filter {
                it.symbol.lowercase().contains(q) ||
                        it.baseCurrency.lowercase().contains(q) ||
                        it.displayName.lowercase().contains(q)
            }
        }

        // Reliability filter
        state.reliabilityFilter?.let { minLevel ->
            list = list.filter { it.reliability.ordinal >= minLevel.ordinal }
        }

        // Regime Filter
        list = when (state.regimeFilter) {
            RegimeFilter.ALL -> list
            RegimeFilter.BTC_UP -> list.filter { it.upRegimeStats.count > 0 }
            RegimeFilter.BTC_DOWN -> list.filter { it.downRegimeStats.count > 0 }
            RegimeFilter.BTC_NEUTRAL -> list.filter { it.neutralRegimeStats.count > 0 }
        }

        // Sort
        list = when (state.sortBy) {
            ScannerSortBy.CORRELATION -> if (state.sortAscending) list.sortedBy { it.pearsonCorrelation } else list.sortedByDescending { it.pearsonCorrelation }
            ScannerSortBy.AMPLIFICATION_UP -> if (state.sortAscending) list.sortedBy { it.amplificationUp } else list.sortedByDescending { it.amplificationUp }
            ScannerSortBy.AMPLIFICATION_DOWN -> if (state.sortAscending) list.sortedBy { it.amplificationDown } else list.sortedByDescending { it.amplificationDown }
            ScannerSortBy.OUTPERFORMANCE -> if (state.sortAscending) list.sortedBy { it.overallOutperformancePct } else list.sortedByDescending { it.overallOutperformancePct }
            ScannerSortBy.UNDERPERFORMANCE -> if (state.sortAscending) list.sortedBy { it.overallUnderperformancePct } else list.sortedByDescending { it.overallUnderperformancePct }
            ScannerSortBy.RELATIVE_PERFORMANCE -> if (state.sortAscending) list.sortedBy { it.overallRelativeMean } else list.sortedByDescending { it.overallRelativeMean }
            ScannerSortBy.OBSERVATIONS -> if (state.sortAscending) list.sortedBy { it.totalObservations } else list.sortedByDescending { it.totalObservations }
            ScannerSortBy.VOLATILITY -> if (state.sortAscending) list.sortedBy { it.volatility } else list.sortedByDescending { it.volatility }
        }

        _uiState.update { it.copy(filteredResults = list) }
    }
}
