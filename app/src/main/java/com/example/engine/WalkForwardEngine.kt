package com.example.engine

import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.MathUtils
import com.example.statistics.ReturnObservation
import kotlin.math.abs

data class WalkForwardStep(
    val timestamp: Long,
    val btcReturn: Double,
    val actualCryptoReturn: Double,
    val expectedCryptoReturn: Double,
    val historicalCorrelation: Double,
    val historicalAmplification: Double,
    val directionalHit: Boolean,
    val outperformanceHit: Boolean,
    val absoluteError: Double
)

data class WalkForwardReport(
    val totalSteps: Int,
    val directionalHitRatePct: Double,
    val outperformanceHitRatePct: Double,
    val meanAbsoluteError: Double,
    val averageHistoricalCorrelation: Double,
    val exploitationScorePct: Double,
    val steps: List<WalkForwardStep>
)

object WalkForwardEngine {

    /**
     * Executes strict walk-forward evaluation without lookahead bias.
     * At index i (time T):
     * 1. Uses ONLY observations [0 until i] to compute historical statistics.
     * 2. Evaluates the incoming observation at index i.
     * 3. Records prediction success.
     */
    fun runWalkForward(
        observations: List<ReturnObservation>,
        warmupCount: Int = 20
    ): WalkForwardReport {
        if (observations.size <= warmupCount) {
            return WalkForwardReport(
                totalSteps = 0,
                directionalHitRatePct = 0.0,
                outperformanceHitRatePct = 0.0,
                meanAbsoluteError = 0.0,
                averageHistoricalCorrelation = 0.0,
                exploitationScorePct = 0.0,
                steps = emptyList()
            )
        }

        val steps = mutableListOf<WalkForwardStep>()
        var directionalHits = 0
        var outperformanceHits = 0
        var totalAbsError = 0.0
        var sumCorrelation = 0.0

        for (i in warmupCount until observations.size) {
            val history = observations.subList(0, i)
            val current = observations[i]

            val histBtcReturns = history.map { it.btcReturn }
            val histCryptoReturns = history.map { it.cryptoReturn }

            val correlation = MathUtils.pearsonCorrelation(histBtcReturns, histCryptoReturns)
            sumCorrelation += correlation

            // Check regime of incoming BTC move
            val regime = PrimaryBtcRegime.fromReturn(current.btcReturn)
            val matchingHistory = history.filter { it.primaryRegime == regime }

            val histAmplification = if (matchingHistory.isNotEmpty()) {
                val meanCrypto = matchingHistory.map { it.cryptoReturn }.average()
                val meanBtc = matchingHistory.map { it.btcReturn }.average()
                if (abs(meanBtc) > 0.0001) meanCrypto / meanBtc else 1.0
            } else {
                1.0
            }

            // Expected return based purely on prior regime amplification
            val expectedReturn = current.btcReturn * histAmplification

            val directionalHit = (current.cryptoReturn > 0 && expectedReturn > 0) ||
                    (current.cryptoReturn < 0 && expectedReturn < 0) ||
                    (abs(current.cryptoReturn) < 0.1 && abs(expectedReturn) < 0.1)

            val expectedOutperformance = histAmplification > 1.0 && current.btcReturn > 0 ||
                    (histAmplification < 1.0 && current.btcReturn < 0)
            val actualOutperformance = current.cryptoReturn > current.btcReturn
            val outperformanceHit = (expectedOutperformance == actualOutperformance)

            val error = abs(current.cryptoReturn - expectedReturn)
            totalAbsError += error

            if (directionalHit) directionalHits++
            if (outperformanceHit) outperformanceHits++

            steps.add(
                WalkForwardStep(
                    timestamp = current.timestamp,
                    btcReturn = current.btcReturn,
                    actualCryptoReturn = current.cryptoReturn,
                    expectedCryptoReturn = expectedReturn,
                    historicalCorrelation = correlation,
                    historicalAmplification = histAmplification,
                    directionalHit = directionalHit,
                    outperformanceHit = outperformanceHit,
                    absoluteError = error
                )
            )
        }

        val stepCount = steps.size
        val dirHitRate = if (stepCount > 0) (directionalHits.toDouble() / stepCount) * 100.0 else 0.0
        val outperfHitRate = if (stepCount > 0) (outperformanceHits.toDouble() / stepCount) * 100.0 else 0.0
        val mae = if (stepCount > 0) totalAbsError / stepCount else 0.0
        val avgCorr = if (stepCount > 0) sumCorrelation / stepCount else 0.0
        val exploitationScore = (dirHitRate * 0.6 + outperfHitRate * 0.4)

        return WalkForwardReport(
            totalSteps = stepCount,
            directionalHitRatePct = dirHitRate,
            outperformanceHitRatePct = outperfHitRate,
            meanAbsoluteError = mae,
            averageHistoricalCorrelation = avgCorr,
            exploitationScorePct = exploitationScore,
            steps = steps
        )
    }
}
