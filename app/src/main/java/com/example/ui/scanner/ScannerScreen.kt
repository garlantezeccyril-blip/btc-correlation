package com.example.ui.scanner

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LookbackRange
import com.example.data.repository.Timeframe
import com.example.regimes.PrimaryBtcRegime
import com.example.statistics.ReliabilityLevel
import com.example.ui.compare.CryptoCompareScreen
import com.example.ui.detail.CryptoDetailScreen
import com.example.ui.settings.SettingsDialog
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
fun ScannerScreen(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCompareScreen by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // If detail is open, show CryptoDetailScreen
    if (uiState.selectedResult != null && !showCompareScreen) {
        CryptoDetailScreen(
            result = uiState.selectedResult,
            walkForwardReport = uiState.walkForwardReport,
            onBack = { viewModel.selectCrypto(null) }
        )
        return
    }

    // If compare screen is open
    if (showCompareScreen) {
        CryptoCompareScreen(
            allResults = uiState.allResults,
            selectedSymbols = uiState.comparedSymbols,
            onToggleSymbol = { viewModel.toggleComparedSymbol(it) },
            onBack = { showCompareScreen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BTC RESPONSE SCANNER",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = BtcGold
                            )
                        }
                        Text(
                            text = "Analyse historique Coinbase Spot",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCompareScreen = true },
                        modifier = Modifier.testTag("compare_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Comparer",
                            tint = CyanAccent
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            tint = TextSecondary
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Loading / Scanning Progress Bar
            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BtcGold,
                    trackColor = TerminalSurfaceVariant
                )
                Text(
                    text = uiState.progressMessage,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Top Control Row: Timeframes & Lookback Window
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Chips (15m, 1H, 4H, 1D, 3D, 7D)
                Timeframe.values().forEach { tf ->
                    FilterChip(
                        selected = uiState.timeframe == tf,
                        onClick = { viewModel.setTimeframe(tf) },
                        label = { Text(tf.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BtcGold,
                            selectedLabelColor = Color.Black,
                            containerColor = TerminalSurface,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (uiState.timeframe == tf) BtcGold else TerminalBorder)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(TerminalBorder)
                        .padding(horizontal = 2.dp)
                )

                // Lookback Range Chips (30 J, 90 J, 1 AN, 3 ANS)
                LookbackRange.values().forEach { range ->
                    FilterChip(
                        selected = uiState.lookbackRange == range,
                        onClick = { viewModel.setLookbackRange(range) },
                        label = { Text(range.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.Black,
                            containerColor = TerminalSurface,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (uiState.lookbackRange == range) CyanAccent else TerminalBorder)
                    )
                }
            }

            // Summary Terminal KPI Card
            TerminalKpiBanner(
                btcReturn = uiState.btcCurrentReturn,
                btcPrice = uiState.btcLastPrice,
                btcRegime = uiState.btcRegime,
                totalAssets = uiState.totalAssetsAnalyzed,
                totalObservations = uiState.totalObservations,
                timeframe = uiState.timeframe
            )

            // Search Bar + Sort & Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Rechercher crypto (ex: SOL, ETH)...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crypto_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurface,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Sort Dropdown button
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TerminalBorder),
                        modifier = Modifier.testTag("sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Trier",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.sortBy.label,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(TerminalSurface)
                    ) {
                        ScannerSortBy.values().forEach { sortItem ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sortItem.label,
                                        fontSize = 12.sp,
                                        color = if (uiState.sortBy == sortItem) CyanAccent else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSortBy(sortItem)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick Regime Filter Chips (TOUS, BTC HAUSSE, BTC BAISSE, BTC NEUTRE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RegimeFilter.values().forEach { rf ->
                    FilterChip(
                        selected = uiState.regimeFilter == rf,
                        onClick = { viewModel.setRegimeFilter(rf) },
                        label = { Text(rf.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (rf) {
                                RegimeFilter.BTC_UP -> BullishGreen
                                RegimeFilter.BTC_DOWN -> BearishRed
                                RegimeFilter.BTC_NEUTRAL -> NeutralAmber
                                RegimeFilter.ALL -> CyanAccent
                            },
                            selectedLabelColor = Color.Black,
                            containerColor = TerminalSurface,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.regimeFilter == rf) Color.Transparent else TerminalBorder
                        )
                    )
                }

                // Reliability Filter Toggle
                FilterChip(
                    selected = uiState.reliabilityFilter != null,
                    onClick = {
                        val next = when (uiState.reliabilityFilter) {
                            null -> ReliabilityLevel.LIMITE
                            ReliabilityLevel.LIMITE -> ReliabilityLevel.CORRECT
                            ReliabilityLevel.CORRECT -> ReliabilityLevel.IMPORTANT
                            ReliabilityLevel.IMPORTANT -> null
                            else -> null
                        }
                        viewModel.setReliabilityFilter(next)
                    },
                    label = {
                        Text(
                            text = "Fiabilité: ${uiState.reliabilityFilter?.shortLabel ?: "Toutes"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BtcGold,
                        selectedLabelColor = Color.Black,
                        containerColor = TerminalSurface,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (uiState.reliabilityFilter != null) BtcGold else TerminalBorder)
                )
            }

            // Main Interactive Table
            ScannerTable(
                results = uiState.filteredResults,
                sortBy = uiState.sortBy,
                sortAscending = uiState.sortAscending,
                onSortChange = { viewModel.setSortBy(it) },
                onSelectCrypto = { viewModel.selectCrypto(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            config = uiState.reliabilityConfig,
            totalCandlesInDb = uiState.totalCandlesInDb,
            onSaveConfig = { lim, cor, imp -> viewModel.updateReliabilityConfig(lim, cor, imp) },
            onClearCache = {
                viewModel.clearCache()
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun TerminalKpiBanner(
    btcReturn: Double,
    btcPrice: Double,
    btcRegime: PrimaryBtcRegime,
    totalAssets: Int,
    totalObservations: Int,
    timeframe: Timeframe
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BTC Current Status
            Column {
                Text(
                    text = "BTC actuellement (${timeframe.label}) :",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (btcReturn >= 0) "+" else ""}${String.format(Locale.US, "%.2f", btcReturn)}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (btcReturn >= 0) BullishGreen else BearishRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = when (btcRegime) {
                            PrimaryBtcRegime.HAUSSE -> BullishGreen.copy(alpha = 0.2f)
                            PrimaryBtcRegime.BAISSE -> BearishRed.copy(alpha = 0.2f)
                            PrimaryBtcRegime.NEUTRE -> NeutralAmber.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = btcRegime.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (btcRegime) {
                                PrimaryBtcRegime.HAUSSE -> BullishGreen
                                PrimaryBtcRegime.BAISSE -> BearishRed
                                PrimaryBtcRegime.NEUTRE -> NeutralAmber
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Assets Count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Actifs analysés :",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "$totalAssets",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent
                )
            }

            // Observations Count
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Observations :",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "$totalObservations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = BtcGold
                )
            }
        }
    }
}
