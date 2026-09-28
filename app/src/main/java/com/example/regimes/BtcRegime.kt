package com.example.regimes

enum class PrimaryBtcRegime(val label: String) {
    HAUSSE("BTC HAUSSE (> +1%)"),
    BAISSE("BTC BAISSE (< -1%)"),
    NEUTRE("BTC NEUTRE (-1% à +1%)");

    companion object {
        fun fromReturn(btcReturn: Double): PrimaryBtcRegime {
            return when {
                btcReturn > 1.0 -> HAUSSE
                btcReturn < -1.0 -> BAISSE
                else -> NEUTRE
            }
        }
    }
}

enum class DetailedBtcRegime(
    val label: String,
    val shortLabel: String,
    val minReturn: Double?,
    val maxReturn: Double?,
    val isBullish: Boolean,
    val isBearish: Boolean
) {
    UP_SUPER("BTC > +5%", "> +5%", 5.0, null, true, false),
    UP_HIGH("BTC +2 à +5%", "+2 à +5%", 2.0, 5.0, true, false),
    UP_MODERATE("BTC +1 à +2%", "+1 à +2%", 1.0, 2.0, true, false),
    NEUTRAL("BTC -1% à +1%", "-1 à +1%", -1.0, 1.0, false, false),
    DOWN_MODERATE("BTC -1 à -2%", "-1 à -2%", -2.0, -1.0, false, true),
    DOWN_HIGH("BTC -2 à -5%", "-2 à -5%", -5.0, -2.0, false, true),
    DOWN_SUPER("BTC < -5%", "< -5%", null, -5.0, false, true);

    companion object {
        fun fromReturn(btcReturn: Double): DetailedBtcRegime {
            return when {
                btcReturn > 5.0 -> UP_SUPER
                btcReturn > 2.0 -> UP_HIGH
                btcReturn > 1.0 -> UP_MODERATE
                btcReturn >= -1.0 -> NEUTRAL
                btcReturn >= -2.0 -> DOWN_MODERATE
                btcReturn >= -5.0 -> DOWN_HIGH
                else -> DOWN_SUPER
            }
        }
    }
}
