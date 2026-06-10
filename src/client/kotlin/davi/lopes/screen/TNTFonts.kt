package davi.lopes.screen

import davi.lopes.TNTTrails
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier

object TNTFonts {
    private val bodyFont = resource("baflion")
    private val smallFont = resource("baflion_small")
    private val titleFont = resource("baflion_title")

    fun body(value: String): MutableComponent = styled(value, bodyFont)

    fun small(value: String): MutableComponent = styled(value, smallFont)

    fun title(value: String): MutableComponent = styled(value, titleFont)

    private fun resource(path: String): FontDescription {
        return FontDescription.Resource(Identifier.fromNamespaceAndPath(TNTTrails.MOD_ID, path))
    }

    private fun styled(value: String, font: FontDescription): MutableComponent {
        return Component.literal(value).withStyle { it.withFont(font).withoutShadow() }
    }
}
