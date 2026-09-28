package com.example.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.MathUtils
import com.example.statistics.ReturnObservation
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BtcGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeutralAmber
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ScatterPlotChart(
    observations: List<ReturnObservation>,
    regression: MathUtils.LinearRegressionResult,
    cryptoSymbol: String,
    modifier: Modifier = Modifier
) {
    if (observations.isEmpty()) {
        Box(
            modifier = modifier
                .background(TerminalSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Données insuffisantes pour le graphique", color = TextMuted)
        }
        return
    }

    var selectedPoint by remember { mutableStateOf<ReturnObservation?>(null) }

    // Dynamic range calculation with padding
    val maxAbsBtc = observations.maxOfOrNull { abs(it.btcReturn) }?.coerceAtLeast(4.0) ?: 5.0
    val maxAbsCrypto = observations.maxOfOrNull { abs(it.cryptoReturn) }?.coerceAtLeast(4.0) ?: 5.0
    val maxRange = max(maxAbsBtc, maxAbsCrypto) * 1.15

    val minX = -maxRange
    val maxX = maxRange
    val minY = -maxRange
    val maxY = maxRange

    Column(
        modifier = modifier
            .background(TerminalSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        // Chart Header with Regression equation and coefficients
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DISTRIBUTION BTC / $cryptoSymbol",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "X = Rendement BTC (%)  |  Y = Rendement $cryptoSymbol (%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Regression metrics chip
            Surface(
                color = Color(0xFF0F1523),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R²: ",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", regression.rSquared),
                        color = BtcGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Corr: ",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", regression.correlation),
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pente β: ",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", regression.slope),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(observations) {
                        detectTapGestures { tapOffset ->
                            val width = size.width
                            val height = size.height

                            var closest: ReturnObservation? = null
                            var minDist = Float.MAX_VALUE

                            for (obs in observations) {
                                val px = ((obs.btcReturn - minX) / (maxX - minX) * width).toFloat()
                                val py = ((maxY - obs.cryptoReturn) / (maxY - minY) * height).toFloat()
                                val dist = (tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py)
                                if (dist < minDist && dist < 1200f) {
                                    minDist = dist
                                    closest = obs
                                }
                            }
                            selectedPoint = closest
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                fun mapX(x: Double): Float = ((x - minX) / (maxX - minX) * w).toFloat()
                fun mapY(y: Double): Float = ((maxY - y) / (maxY - minY) * h).toFloat()

                // Draw subtle grid & axes
                val zeroX = mapX(0.0)
                val zeroY = mapY(0.0)

                val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Grid lines at intervals
                val step = when {
                    maxRange > 20 -> 10.0
                    maxRange > 10 -> 5.0
                    else -> 2.0
                }

                var currentStep = step
                while (currentStep < maxRange) {
                    val posPx = mapX(currentStep)
                    val negPx = mapX(-currentStep)
                    val posPy = mapY(currentStep)
                    val negPy = mapY(-currentStep)

                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(posPx, 0f),
                        end = Offset(posPx, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(negPx, 0f),
                        end = Offset(negPx, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(0f, posPy),
                        end = Offset(w, posPy),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(0f, negPy),
                        end = Offset(w, negPy),
                        strokeWidth = 1f
                    )
                    currentStep += step
                }

                // X=0 and Y=0 Axes
                drawLine(
                    color = Color(0x66FFFFFF),
                    start = Offset(zeroX, 0f),
                    end = Offset(zeroX, h),
                    strokeWidth = 1.5f,
                    pathEffect = dashedEffect
                )
                drawLine(
                    color = Color(0x66FFFFFF),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.5f,
                    pathEffect = dashedEffect
                )

                // Regression Line: Y = intercept + slope * X
                val regY1 = regression.intercept + regression.slope * minX
                val regY2 = regression.intercept + regression.slope * maxX

                drawLine(
                    color = CyanAccent,
                    start = Offset(mapX(minX), mapY(regY1)),
                    end = Offset(mapX(maxX), mapY(regY2)),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Draw Points
                for (obs in observations) {
                    val px = mapX(obs.btcReturn)
                    val py = mapY(obs.cryptoReturn)

                    val color = when (obs.primaryRegime) {
                        PrimaryBtcRegime.HAUSSE -> BullishGreen
                        PrimaryBtcRegime.BAISSE -> BearishRed
                        PrimaryBtcRegime.NEUTRE -> NeutralAmber
                    }

                    drawCircle(
                        color = color.copy(alpha = 0.65f),
                        radius = 4f,
                        center = Offset(px, py)
                    )
                }

                // Highlight selected point if any
                selectedPoint?.let { sel ->
                    val spx = mapX(sel.btcReturn)
                    val spy = mapY(sel.cryptoReturn)

                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = Offset(spx, spy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = BtcGold,
                        radius = 5f,
                        center = Offset(spx, spy)
                    )
                }
            }

            // Tooltip overlay if a point is tapped
            selectedPoint?.let { pt ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(Date(pt.timestamp * 1000))
                Surface(
                    color = Color(0xF00A0E17),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = dateStr,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BTC: ${if (pt.btcReturn >= 0) "+" else ""}${String.format(Locale.US, "%.2f", pt.btcReturn)}%",
                            color = if (pt.btcReturn >= 0) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$cryptoSymbol: ${if (pt.cryptoReturn >= 0) "+" else ""}${String.format(Locale.US, "%.2f", pt.cryptoReturn)}%",
                            color = if (pt.cryptoReturn >= 0) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Spread: ${if (pt.relativePerformance >= 0) "+" else ""}${String.format(Locale.US, "%.2f", pt.relativePerformance)} pts",
                            color = if (pt.relativePerformance >= 0) CyanAccent else NeutralAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-${String.format(Locale.US, "%.1f", maxRange)}%",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "BTC 0%",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "+${String.format(Locale.US, "%.1f", maxRange)}%",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
