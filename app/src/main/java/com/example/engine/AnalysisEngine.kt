package com.example.engine

import com.example.data.local.CandleEntity
import com.example.regimes.DetailedBtcRegime
import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.CryptoAnalysisResult
import com.example.statistics.MathUtils
import com.example.statistics.RegimeStatistics
import com.example.statistics.ReliabilityLevel
import com.example.statistics.ReturnObservation
import kotlin.math.abs

data class ReliabilityConfig(
    val thresholdLimited: Int = 30,
    val thresholdCorrect: Int = 100,
    val thresholdImportant: Int = 300
)

object AnalysisEngine {

    /**
     * Aligns BTC candles and Altcoin candles by timestamp and computes return observations.
     * cryptoReturn = (prix_fin_crypto / prix_début_crypto - 1) * 100
     * btcReturn = (prix_fin_BTC / prix_début_BTC - 1) * 100
     */
    fun computeObservations(
        btcCandles: List<CandleEntity>,
        cryptoCandles: List<CandleEntity>
    ): List<ReturnObservation> {
        val btcMap = btcCandles.associateBy { it.timestamp }
        val observations = mutableListOf<ReturnObservation>()

        for (c in cryptoCandles) {
            val btc = btcMap[c.timestamp] ?: continue
            if (btc.open <= 0.0 || c.open <= 0.0) continue

            val btcRet = ((btc.close / btc.open) - 1.0) * 100.0
            val cryptoRet = ((c.close / c.open) - 1.0) * 100.0

            observations.add(
                ReturnObservation(
                    timestamp = c.timestamp,
                    btcReturn = btcRet,
                    cryptoReturn = cryptoRet,
                    relativePerformance = cryptoRet - btcRet
                )
            )
        }

        return observations.sortedBy { it.timestamp }
    }

    /**
     * Computes complete statistical analysis for a cryptocurrency relative to Bitcoin.
     */
    fun analyzeCrypto(
        symbol: String,
        baseCurrency: String,
        displayName: String,
        observations: List<ReturnObservation>,
        config: ReliabilityConfig = ReliabilityConfig()
    ): CryptoAnalysisResult {
        val total = observations.size
        val overallReliability = ReliabilityLevel.fromCount(
            total,
            config.thresholdLimited,
            config.thresholdCorrect,
            config.thresholdImportant
        )

        if (total == 0) {
            val emptyStats = createEmptyRegimeStats("Vide", overallReliability)
            val detailedEmpty = DetailedBtcRegime.values().associateWith {
                createEmptyRegimeStats(it.label, overallReliability)
            }
            return CryptoAnalysisResult(
                symbol = symbol,
                baseCurrency = baseCurrency,
                displayName = displayName,
                totalObservations = 0,
                pearsonCorrelation = 0.0,
                rSquared = 0.0,
                regressionSlope = 0.0,
                regressionIntercept = 0.0,
                volatility = 0.0,
                reliability = overallReliability,
                overallMeanReturn = 0.0,
                overallMedianReturn = 0.0,
                overallRelativeMean = 0.0,
                overallSameDirectionPct = 0.0,
                overallOutperformancePct = 0.0,
                overallUnderperformancePct = 0.0,
                upRegimeStats = emptyStats,
                downRegimeStats = emptyStats,
                neutralRegimeStats = emptyStats,
                detailedRegimeStats = detailedEmpty,
                observations = emptyList()
            )
        }

        val btcReturns = observations.map { it.btcReturn }
        val cryptoReturns = observations.map { it.cryptoReturn }
        val relativeReturns = observations.map { it.relativePerformance }

        // Pearson correlation & Linear regression
        val regression = MathUtils.linearRegression(btcReturns, cryptoReturns)
        val volatility = MathUtils.standardDeviation(cryptoReturns)

        val overallMeanReturn = MathUtils.mean(cryptoReturns)
        val overallMedianReturn = MathUtils.median(cryptoReturns)
        val overallRelativeMean = MathUtils.mean(relativeReturns)

        // Direction & Performance
        var sameDirCount = 0
        var outperfCount = 0
        var underperfCount = 0

        for (obs in observations) {
            if ((obs.btcReturn > 0 && obs.cryptoReturn > 0) ||
                (obs.btcReturn < 0 && obs.cryptoReturn < 0) ||
                (obs.btcReturn == 0.0 && obs.cryptoReturn == 0.0)
            ) {
                sameDirCount++
            }
            if (obs.cryptoReturn > obs.btcReturn) {
                outperfCount++
            } else if (obs.cryptoReturn < obs.btcReturn) {
                underperfCount++
            }
        }

        val overallSameDirectionPct = (sameDirCount.toDouble() / total) * 100.0
        val overallOutperformancePct = (outperfCount.toDouble() / total) * 100.0
        val overallUnderperformancePct = (underperfCount.toDouble() / total) * 100.0

        // Primary Regimes
        val upObs = observations.filter { it.primaryRegime == PrimaryBtcRegime.HAUSSE }
        val downObs = observations.filter { it.primaryRegime == PrimaryBtcRegime.BAISSE }
        val neutralObs = observations.filter { it.primaryRegime == PrimaryBtcRegime.NEUTRE }

        val upStats = computeRegimeStatistics(
            PrimaryBtcRegime.HAUSSE.label,
            upObs,
            config
        )
        val downStats = computeRegimeStatistics(
            PrimaryBtcRegime.BAISSE.label,
            downObs,
            config
        )
        val neutralStats = computeRegimeStatistics(
            PrimaryBtcRegime.NEUTRE.label,
            neutralObs,
            config
        )

        // Detailed Sub-categories
        val detailedMap = DetailedBtcRegime.values().associateWith { regime ->
            val bucketObs = observations.filter { it.detailedRegime == regime }
            computeRegimeStatistics(regime.label, bucketObs, config)
        }

        return CryptoAnalysisResult(
            symbol = symbol,
            baseCurrency = baseCurrency,
            displayName = displayName,
            totalObservations = total,
            pearsonCorrelation = regression.correlation,
            rSquared = regression.rSquared,
            regressionSlope = regression.slope,
            regressionIntercept = regression.intercept,
            volatility = volatility,
            reliability = overallReliability,
            overallMeanReturn = overallMeanReturn,
            overallMedianReturn = overallMedianReturn,
            overallRelativeMean = overallRelativeMean,
            overallSameDirectionPct = overallSameDirectionPct,
            overallOutperformancePct = overallOutperformancePct,
            overallUnderperformancePct = overallUnderperformancePct,
            upRegimeStats = upStats,
            downRegimeStats = downStats,
            neutralRegimeStats = neutralStats,
            detailedRegimeStats = detailedMap,
            observations = observations
        )
    }

