package com.example.statistics

import kotlin.math.abs
import kotlin.math.sqrt

object MathUtils {

    fun mean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.average()
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2]
        }
    }

    fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val avg = values.average()
        val variance = values.map { (it - avg) * (it - avg) }.average()
        return sqrt(variance)
    }

    /**
     * Pearson Correlation Coefficient r between X (e.g. BTC) and Y (e.g. Crypto)
     * r = Σ((x - x̄)(y - ȳ)) / sqrt(Σ(x - x̄)² * Σ(y - ȳ)²)
     */
    fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 2) return 0.0
        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var sumCov = 0.0
        var sumVarX = 0.0
        var sumVarY = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            sumCov += dx * dy
            sumVarX += dx * dx
            sumVarY += dy * dy
        }

        val denominator = sqrt(sumVarX * sumVarY)
        if (denominator == 0.0 || denominator.isNaN()) return 0.0
        val r = sumCov / denominator
        return r.coerceIn(-1.0, 1.0)
    }

    data class LinearRegressionResult(
        val slope: Double,      // Beta (β)
        val intercept: Double,  // Alpha (α)
        val rSquared: Double,   // R²
        val correlation: Double // Pearson r
    )

    /**
     * Computes linear regression Y = α + β * X
     * β = Cov(X, Y) / Var(X)
     * α = meanY - β * meanX
     * Note: Slope β is clearly distinguished from the simple mean return amplification ratio!
     */
    fun linearRegression(x: List<Double>, y: List<Double>): LinearRegressionResult {
        if (x.size != y.size || x.size < 2) {
            return LinearRegressionResult(0.0, 0.0, 0.0, 0.0)
        }
        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var sumCov = 0.0
        var sumVarX = 0.0
        var sumVarY = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            sumCov += dx * dy
            sumVarX += dx * dx
            sumVarY += dy * dy
        }

        val slope = if (sumVarX > 0.0) sumCov / sumVarX else 0.0
        val intercept = meanY - slope * meanX

        val denom = sqrt(sumVarX * sumVarY)
        val r = if (denom > 0.0) (sumCov / denom).coerceIn(-1.0, 1.0) else 0.0
        val rSquared = r * r

        return LinearRegressionResult(
            slope = slope,
            intercept = intercept,
            rSquared = rSquared,
            correlation = r
        )
    }

    /**
     * Amplification coefficient = cryptoMean / btcMean
     */
    fun amplification(cryptoMean: Double, btcMean: Double): Double {
        if (abs(btcMean) < 0.0001) return 1.0
        return cryptoMean / btcMean
    }
}
