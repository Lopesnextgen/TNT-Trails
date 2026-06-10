package davi.lopes.config

data class TNTTrailsConfig(
    var enabled: Boolean = true,
    var tntEsp: Boolean = true,
    var tntEspDelayMs: Int = 1000,
    var delayMs: Int = 3000,
    var lineWidth: Float = 2f,
    var red: Int = 255,
    var green: Int = 72,
    var blue: Int = 72,
    var alpha: Int = 220
) {
    fun normalize() {
        tntEspDelayMs = tntEspDelayMs.coerceIn(0, 10000)
        delayMs = delayMs.coerceIn(100, 30000)
        lineWidth = lineWidth.coerceIn(1f, 16f)
        red = red.coerceIn(0, 255)
        green = green.coerceIn(0, 255)
        blue = blue.coerceIn(0, 255)
        alpha = alpha.coerceIn(0, 255)
    }

    fun argb(alphaMultiplier: Float = 1f): Int {
        val adjustedAlpha = (alpha * alphaMultiplier).toInt().coerceIn(0, 255)
        return adjustedAlpha shl 24 or (red shl 16) or (green shl 8) or blue
    }
}
