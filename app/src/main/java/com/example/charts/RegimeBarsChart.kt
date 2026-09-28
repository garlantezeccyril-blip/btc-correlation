package com.example.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statistics.RegimeStatistics
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BtcGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeutralAmber
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun RegimeBarsChart(
    regimes: List<Pair<String, RegimeStatistics>>,
    cryptoSymbol: String,
    modifier: Modifier = Modifier
) {
    val maxVal = regimes.maxOfOrNull {
        max(abs(it.second.btcMeanReturn), abs(it.second.cryptoMeanReturn))
    }?.coerceAtLeast(3.0) ?: 5.0

    Column(
        modifier = modifier
            .background(TerminalSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RÉACTIONS MOYENNES PAR RÉGIME",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            // Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(BtcGold, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("BTC", fontSize = 10.sp, color = TextSecondary)

                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(CyanAccent, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(cryptoSymbol, fontSize = 10.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        regimes.forEach { (label, stat) ->
            if (stat.count > 0) {
                RegimeBarRow(
                    label = label,
                    btcMean = stat.btcMeanReturn,
                    cryptoMean = stat.cryptoMeanReturn,
                    count = stat.count,
                    maxVal = maxVal
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RegimeBarRow(
    label: String,
    btcMean: Double,
    cryptoMean: Double,
    count: Int,
    maxVal: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.width(110.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "N=$count",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Dual bar row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left (Negative) area / Right (Positive) area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .background(Color(0xFF0F1420), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Center line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Render BTC Bar
                val btcFraction = (abs(btcMean) / maxVal).coerceIn(0.0, 1.0).toFloat()
                val cryptoFraction = (abs(cryptoMean) / maxVal).coerceIn(0.0, 1.0).toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (btcMean < 0) {
                        Spacer(modifier = Modifier.weight(1f - btcFraction))
                        Box(
                            modifier = Modifier
                                .weight(btcFraction)
                                .height(5.dp)
                                .background(BtcGold.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Positive BTC
                    if (btcMean >= 0) {
                        Box(
                            modifier = Modifier
                                .weight(btcFraction)
                                .height(5.dp)
                                .background(BtcGold.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.weight(1f - btcFraction))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Render Crypto Bar overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cryptoMean < 0) {
                        Spacer(modifier = Modifier.weight(1f - cryptoFraction))
                        Box(
                            modifier = Modifier
                                .weight(cryptoFraction)
                                .height(6.dp)
                                .background(
                                    if (cryptoMean < btcMean) BearishRed else CyanAccent,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (cryptoMean >= 0) {
                        Box(
                            modifier = Modifier
                                .weight(cryptoFraction)
                                .height(6.dp)
                                .background(
                                    if (cryptoMean > btcMean) BullishGreen else CyanAccent,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.weight(1f - cryptoFraction))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Values
            Column(
                modifier = Modifier.width(90.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "BTC: ${if (btcMean >= 0) "+" else ""}${String.format(Locale.US, "%.1f", btcMean)}%",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = BtcGold
                )
                Text(
                    text = "Crypto: ${if (cryptoMean >= 0) "+" else ""}${String.format(Locale.US, "%.1f", cryptoMean)}%",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (cryptoMean >= 0) BullishGreen else BearishRed
                )
            }
        }
    }
}
