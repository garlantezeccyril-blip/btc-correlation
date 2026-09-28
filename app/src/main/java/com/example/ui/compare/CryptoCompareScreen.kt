package com.example.ui.compare

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regimes.DetailedBtcRegime
import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.CryptoAnalysisResult
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BtcGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeutralAmber
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoCompareScreen(
    allResults: List<CryptoAnalysisResult>,
    selectedSymbols: Set<String>,
    onToggleSymbol: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val scrollState = rememberScrollState()

    val comparedResults = allResults.filter { selectedSymbols.contains(it.symbol) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "COMPARAISON DE RÉACTION",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Comportement multi-actifs dans les mêmes régimes BTC",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = TerminalBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Asset Selector Chips
            Text(
                text = "ACTIFS COMPARÉS (${comparedResults.size} SÉLECTIONNÉS)",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allResults) { res ->
                    val isSelected = selectedSymbols.contains(res.symbol)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleSymbol(res.symbol) },
                        label = {
                            Text(
                                text = res.baseCurrency,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black,
                            containerColor = TerminalSurface,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) CyanAccent else TerminalBorder)
                    )
                }
            }

            // Objective statistical matrix disclaimer
            Surface(
                color = TerminalSurfaceVariant,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Note méthodologique : Présentation purement statistique objective. Aucun classement 'meilleur / moins bon' n'est calculé.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Primary Regimes Comparison Cards
            CompareRegimeSection(
                title = "BTC HAUSSE (> +1%)",
                isBullish = true,
                comparedResults = comparedResults,
                extractMean = { it.upRegimeStats.cryptoMeanReturn },
                extractAmplif = { it.amplificationUp },
                extractSameDir = { it.upRegimeStats.sameDirectionPct },
                extractCount = { it.upRegimeStats.count }
            )

            CompareRegimeSection(
                title = "BTC BAISSE (< -1%)",
                isBullish = false,
                comparedResults = comparedResults,
                extractMean = { it.downRegimeStats.cryptoMeanReturn },
                extractAmplif = { it.amplificationDown },
                extractSameDir = { it.downRegimeStats.sameDirectionPct },
                extractCount = { it.downRegimeStats.count }
            )

            // Detailed Regimes Matrix
            Text(
                text = "SOUS-CATÉGORIES DE MOUVEMENTS BTC",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            val subRegimes = listOf(
                DetailedBtcRegime.UP_SUPER,
                DetailedBtcRegime.UP_HIGH,
                DetailedBtcRegime.UP_MODERATE,
                DetailedBtcRegime.NEUTRAL,
                DetailedBtcRegime.DOWN_MODERATE,
                DetailedBtcRegime.DOWN_HIGH,
                DetailedBtcRegime.DOWN_SUPER
            )

            subRegimes.forEach { reg ->
                DetailedCompareRow(
                    regime = reg,
                    comparedResults = comparedResults
                )
            }
        }
    }
}

@Composable
private fun CompareRegimeSection(
    title: String,
    isBullish: Boolean,
    comparedResults: List<CryptoAnalysisResult>,
    extractMean: (CryptoAnalysisResult) -> Double,
    extractAmplif: (CryptoAnalysisResult) -> Double,
    extractSameDir: (CryptoAnalysisResult) -> Double,
    extractCount: (CryptoAnalysisResult) -> Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, if (isBullish) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isBullish) BullishGreen else BearishRed
            )

            Spacer(modifier = Modifier.height(10.dp))

            comparedResults.forEach { res ->
                val mean = extractMean(res)
                val amplif = extractAmplif(res)
                val sameDir = extractSameDir(res)
                val count = extractCount(res)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = res.baseCurrency,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.width(65.dp)
                    )

                    Text(
                        text = "→ ${if (mean >= 0) "+" else ""}${String.format(Locale.US, "%.1f", mean)}%",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (mean >= 0) BullishGreen else BearishRed,
                        modifier = Modifier.width(85.dp)
                    )

                    Text(
                        text = "Amplif: ${String.format(Locale.US, "%.2f", amplif)}x",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = BtcGold,
                        modifier = Modifier.width(95.dp)
                    )

                    Text(
                        text = "Même dir: ${String.format(Locale.US, "%.0f", sameDir)}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "N=$count",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedCompareRow(
    regime: DetailedBtcRegime,
    comparedResults: List<CryptoAnalysisResult>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = regime.label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (regime.isBullish) BullishGreen else if (regime.isBearish) BearishRed else NeutralAmber
            )

            Spacer(modifier = Modifier.height(6.dp))

            comparedResults.forEach { res ->
                val stat = res.detailedRegimeStats[regime]
                val mean = stat?.cryptoMeanReturn ?: 0.0
                val count = stat?.count ?: 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = res.baseCurrency,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "→ ${if (mean >= 0) "+" else ""}${String.format(Locale.US, "%.1f", mean)}%",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (mean >= 0) BullishGreen else BearishRed,
                        modifier = Modifier.width(80.dp)
                    )
                    if (stat != null && stat.count > 0) {
                        Text(
                            text = "Surperf: ${String.format(Locale.US, "%.0f", stat.outperformancePct)}%",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "N=$count",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Pas de données",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
