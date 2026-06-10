package davi.lopes.screen

import davi.lopes.TNTTrails
import davi.lopes.screen.widget.TNTButton
import davi.lopes.update.ReleaseInfo
import davi.lopes.update.UpdateManager
import davi.lopes.util.BrowserUtil
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

class UpdatePromptScreen(
    private val parent: Screen,
    private val release: ReleaseInfo
) : Screen(TNTFonts.title("TNT Trails Update")) {
    private var panelX = 0
    private var panelY = 0
    private var panelWidth = 0
    private var panelHeight = 0

    override fun init() {
        panelWidth = (width - 24).coerceAtMost(340)
        panelHeight = 154
        panelX = (width - panelWidth) / 2
        panelY = (height - panelHeight) / 2
        val buttonY = panelY + panelHeight - 31
        addRenderableWidget(
            TNTButton(panelX + 18, buttonY, (panelWidth - 44) / 2, 21, "Download", true) {
                BrowserUtil.open(release.downloadUrl)
                minecraft.setScreen(parent)
            }
        )
        addRenderableWidget(
            TNTButton(
                panelX + 26 + (panelWidth - 44) / 2,
                buttonY,
                (panelWidth - 44) / 2,
                21,
                "Ignore"
            ) {
                UpdateManager.ignore()
                minecraft.setScreen(parent)
            }
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, TNTPalette.BACKGROUND)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, TNTPalette.PANEL)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 45, TNTPalette.PANEL_HEADER)
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, TNTPalette.BORDER)
        graphics.drawCenteredString(
            font,
            TNTFonts.title("Update Available"),
            width / 2,
            panelY + 13,
            TNTPalette.TEXT
        )
        graphics.drawCenteredString(
            font,
            TNTFonts.body("A new TNT Trails version is available."),
            width / 2,
            panelY + 57,
            TNTPalette.TEXT
        )
        graphics.drawCenteredString(
            font,
            TNTFonts.small("Current ${currentVersion()}   Latest ${release.version}"),
            width / 2,
            panelY + 76,
            TNTPalette.MUTED
        )
        graphics.drawCenteredString(
            font,
            TNTFonts.small("Download the update now?"),
            width / 2,
            panelY + 93,
            TNTPalette.MUTED
        )
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        UpdateManager.ignore()
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen(): Boolean = false

    private fun currentVersion(): String {
        return FabricLoader.getInstance()
            .getModContainer(TNTTrails.MOD_ID)
            .orElseThrow()
            .metadata
            .version
            .friendlyString
    }
}
