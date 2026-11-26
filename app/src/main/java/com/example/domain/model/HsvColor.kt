package com.example.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    init {
        require(red in CHANNEL_RANGE) { "Red out of range: $red" }
        require(green in CHANNEL_RANGE) { "Green out of range: $green" }
        require(blue in CHANNEL_RANGE) { "Blue out of range: $blue" }
    }

    val hexString: String
        get() = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)

    val argbInt: Int
        get() = (0xFF shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)

    val relativeLuminance: Float
        get() = (0.2126f * red + 0.7152f * green + 0.0722f * blue) / 255f

    companion object {
        private val CHANNEL_RANGE = 0..255
    }
}

data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
) {
    fun normalized(): HsvColor = HsvColor(
        hue = normalizeHue(hue),
        saturation = saturation.coerceIn(0f, 1f),
        value = value.coerceIn(0f, 1f),
    )

    fun withHueSaturation(newHue: Float, newSaturation: Float): HsvColor = HsvColor(
        hue = normalizeHue(newHue),
        saturation = newSaturation.coerceIn(0f, 1f),
        value = value.coerceIn(0f, 1f),
    )

    fun withValue(newValue: Float): HsvColor = copy(
        hue = normalizeHue(hue),
        saturation = saturation.coerceIn(0f, 1f),
        value = newValue.coerceIn(0f, 1f),
    )

    fun toRgb(): RgbColor {
        val h = normalizeHue(hue)
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)

        val chroma = v * s
        val hPrime = h / 60f
        val secondary = chroma * (1f - abs((hPrime % 2f) - 1f))
        val match = v - chroma

        val (r1, g1, b1) = when {
            hPrime < 1f -> Triple(chroma, secondary, 0f)
            hPrime < 2f -> Triple(secondary, chroma, 0f)
            hPrime < 3f -> Triple(0f, chroma, secondary)
            hPrime < 4f -> Triple(0f, secondary, chroma)
            hPrime < 5f -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }

        return RgbColor(
            red = ((r1 + match) * 255f).roundToInt().coerceIn(0, 255),
            green = ((g1 + match) * 255f).roundToInt().coerceIn(0, 255),
            blue = ((b1 + match) * 255f).roundToInt().coerceIn(0, 255),
        )
    }

    fun toHex(): String = toRgb().hexString

    fun toArgbInt(): Int = toRgb().argbInt

    val isLightColor: Boolean
        get() = toRgb().relativeLuminance >= 0.56f

    companion object {
        fun normalizeHue(rawHue: Float): Float {
            if (!rawHue.isFinite()) return 0f
            val mod = rawHue % 360f
            return if (mod < 0f) mod + 360f else mod
        }

        fun of(hue: Float, saturation: Float, value: Float): HsvColor = HsvColor(
            hue = normalizeHue(hue),
            saturation = saturation.coerceIn(0f, 1f),
            value = value.coerceIn(0f, 1f),
        )
    }
}
