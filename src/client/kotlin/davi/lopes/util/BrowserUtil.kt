package davi.lopes.util

import davi.lopes.TNTTrails
import net.minecraft.util.Util
import java.net.URI

object BrowserUtil {
    fun open(url: String) {
        runCatching {
            Util.getPlatform().openUri(URI.create(url))
        }.onFailure {
            TNTTrails.logger.error("Failed to open URL: {}", url, it)
        }
    }
}
