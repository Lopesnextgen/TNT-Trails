package davi.lopes.screen.widget

import davi.lopes.screen.TNTFonts
import davi.lopes.screen.TNTPalette
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class TNTLink(
    x: Int,
    y: Int,
    width: Int,
    label: String,
    private val onPress: () -> Unit
) : AbstractWidget(x, y, width, 10, Component.literal(label)) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            TNTFonts.small(message.string),
            x + width / 2,
            y,
            if (isHoveredOrFocused) TNTPalette.WHITE else TNTPalette.ACCENT
        )
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        onPress()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}
