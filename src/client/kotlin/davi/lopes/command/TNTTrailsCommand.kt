package davi.lopes.command

import davi.lopes.screen.TNTTrailsScreen
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft

object TNTTrailsCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("tnttrails").executes {
                    val client = Minecraft.getInstance()
                    client.execute {
                        client.setScreen(TNTTrailsScreen(client.screen))
                    }
                    1
                }
            )
        }
    }
}
