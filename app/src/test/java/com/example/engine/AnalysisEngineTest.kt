package com.example.engine

import com.example.data.local.CandleEntity
import com.example.regimes.DetailedBtcRegime
import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.MathUtils
import com.example.statistics.ReliabilityLevel
import com.example.statistics.ReturnObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AnalysisEngineTest {

    @Test
    fun testMathematicalPrinciplesExamplesFromBrief() {
        // Example 1 from prompt:
        // BTC = -10%, SOL = -20% -> relativePerformance = -20 - (-10) = -10 points
        val btcRet1 = -10.0
        val solRet1 = -20.0
        val relPerf1 = solRet1 - btcRet1
        assertEquals(-10.0, relPerf1, 0.001)

        // Example 2 from prompt:
        // BTC = +10%, SOL = +25% -> relativePerformance = +25 - (+10) = +15 points
        val btcRet2 = 10.0
        val solRet2 = 25.0
        val relPerf2 = solRet2 - btcRet2
        assertEquals(15.0, relPerf2, 0.001)
    }

    @Test
    fun testPearsonCorrelationAndLinearRegression() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(2.0, 4.0, 6.0, 8.0, 10.0)

        val corr = MathUtils.pearsonCorrelation(x, y)
        assertEquals(1.0, corr, 0.001)

        val reg = MathUtils.linearRegression(x, y)
        assertEquals(2.0, reg.slope, 0.001) // Beta = 2.0
        assertEquals(0.0, reg.intercept, 0.001) // Alpha = 0.0
        assertEquals(1.0, reg.rSquared, 0.001) // R² = 1.0
    }

    @Test
    fun testRegressionSlopeDistinctFromAmplificationRatio() {
        // When intercept != 0, slope β != mean(Y) / mean(X)
        val btcReturns = listOf(1.0, 2.0, 3.0, 4.0)
        val cryptoReturns = listOf(5.0, 7.0, 9.0, 11.0) // Y = 3 + 2*X

        val reg = MathUtils.linearRegression(btcReturns, cryptoReturns)
        val btcMean = btcReturns.average() // 2.5
        val cryptoMean = cryptoReturns.average() // 8.0
        val simpleRatio = cryptoMean / btcMean // 3.2

        assertEquals(2.0, reg.slope, 0.001) // β is 2.0
        assertEquals(3.2, simpleRatio, 0.001) // simple amplification is 3.2
        assertNotEquals(reg.slope, simpleRatio, 0.01)
    }

    @Test
    fun testPrimaryRegimesClassification() {
        assertEquals(PrimaryBtcRegime.HAUSSE, PrimaryBtcRegime.fromReturn(1.01))
        assertEquals(PrimaryBtcRegime.HAUSSE, PrimaryBtcRegime.fromReturn(4.5))

        assertEquals(PrimaryBtcRegime.BAISSE, PrimaryBtcRegime.fromReturn(-1.01))
        assertEquals(PrimaryBtcRegime.BAISSE, PrimaryBtcRegime.fromReturn(-3.8))

        assertEquals(PrimaryBtcRegime.NEUTRE, PrimaryBtcRegime.fromReturn(0.5))
        assertEquals(PrimaryBtcRegime.NEUTRE, PrimaryBtcRegime.fromReturn(-0.8))
        assertEquals(PrimaryBtcRegime.NEUTRE, PrimaryBtcRegime.fromReturn(1.0))
        assertEquals(PrimaryBtcRegime.NEUTRE, PrimaryBtcRegime.fromReturn(-1.0))
    }

    @Test
    fun testDetailedRegimesClassification() {
        assertEquals(DetailedBtcRegime.UP_SUPER, DetailedBtcRegime.fromReturn(6.2))
        assertEquals(DetailedBtcRegime.UP_HIGH, DetailedBtcRegime.fromReturn(3.1))
        assertEquals(DetailedBtcRegime.UP_MODERATE, DetailedBtcRegime.fromReturn(1.5))
        assertEquals(DetailedBtcRegime.NEUTRAL, DetailedBtcRegime.fromReturn(0.2))
        assertEquals(DetailedBtcRegime.DOWN_MODERATE, DetailedBtcRegime.fromReturn(-1.4))
        assertEquals(DetailedBtcRegime.DOWN_HIGH, DetailedBtcRegime.fromReturn(-3.5))
        assertEquals(DetailedBtcRegime.DOWN_SUPER, DetailedBtcRegime.fromReturn(-6.0))
    }

    @Test
    fun testObservationsCalculationFromCandles() {
        val btcCandles = listOf(
            CandleEntity("BTC-USD", "1h", 1000L, 50000.0, 51000.0, 49000.0, 51000.0, 100.0), // +2%
            CandleEntity("BTC-USD", "1h", 2000L, 51000.0, 52000.0, 49500.0, 49980.0, 120.0)  // -2%
        )

        val solCandles = listOf(
            CandleEntity("SOL-USD", "1h", 1000L, 100.0, 106.0, 99.0, 104.0, 500.0),  // +4%
            CandleEntity("SOL-USD", "1h", 2000L, 104.0, 105.0, 98.0, 100.88, 600.0)  // -3%
        )

        val observations = AnalysisEngine.computeObservations(btcCandles, solCandles)
        assertEquals(2, observations.size)

        // Observation 1: BTC +2%, SOL +4% -> relativePerformance = +2 points
        assertEquals(2.0, observations[0].btcReturn, 0.01)
        assertEquals(4.0, observations[0].cryptoReturn, 0.01)
        assertEquals(2.0, observations[0].relativePerformance, 0.01)

        // Observation 2: BTC -2%, SOL -3% -> relativePerformance = -1 points
        assertEquals(-2.0, observations[1].btcReturn, 0.01)
        assertEquals(-3.0, observations[1].cryptoReturn, 0.01)
        assertEquals(-1.0, observations[1].relativePerformance, 0.01)
    }

    @Test
    fun testStatisticalReliabilityThresholds() {
        assertEquals(ReliabilityLevel.FAIBLE, ReliabilityLevel.fromCount(15))
        assertEquals(ReliabilityLevel.LIMITE, ReliabilityLevel.fromCount(45))
        assertEquals(ReliabilityLevel.CORRECT, ReliabilityLevel.fromCount(150))
        assertEquals(ReliabilityLevel.IMPORTANT, ReliabilityLevel.fromCount(450))
    }

    @Test
    fun testWalkForwardZeroLookahead() {
        val observations = mutableListOf<ReturnObservation>()
        for (i in 0 until 50) {
            val btcRet = if (i % 2 == 0) 2.0 else -2.0
            val cryptoRet = btcRet * 1.5 // 1.5x amplification
            observations.add(
                ReturnObservation(
                    timestamp = 1000L + i * 3600L,
                    btcReturn = btcRet,
                    cryptoReturn = cryptoRet,
                    relativePerformance = cryptoRet - btcRet
                )
            )
        }

        val report = WalkForwardEngine.runWalkForward(observations, warmupCount = 20)
        assertEquals(30, report.totalSteps)
        assertTrue(report.directionalHitRatePct > 90.0)
    }
}
