package davi.lopes.screen.widget

import davi.lopes.screen.TNTFonts
import davi.lopes.screen.TNTPalette
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class TNTButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    label: String,
    private val accent: Boolean = false,
    private val onPress: () -> Unit
) : AbstractWidget(x, y, width, height, Component.literal(label)) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = x
        val y = y
        val color = when {
            !active -> TNTPalette.CONTROL_DISABLED
            isHoveredOrFocused -> TNTPalette.CONTROL_HOVER
            else -> TNTPalette.CONTROL
        }
        graphics.fill(x, y, x + width, y + height, color)
        graphics.renderOutline(x, y, width, height, TNTPalette.BORDER)
        if (accent) {
            graphics.fill(x, y + height - 2, x + width, y + height, TNTPalette.ACCENT)
        } else if (isHoveredOrFocused && active) {
            graphics.fill(x, y, x + 2, y + height, TNTPalette.ACCENT)
        }
        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            TNTFonts.body(message.string),
            x + width / 2,
            y + (height - 8) / 2,
            if (active) TNTPalette.TEXT else TNTPalette.MUTED
        )
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        if (active) {
            onPress()
        }
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}
