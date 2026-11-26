package com.example.domain.model

import java.util.Locale

enum class PresetColorTag(
    val code: String,
    val rgbInt: Int,
) {
    PINE("PINE", 0x1B3C33),
    COBALT("COBALT", 0x29589C),
    AMBER("AMBER", 0xB87614),
    TERRACOTTA("TERRACOTTA", 0xC85A32),
    PLUM("PLUM", 0x6E4182),
    ;

    val red: Int
        get() = (rgbInt shr 16) and 0xFF

    val green: Int
        get() = (rgbInt shr 8) and 0xFF

    val blue: Int
        get() = rgbInt and 0xFF
}

sealed interface EventColorTag {
    val code: String
    val red: Int
    val green: Int
    val blue: Int

    val rgbInt: Int
        get() = ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)

    val hexString: String
        get() = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)

    data class Preset(val preset: PresetColorTag) : EventColorTag {
        override val code: String
            get() = preset.code

        override val red: Int
            get() = preset.red

        override val green: Int
            get() = preset.green

        override val blue: Int
            get() = preset.blue
    }

    data class CustomRgb(
        override val red: Int,
        override val green: Int,
        override val blue: Int,
    ) : EventColorTag {
        init {
            require(red in CHANNEL_RANGE) { "Red channel out of range: $red" }
            require(green in CHANNEL_RANGE) { "Green channel out of range: $green" }
            require(blue in CHANNEL_RANGE) { "Blue channel out of range: $blue" }
        }

        override val code: String
            get() = hexString
    }

    companion object {
        private val CHANNEL_RANGE = 0..255
        private val HEX_RGB_REGEX = Regex("^#?([0-9A-Fa-f]{6})$")

        val PINE: EventColorTag = Preset(PresetColorTag.PINE)
        val COBALT: EventColorTag = Preset(PresetColorTag.COBALT)
        val AMBER: EventColorTag = Preset(PresetColorTag.AMBER)
        val TERRACOTTA: EventColorTag = Preset(PresetColorTag.TERRACOTTA)
        val PLUM: EventColorTag = Preset(PresetColorTag.PLUM)

        val presets: List<EventColorTag> = listOf(PINE, COBALT, AMBER, TERRACOTTA, PLUM)

        fun fromRgb(red: Int, green: Int, blue: Int): EventColorTag = CustomRgb(
            red = red.coerceIn(CHANNEL_RANGE),
            green = green.coerceIn(CHANNEL_RANGE),
            blue = blue.coerceIn(CHANNEL_RANGE),
        )

        fun fromHexOrNull(raw: String?): EventColorTag? {
            val trimmed = raw?.trim() ?: return null
            val match = HEX_RGB_REGEX.matchEntire(trimmed) ?: return null
            val hex = match.groupValues[1]
            val rgb = hex.toIntOrNull(16) ?: return null
            return CustomRgb(
                red = (rgb shr 16) and 0xFF,
                green = (rgb shr 8) and 0xFF,
                blue = rgb and 0xFF,
            )
        }

        fun fromCodeOrDefault(raw: String?, fallback: EventColorTag = PINE): EventColorTag {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return fallback
            PresetColorTag.entries.firstOrNull { it.code == normalized }?.let { return Preset(it) }
            return fromHexOrNull(normalized) ?: fallback
        }
    }
}
