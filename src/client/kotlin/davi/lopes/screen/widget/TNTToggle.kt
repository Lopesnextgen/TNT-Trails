package davi.lopes.screen.widget

import davi.lopes.screen.TNTFonts
import davi.lopes.screen.TNTPalette
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class TNTToggle(
    x: Int,
    y: Int,
    width: Int,
    label: String,
    selected: Boolean,
    private val onChanged: (Boolean) -> Unit
) : AbstractWidget(x, y, width, 22, Component.literal(label)) {
    private var selected = selected

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
            TNTFonts.body(message.string),
            x + 8,
            y + 7,
            TNTPalette.TEXT,
            false
        )

        val switchWidth = 28
        val switchHeight = 10
        val switchX = x + width - switchWidth - 7
        val switchY = y + (height - switchHeight) / 2
        graphics.fill(switchX, switchY, switchX + switchWidth, switchY + switchHeight, TNTPalette.TRACK)
        graphics.renderOutline(switchX, switchY, switchWidth, switchHeight, TNTPalette.BORDER)
        val knobX = if (selected) switchX + switchWidth - 9 else switchX + 2
        graphics.fill(
            knobX,
            switchY + 2,
            knobX + 7,
            switchY + switchHeight - 2,
            if (selected) TNTPalette.ACCENT else TNTPalette.MUTED
        )
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        selected = !selected
        onChanged(selected)
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}
