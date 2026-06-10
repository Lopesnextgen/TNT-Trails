package davi.lopes.screen

import davi.lopes.TNTTrails
import davi.lopes.config.ConfigManager
import davi.lopes.config.TNTTrailsConfig
import davi.lopes.screen.widget.TNTButton
import davi.lopes.screen.widget.TNTLink
import davi.lopes.screen.widget.TNTToggle
import davi.lopes.update.UpdateManager
import davi.lopes.util.BrowserUtil
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import kotlin.math.roundToInt

class TNTTrailsScreen(private val parent: Screen?) : Screen(TNTFonts.title(TNTTrails.MOD_NAME)) {
    private val config
        get() = ConfigManager.config

    private var panelX = 0
    private var panelY = 0
    private var panelWidth = 0
    private var panelHeight = 0

    override fun init() {
        panelWidth = (width - 16).coerceAtMost(460)
        panelHeight = (height - 12).coerceAtMost(296)
        panelX = (width - panelWidth) / 2
        panelY = (height - panelHeight) / 2

        val innerPadding = 18
        val columnGap = 14
        val columnWidth = (panelWidth - innerPadding * 2 - columnGap) / 2
        val left = panelX + innerPadding
        val right = left + columnWidth + columnGap
        val controlsTop = panelY + 74
        val rowGap = if (panelHeight < 270) 24 else 27

        addRenderableWidget(
            TNTToggle(left, controlsTop, columnWidth, "Enabled", config.enabled) {
                config.enabled = it
            }
        )
        addRenderableWidget(
            TNTToggle(left, controlsTop + rowGap, columnWidth, "TNT ESP", config.tntEsp) {
                config.tntEsp = it
            }
        )
        addRenderableWidget(
            SettingSlider(
                left,
                controlsTop + rowGap * 2,
                columnWidth,
                "Trail Delay",
                100.0,
                30000.0,
                config.delayMs.toDouble(),
                0
            ) { config.delayMs = it.roundToInt() }
        )
        addRenderableWidget(
            SettingSlider(
                left,
                controlsTop + rowGap * 3,
                columnWidth,
                "Line Width",
                1.0,
                16.0,
                config.lineWidth.toDouble(),
                1
            ) { config.lineWidth = it.toFloat() }
        )
        addRenderableWidget(
            SettingSlider(
                left,
                controlsTop + rowGap * 4,
                columnWidth,
                "ESP Hold (ms)",
                0.0,
                10000.0,
                config.tntEspDelayMs.toDouble(),
                0
            ) { config.tntEspDelayMs = it.roundToInt() }
        )

        addRenderableWidget(colorSlider(right, controlsTop, columnWidth, "Red", config.red) { config.red = it })
        addRenderableWidget(colorSlider(right, controlsTop + rowGap, columnWidth, "Green", config.green) { config.green = it })
        addRenderableWidget(colorSlider(right, controlsTop + rowGap * 2, columnWidth, "Blue", config.blue) { config.blue = it })
        addRenderableWidget(colorSlider(right, controlsTop + rowGap * 3, columnWidth, "Alpha", config.alpha) { config.alpha = it })

        UpdateManager.latestRelease?.let { release ->
            addRenderableWidget(
                TNTLink(
                    panelX + (panelWidth - 220) / 2,
                    panelY + 50,
                    220,
                    "This client is outdated. Download"
                ) {
                    BrowserUtil.open(release.downloadUrl)
                }
            )
        }

        val buttonWidth = 98
        val buttonGap = 8
        val bottom = panelY + panelHeight - 27
        addRenderableWidget(
            TNTButton(
                panelX + panelWidth / 2 - buttonWidth - buttonGap / 2,
                bottom,
                buttonWidth,
                20,
                "Reset"
            ) {
                applyDefaults()
                rebuildWidgets()
            }
        )
        addRenderableWidget(
            TNTButton(
                panelX + panelWidth / 2 + buttonGap / 2,
                bottom,
                buttonWidth,
                20,
                "Done",
                true
            ) {
                closeScreen()
            }
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, TNTPalette.BACKGROUND)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, TNTPalette.PANEL)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 61, TNTPalette.PANEL_HEADER)
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, TNTPalette.BORDER)
        graphics.drawCenteredString(
            font,
            TNTFonts.title(TNTTrails.MOD_NAME),
            width / 2,
            panelY + 13,
            TNTPalette.TEXT
        )
        graphics.drawCenteredString(
            font,
            TNTFonts.small(TNTTrails.CREDIT),
            width / 2,
            panelY + 40,
            TNTPalette.MUTED
        )

        val innerPadding = 18
        val columnGap = 14
        val columnWidth = (panelWidth - innerPadding * 2 - columnGap) / 2
        val left = panelX + innerPadding
        val right = left + columnWidth + columnGap
        graphics.drawString(font, TNTFonts.small("GENERAL"), left, panelY + 64, TNTPalette.MUTED, false)
        graphics.drawString(font, TNTFonts.small("TRAIL COLOR"), right, panelY + 64, TNTPalette.MUTED, false)

        val rowGap = if (panelHeight < 270) 24 else 27
        val previewY = panelY + 82 + rowGap * 4
        graphics.drawString(font, TNTFonts.small("PREVIEW"), right, previewY, TNTPalette.MUTED, false)
        graphics.fill(right + 50, previewY, right + columnWidth, previewY + 8, config.argb())
        graphics.renderOutline(right + 50, previewY, columnWidth - 50, 8, TNTPalette.BORDER)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        closeScreen()
    }

    override fun removed() {
        ConfigManager.save()
    }

    override fun isPauseScreen(): Boolean = false

    private fun colorSlider(
        x: Int,
        y: Int,
        width: Int,
        label: String,
        initialValue: Int,
        onChanged: (Int) -> Unit
    ): SettingSlider {
        return SettingSlider(
            x,
            y,
            width,
            label,
            0.0,
            255.0,
            initialValue.toDouble(),
            0
        ) { onChanged(it.roundToInt()) }
    }

    private fun applyDefaults() {
        val defaults = TNTTrailsConfig()
        config.enabled = defaults.enabled
        config.tntEsp = defaults.tntEsp
        config.tntEspDelayMs = defaults.tntEspDelayMs
        config.delayMs = defaults.delayMs
        config.lineWidth = defaults.lineWidth
        config.red = defaults.red
        config.green = defaults.green
        config.blue = defaults.blue
        config.alpha = defaults.alpha
        ConfigManager.save()
    }

    private fun closeScreen() {
        ConfigManager.save()
        minecraft.setScreen(parent)
    }
}
