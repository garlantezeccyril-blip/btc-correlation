package com.example.ui.detail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.charts.ScatterPlotChart
import com.example.engine.WalkForwardReport
import com.example.regimes.DetailedBtcRegime
import com.example.statistics.CryptoAnalysisResult
import com.example.statistics.MathUtils
import com.example.statistics.RegimeStatistics
import com.example.statistics.ReliabilityLevel
import com.example.ui.scanner.ReliabilityChip
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDetailScreen(
    result: CryptoAnalysisResult,
    walkForwardReport: WalkForwardReport?,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Analyse Complète, 1: Walk-Forward

    val regression = MathUtils.LinearRegressionResult(
        slope = result.regressionSlope,
        intercept = result.regressionIntercept,
        rSquared = result.rSquared,
        correlation = result.pearsonCorrelation
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${result.baseCurrency} / BTC",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.displayName,
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Selector: Mode Historique vs Mode Walk-Forward
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("ANALYSE HISTORIQUE", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BtcGold,
                        selectedLabelColor = Color.Black,
                        containerColor = TerminalSurface,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selectedTab == 0) BtcGold else TerminalBorder),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("MODE WALK-FORWARD", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = TerminalSurface,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selectedTab == 1) CyanAccent else TerminalBorder),
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedTab == 0) {
                // Section: Relation Générale
                GeneralRelationshipCard(result)

                // Section: Primary Regimes Breakdown (BTC HAUSSE vs BTC BAISSE)
                Text(
                    text = "RÉACTIONS AUX RÉGIMES MAJEURS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RegimeDetailCard(
                        title = "BTC HAUSSE",
                        isBullish = true,
                        stats = result.upRegimeStats,
                        cryptoSymbol = result.baseCurrency,
                        modifier = Modifier.weight(1f)
                    )
                    RegimeDetailCard(
                        title = "BTC BAISSE",
                        isBullish = false,
                        stats = result.downRegimeStats,
                        cryptoSymbol = result.baseCurrency,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section 9: Distribution Scatter Plot & Linear Regression
                ScatterPlotChart(
                    observations = result.observations,
                    regression = regression,
                    cryptoSymbol = result.baseCurrency,
                    modifier = Modifier.fillMaxWidth()
                )

                // Section 10: Conditional Analysis ("Que fait cette crypto quand BTC bouge ?")
                ConditionalAnalysisSection(
                    result = result,
                    cryptoSymbol = result.baseCurrency
                )

            } else {
                // Section 13: Mode Walk-Forward
                WalkForwardReportSection(
                    report = walkForwardReport,
                    cryptoSymbol = result.baseCurrency
                )
            }

            // Philosophy Disclaimer
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Objectif quantitatif : cette application ne formule aucun conseil d'investissement. Elle analyse strictement la réaction statistique historique des actifs sans prédiction de prix.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralRelationshipCard(result: CryptoAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RELATION GÉNÉRALE AVEC BITCOIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                ReliabilityChip(result.reliability)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Corrélation", String.format(Locale.US, "%.2f", result.pearsonCorrelation), CyanAccent)
                MetricColumn("R² Déterm.", String.format(Locale.US, "%.2f", result.rSquared), BtcGold)
                MetricColumn("Pente β", String.format(Locale.US, "%.2f", result.regressionSlope), TextPrimary)
                MetricColumn("Volatilité", String.format(Locale.US, "%.1f%%", result.volatility), TextSecondary)
                MetricColumn("Observations", "${result.totalObservations}", TextPrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reliability assessment explanation
            val reliabilityExplanation = when (result.reliability) {
                ReliabilityLevel.FAIBLE -> "Échantillon faible (< 30 obs) : les métriques peuvent être soumises à une forte instabilité statistique."
                ReliabilityLevel.LIMITE -> "Échantillon limité (30 à 100 obs) : représentativité statistique minimale."
                ReliabilityLevel.CORRECT -> "Échantillon correct (100 à 300 obs) : niveau de significativité adéquat."
                ReliabilityLevel.IMPORTANT -> "Échantillon important (> 300 obs) : grande robustesse statistique."
            }

            Text(
                text = reliabilityExplanation,
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

@Composable
private fun RegimeDetailCard(
    title: String,
    isBullish: Boolean,
    stats: RegimeStatistics,
    cryptoSymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, if (isBullish) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isBullish) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isBullish) BullishGreen else BearishRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isBullish) BullishGreen else BearishRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "BTC moyen : ${if (stats.btcMeanReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", stats.btcMeanReturn)}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = BtcGold
            )
            Text(
                text = "$cryptoSymbol moyen : ${if (stats.cryptoMeanReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", stats.cryptoMeanReturn)}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (stats.cryptoMeanReturn >= 0) BullishGreen else BearishRed
            )
            Text(
                text = "Spread : ${if (stats.relativePerformanceMean >= 0) "+" else ""}${String.format(Locale.US, "%.1f", stats.relativePerformanceMean)} pts",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CyanAccent
            )
            Text(
                text = "Même direction : ${String.format(Locale.US, "%.0f", stats.sameDirectionPct)}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Text(
                text = "Surperformance : ${String.format(Locale.US, "%.0f", stats.outperformancePct)}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Text(
                text = "Amplification : ${String.format(Locale.US, "%.2f", stats.amplification)}x",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = BtcGold
            )
            Text(
                text = "Observations : ${stats.count}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun ConditionalAnalysisSection(
    result: CryptoAnalysisResult,
    cryptoSymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ANALYSE CONDITIONNELLE",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Que fait $cryptoSymbol quand BTC bouge ?",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Buckets in descending order from highest bull to deepest bear
            val orderedRegimes = listOf(
                DetailedBtcRegime.UP_SUPER,
                DetailedBtcRegime.UP_HIGH,
                DetailedBtcRegime.UP_MODERATE,
                DetailedBtcRegime.NEUTRAL,
                DetailedBtcRegime.DOWN_MODERATE,
                DetailedBtcRegime.DOWN_HIGH,
                DetailedBtcRegime.DOWN_SUPER
            )

            orderedRegimes.forEach { regime ->
                val stat = result.detailedRegimeStats[regime]
                if (stat != null) {
                    ConditionalRow(
                        regime = regime,
                        stat = stat,
                        cryptoSymbol = cryptoSymbol
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ConditionalRow(
    regime: DetailedBtcRegime,
    stat: RegimeStatistics,
    cryptoSymbol: String
) {
    Surface(
        color = TerminalSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            0.5.dp,
            if (regime.isBullish) BullishGreen.copy(alpha = 0.3f)
            else if (regime.isBearish) BearishRed.copy(alpha = 0.3f)
            else TerminalBorder
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = regime.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (regime.isBullish) BullishGreen else if (regime.isBearish) BearishRed else NeutralAmber
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "N=${stat.count}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                ReliabilityChip(stat.reliability)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "→ $cryptoSymbol moy : ${if (stat.cryptoMeanReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", stat.cryptoMeanReturn)}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stat.cryptoMeanReturn >= 0) BullishGreen else BearishRed
                )
                Text(
                    text = "Même dir : ${String.format(Locale.US, "%.0f", stat.sameDirectionPct)}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Text(
                    text = "Surperf : ${String.format(Locale.US, "%.0f", stat.outperformancePct)}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Text(
                    text = "Amplif : ${String.format(Locale.US, "%.2f", stat.amplification)}x",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = BtcGold
                )
            }
        }
    }
}

@Composable
private fun WalkForwardReportSection(
    report: WalkForwardReport?,
    cryptoSymbol: String
) {
    if (report == null || report.totalSteps == 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            border = BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MODE WALK-FORWARD : DONNÉES INSUFFISANTES",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Le test walk-forward nécessite un minimum de 20 observations historiques pour commencer l'évaluation sans fuite d'information future.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ÉVALUATION WALK-FORWARD SANS FUITE DE DONNÉES",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "À chaque pas T, les statistiques utilisent UNIQUEMENT les données antérieures à T pour évaluer la réaction sur le mouvement T.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Score Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    "Hit Rate Direction",
                    String.format(Locale.US, "%.1f%%", report.directionalHitRatePct),
                    if (report.directionalHitRatePct >= 50) BullishGreen else BearishRed
                )
                MetricColumn(
                    "Hit Surperformance",
                    String.format(Locale.US, "%.1f%%", report.outperformanceHitRatePct),
                    CyanAccent
                )
                MetricColumn(
                    "Erreur Abs. Moy.",
                    String.format(Locale.US, "%.2f%%", report.meanAbsoluteError),
                    TextSecondary
                )
                MetricColumn(
                    "Pas Validés",
                    "${report.totalSteps}",
                    TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DERNIERS PAS OBSERVÉS HORS-ÉCHANTILLON",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            report.steps.takeLast(10).reversed().forEach { step ->
                Surface(
                    color = TerminalSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                            .format(Date(step.timestamp * 1000))
                        Text(
                            text = dateStr,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            modifier = Modifier.width(75.dp)
                        )
                        Text(
                            text = "BTC ${if (step.btcReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", step.btcReturn)}%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = BtcGold,
                            modifier = Modifier.width(65.dp)
                        )
                        Text(
                            text = "$cryptoSymbol ${if (step.actualCryptoReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", step.actualCryptoReturn)}%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = if (step.actualCryptoReturn >= 0) BullishGreen else BearishRed,
                            modifier = Modifier.weight(1f)
                        )

                        // Direction match icon
                        if (step.directionalHit) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Validé",
                                tint = BullishGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Inversé",
                                tint = BearishRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
