package davi.lopes.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import davi.lopes.TNTTrails
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object ConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path = FabricLoader.getInstance().configDir.resolve("${TNTTrails.MOD_ID}.json")
    private val factionsPath = FabricLoader.getInstance().configDir.resolve("factions-utilities.json")

    var config = TNTTrailsConfig()
        private set

    fun load() {
        restoreStandaloneConfig()
        config = runCatching {
            if (!Files.exists(path)) {
                return@runCatching TNTTrailsConfig()
            }

            Files.newBufferedReader(path).use { reader ->
                decode(JsonParser.parseReader(reader).asJsonObject)
            }
        }.onFailure {
            TNTTrails.logger.error("Failed to load configuration", it)
        }.getOrDefault(TNTTrailsConfig())

        config.normalize()
        save()
    }

    fun save() {
        config.normalize()

        runCatching {
            Files.createDirectories(path.parent)
            val temporaryPath = path.resolveSibling("${path.fileName}.tmp")
            Files.newBufferedWriter(temporaryPath).use { writer ->
                gson.toJson(config, writer)
            }
            runCatching {
                Files.move(
                    temporaryPath,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            }.getOrElse {
                Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING)
            }
        }.onFailure {
            TNTTrails.logger.error("Failed to save configuration", it)
        }
    }

    private fun restoreStandaloneConfig() {
        if (Files.exists(path) || !Files.exists(factionsPath)) {
            return
        }

        runCatching {
            Files.newBufferedReader(factionsPath).use { reader ->
                val root = JsonParser.parseReader(reader).asJsonObject
                val tnt = root.getAsJsonObject("tntTrails") ?: return@runCatching
                Files.createDirectories(path.parent)
                Files.newBufferedWriter(path).use { writer ->
                    gson.toJson(tnt, writer)
                }
            }
        }.onFailure {
            TNTTrails.logger.warn("Unable to restore TNT Trails configuration", it)
        }
    }

    private fun decode(root: JsonObject): TNTTrailsConfig {
        val source = root.getAsJsonObject("tntTrails") ?: root
        val decoded = gson.fromJson(source, TNTTrailsConfig::class.java) ?: TNTTrailsConfig()
        if (!source.has("tntEspDelayMs")) {
            decoded.tntEspDelayMs = TNTTrailsConfig().tntEspDelayMs
        }
        return decoded
    }
}