    private fun computeRegimeStatistics(
        label: String,
        obsList: List<ReturnObservation>,
        config: ReliabilityConfig
    ): RegimeStatistics {
        val count = obsList.size
        val reliability = ReliabilityLevel.fromCount(
            count,
            config.thresholdLimited,
            config.thresholdCorrect,
            config.thresholdImportant
        )

        if (count == 0) {
            return createEmptyRegimeStats(label, reliability)
        }

        val btcReturns = obsList.map { it.btcReturn }
        val cryptoReturns = obsList.map { it.cryptoReturn }
        val relativeReturns = obsList.map { it.relativePerformance }

        val btcMean = MathUtils.mean(btcReturns)
        val cryptoMean = MathUtils.mean(cryptoReturns)
        val cryptoMedian = MathUtils.median(cryptoReturns)
        val relMean = MathUtils.mean(relativeReturns)
        val relMedian = MathUtils.median(relativeReturns)
        val volatility = MathUtils.standardDeviation(cryptoReturns)

        var sameDir = 0
        var outperf = 0
        var underperf = 0

        for (obs in obsList) {
            if ((obs.btcReturn > 0 && obs.cryptoReturn > 0) ||
                (obs.btcReturn < 0 && obs.cryptoReturn < 0) ||
                (obs.btcReturn == 0.0 && obs.cryptoReturn == 0.0)
            ) {
                sameDir++
            }
            if (obs.cryptoReturn > obs.btcReturn) {
                outperf++
            } else if (obs.cryptoReturn < obs.btcReturn) {
                underperf++
            }
        }

        val sameDirPct = (sameDir.toDouble() / count) * 100.0
        val outperfPct = (outperf.toDouble() / count) * 100.0
        val underperfPct = (underperf.toDouble() / count) * 100.0

        // Amplification = cryptoMean / btcMean
        val amplification = if (abs(btcMean) > 0.0001) cryptoMean / btcMean else 1.0

        return RegimeStatistics(
            regimeName = label,
            count = count,
            btcMeanReturn = btcMean,
            cryptoMeanReturn = cryptoMean,
            cryptoMedianReturn = cryptoMedian,
            relativePerformanceMean = relMean,
            relativePerformanceMedian = relMedian,
            sameDirectionPct = sameDirPct,
            outperformancePct = outperfPct,
            underperformancePct = underperfPct,
            volatility = volatility,
            amplification = amplification,
            reliability = reliability
        )
    }

    private fun createEmptyRegimeStats(label: String, reliability: ReliabilityLevel): RegimeStatistics {
        return RegimeStatistics(
            regimeName = label,
            count = 0,
            btcMeanReturn = 0.0,
            cryptoMeanReturn = 0.0,
            cryptoMedianReturn = 0.0,
            relativePerformanceMean = 0.0,
            relativePerformanceMedian = 0.0,
            sameDirectionPct = 0.0,
            outperformancePct = 0.0,
            underperformancePct = 0.0,
            volatility = 0.0,
            amplification = 0.0,
            reliability = reliability
        )
    }
}
