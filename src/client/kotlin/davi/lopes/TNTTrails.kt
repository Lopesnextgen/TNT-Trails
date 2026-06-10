package davi.lopes

import davi.lopes.command.TNTTrailsCommand
import davi.lopes.config.ConfigManager
import davi.lopes.render.TNTTrailRenderer
import davi.lopes.update.UpdateManager
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object TNTTrails : ClientModInitializer {
    const val MOD_ID = "tnt-trails"
    const val MOD_NAME = "TNT Trails"
    const val CREDIT = "This client has made by Lopes (jvmexploit)"

    val logger = LoggerFactory.getLogger(MOD_NAME)

    override fun onInitializeClient() {
        ConfigManager.load()
        TNTTrailRenderer.register()
        TNTTrailsCommand.register()
        UpdateManager.initialize()
        logger.info("{} initialized", MOD_NAME)
    }
}
