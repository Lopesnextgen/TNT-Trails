package davi.lopes.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.network.chat.Component
import java.util.Locale
import kotlin.math.roundToInt

class SettingSlider(
    x: Int,
    y: Int,
    width: Int,
    private val label: String,
    private val minimum: Double,
    private val maximum: Double,
    initialValue: Double,
    private val decimals: Int,
    private val onChanged: (Double) -> Unit
) : AbstractSliderButton(
    x,
    y,
    width,
    22,
    Component.empty(),
    ((initialValue - minimum) / (maximum - minimum)).coerceIn(0.0, 1.0)
) {
    private var formattedValue = ""

    override fun updateMessage() {
        val currentValue = actualValue()
        formattedValue = if (decimals == 0) {
            currentValue.roundToInt().toString()
        } else {
            String.format(Locale.ROOT, "%.${decimals}f", currentValue)
        }
        message = Component.literal("$label: $formattedValue")
    }

    override fun applyValue() {
        onChanged(actualValue())
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = x
        val y = y
        graphics.fill(
            x,
            y,
            x + width,
            y + height,
            if (isHoveredOrFocused) TNTPalette.CONTROL_HOVER else TNTPalette.CONTROL
        )
        graphics.renderOutline(x, y, width, height, TNTPalette.BORDER)
        graphics.drawString(
            Minecraft.getInstance().font,
            TNTFonts.body(label),
            x + 8,
            y + 4,
            TNTPalette.TEXT,
            false
        )
        val valueText = TNTFonts.small(formattedValue)
        graphics.drawString(
            Minecraft.getInstance().font,
            valueText,
            x + width - Minecraft.getInstance().font.width(valueText) - 8,
            y + 4,
            TNTPalette.MUTED,
            false
        )

        val trackLeft = x + 8
        val trackRight = x + width - 8
        val trackY = y + 16
        val fillRight = trackLeft + ((trackRight - trackLeft) * value).roundToInt()
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, TNTPalette.TRACK)
        graphics.fill(trackLeft, trackY, fillRight, trackY + 2, TNTPalette.ACCENT)
        graphics.fill(fillRight - 1, trackY - 2, fillRight + 2, trackY + 4, TNTPalette.WHITE)
    }

    private fun actualValue(): Double {
        return minimum + value * (maximum - minimum)
    }
}
