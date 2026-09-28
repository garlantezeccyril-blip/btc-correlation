package com.example.ui.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statistics.CryptoAnalysisResult
import com.example.statistics.ReliabilityLevel
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BtcGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeutralAmber
import com.example.ui.theme.SampleGood
import com.example.ui.theme.SampleLimited
import com.example.ui.theme.SampleStrong
import com.example.ui.theme.SampleWeak
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBorderLight
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ScannerTable(
    results: List<CryptoAnalysisResult>,
    sortBy: ScannerSortBy,
    sortAscending: Boolean,
    onSortChange: (ScannerSortBy) -> Unit,
    onSelectCrypto: (CryptoAnalysisResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Aucun actif correspondant aux filtres",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ajustez vos filtres de recherche ou de régime",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scanner_table_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = results,
            key = { it.symbol }
        ) { item ->
            ScannerRowCard(
                result = item,
                onClick = { onSelectCrypto(item) }
            )
        }
    }
}

@Composable
fun ScannerRowCard(
    result: CryptoAnalysisResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("crypto_card_${result.baseCurrency.lowercase()}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Asset Identity + Reliability Badge + Pearson Correlation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Asset Icon placeholder badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (result.baseCurrency) {
                                "SOL" -> Color(0xFF14F195).copy(alpha = 0.2f)
                                "ETH" -> Color(0xFF627EEA).copy(alpha = 0.2f)
                                "XRP" -> Color(0xFF23292F)
                                else -> CyanAccent.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (result.baseCurrency) {
                                "SOL" -> Color(0xFF14F195)
                                "ETH" -> Color(0xFF627EEA)
                                else -> CyanAccent
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = result.baseCurrency.take(3),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.baseCurrency,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = result.displayName,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        ReliabilityChip(result.reliability)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${result.totalObservations} obs",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Pearson Correlation Pill
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Corr. Pearson",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", result.pearsonCorrelation),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            result.pearsonCorrelation > 0.7 -> CyanAccent
                            result.pearsonCorrelation > 0.4 -> BtcGold
                            else -> TextSecondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-column statistical breakdown: BTC UP vs BTC DOWN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurfaceVariant, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // BTC HAUSSE Reaction
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = BullishGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "BTC HAUSSE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BullishGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Moy: ${if (result.upRegimeStats.cryptoMeanReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", result.upRegimeStats.cryptoMeanReturn)}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (result.upRegimeStats.cryptoMeanReturn >= 0) BullishGreen else BearishRed
                    )
                    Text(
                        text = "Amplif: ${String.format(Locale.US, "%.2f", result.amplificationUp)}x",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BtcGold
                    )
                    Text(
                        text = "Même dir: ${String.format(Locale.US, "%.0f", result.upRegimeStats.sameDirectionPct)}%",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(55.dp)
                        .background(TerminalBorder)
                )

                // BTC BAISSE Reaction
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = BearishRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "BTC BAISSE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BearishRed
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Moy: ${if (result.downRegimeStats.cryptoMeanReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", result.downRegimeStats.cryptoMeanReturn)}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = BearishRed
                    )
                    Text(
                        text = "Amplif: ${String.format(Locale.US, "%.2f", result.amplificationDown)}x",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BtcGold
                    )
                    Text(
                        text = "Spread: ${if (result.downRegimeStats.relativePerformanceMean >= 0) "+" else ""}${String.format(Locale.US, "%.1f", result.downRegimeStats.relativePerformanceMean)} pts",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom metric bar: Outperformance, Volatility, Click to inspect
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Surperf: ${String.format(Locale.US, "%.0f", result.overallOutperformancePct)}%",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (result.overallOutperformancePct > 50) BullishGreen else TextMuted
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Volatilité: ${String.format(Locale.US, "%.1f", result.volatility)}%",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fiche détaillée",
                        fontSize = 11.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ouvrir",
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReliabilityChip(level: ReliabilityLevel) {
    val (bgColor, textColor) = when (level) {
        ReliabilityLevel.FAIBLE -> Pair(SampleWeak.copy(alpha = 0.2f), SampleWeak)
        ReliabilityLevel.LIMITE -> Pair(SampleLimited.copy(alpha = 0.2f), SampleLimited)
        ReliabilityLevel.CORRECT -> Pair(SampleGood.copy(alpha = 0.2f), SampleGood)
        ReliabilityLevel.IMPORTANT -> Pair(SampleStrong.copy(alpha = 0.2f), SampleStrong)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = level.shortLabel,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
