package davi.lopes.update

import com.google.gson.JsonParser
import davi.lopes.TNTTrails
import davi.lopes.screen.UpdatePromptScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

object UpdateManager {
    private const val RELEASE_API = "https://api.github.com/repos/Lopesnextgen/TNT-Trails/releases/latest"

    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "TNT-Trails-Update-Check").apply { isDaemon = true }
    }

    @Volatile
    var latestRelease: ReleaseInfo? = null
        private set

    @Volatile
    private var promptDisplayed = false

    fun initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick(::showPrompt))
        CompletableFuture.runAsync(::checkForUpdates, executor)
    }

    fun ignore() {
        promptDisplayed = true
    }

    private fun checkForUpdates() {
        runCatching {
            val request = HttpRequest.newBuilder(URI.create(RELEASE_API))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "${TNTTrails.MOD_ID}-update-checker")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                error("GitHub returned HTTP ${response.statusCode()}")
            }

            val json = JsonParser.parseString(response.body()).asJsonObject
            if (json["draft"]?.asBoolean == true || json["prerelease"]?.asBoolean == true) {
                return@runCatching
            }

            val latestVersion = json["tag_name"]?.asString ?: return@runCatching
            val currentVersion = FabricLoader.getInstance()
                .getModContainer(TNTTrails.MOD_ID)
                .orElseThrow()
                .metadata
                .version
                .friendlyString

            if (!VersionComparator.isNewer(latestVersion, currentVersion)) {
                return@runCatching
            }

            val pageUrl = json["html_url"]?.asString ?: return@runCatching
            val assetUrl = json["assets"]?.asJsonArray
                ?.mapNotNull { it.asJsonObject["browser_download_url"]?.asString }
                ?.firstOrNull { it.endsWith(".jar", ignoreCase = true) }

            latestRelease = ReleaseInfo(
                version = latestVersion,
                pageUrl = pageUrl,
                downloadUrl = assetUrl ?: pageUrl
            )
        }.onFailure {
            TNTTrails.logger.warn("Unable to check for updates: {}", it.message)
        }
    }

    private fun showPrompt(client: Minecraft) {
        val release = latestRelease ?: return
        if (promptDisplayed || client.overlay != null) {
            return
        }

        val parent = client.screen ?: return
        promptDisplayed = true
        client.setScreen(UpdatePromptScreen(parent, release))
    }
}
