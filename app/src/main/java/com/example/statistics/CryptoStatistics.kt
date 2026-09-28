package com.example.statistics

import com.example.regimes.DetailedBtcRegime
import com.example.regimes.PrimaryBtcRegime

data class ReturnObservation(
    val timestamp: Long,
    val btcReturn: Double,
    val cryptoReturn: Double,
    val relativePerformance: Double = cryptoReturn - btcReturn
) {
    val primaryRegime: PrimaryBtcRegime get() = PrimaryBtcRegime.fromReturn(btcReturn)
    val detailedRegime: DetailedBtcRegime get() = DetailedBtcRegime.fromReturn(btcReturn)
}

enum class ReliabilityLevel(val label: String, val shortLabel: String) {
    FAIBLE("ÉCHANTILLON FAIBLE", "FAIBLE"),
    LIMITE("ÉCHANTILLON LIMITÉ", "LIMITÉ"),
    CORRECT("ÉCHANTILLON CORRECT", "CORRECT"),
    IMPORTANT("ÉCHANTILLON IMPORTANT", "IMPORTANT");

    companion object {
        fun fromCount(
            count: Int,
            thresholdLimited: Int = 30,
            thresholdCorrect: Int = 100,
            thresholdImportant: Int = 300
        ): ReliabilityLevel {
            return when {
                count < thresholdLimited -> FAIBLE
                count < thresholdCorrect -> LIMITE
                count < thresholdImportant -> CORRECT
                else -> IMPORTANT
            }
        }
    }
}

data class RegimeStatistics(
    val regimeName: String,
    val count: Int,
    val btcMeanReturn: Double,
    val cryptoMeanReturn: Double,
    val cryptoMedianReturn: Double,
    val relativePerformanceMean: Double,
    val relativePerformanceMedian: Double,
    val sameDirectionPct: Double,
    val outperformancePct: Double,
    val underperformancePct: Double,
    val volatility: Double,
    val amplification: Double,
    val reliability: ReliabilityLevel
)

data class CryptoAnalysisResult(
    val symbol: String,
    val baseCurrency: String,
    val displayName: String,
    val totalObservations: Int,
    val pearsonCorrelation: Double,
    val rSquared: Double,
    val regressionSlope: Double, // β distinct from amplification
    val regressionIntercept: Double,
    val volatility: Double,
    val reliability: ReliabilityLevel,
    val overallMeanReturn: Double,
    val overallMedianReturn: Double,
    val overallRelativeMean: Double,
    val overallSameDirectionPct: Double,
    val overallOutperformancePct: Double,
    val overallUnderperformancePct: Double,
    val upRegimeStats: RegimeStatistics,
    val downRegimeStats: RegimeStatistics,
    val neutralRegimeStats: RegimeStatistics,
    val detailedRegimeStats: Map<DetailedBtcRegime, RegimeStatistics>,
    val observations: List<ReturnObservation>
) {
    val amplificationUp: Double get() = upRegimeStats.amplification
    val amplificationDown: Double get() = downRegimeStats.amplification
    val outperformanceOverall: Double get() = overallOutperformancePct
    val relativePerformanceOverall: Double get() = overallRelativeMean
}
