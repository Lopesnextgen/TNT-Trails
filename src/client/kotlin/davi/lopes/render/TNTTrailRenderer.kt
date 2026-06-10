package davi.lopes.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import davi.lopes.config.ConfigManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

object TNTTrailRenderer {
    private const val MIN_TRAIL_POINTS = 2

    private val activeTnts = HashMap<Int, TrackedTnt>()
    private val loggedTrails = ArrayList<LoggedTntTrail>()
    private var level: ClientLevel? = null

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick(::tick))
        WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderEvents.AfterEntities(::render))
    }

    private fun tick(client: Minecraft) {
        val currentLevel = client.level
        if (currentLevel !== level) {
            clear()
            level = currentLevel
        }

        val config = ConfigManager.config
        if (!config.enabled || currentLevel == null) {
            clear()
            return
        }

        val now = System.currentTimeMillis()
        loggedTrails.removeIf { it.isExpired(now, maxOf(config.delayMs, config.tntEspDelayMs)) }

        val visibleTnts = currentLevel.entitiesForRendering()
            .filterIsInstance<PrimedTnt>()
            .filter { it.fuse > 0 }
            .toList()
        val visibleIds = visibleTnts.mapTo(HashSet()) { it.id }

        visibleTnts.forEach { tnt ->
            val tracked = activeTnts.getOrPut(tnt.id) { TrackedTnt() }
            tracked.lastBox = tnt.boundingBox

            val position = tnt.position()
            if (tracked.positions.lastOrNull() != position) {
                tracked.positions += position
            }
        }

        val iterator = activeTnts.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key in visibleIds) {
                continue
            }

            val tracked = entry.value
            if (tracked.positions.isNotEmpty()) {
                loggedTrails += LoggedTntTrail(
                    positions = tracked.positions.toList(),
                    lastBox = tracked.lastBox,
                    createdAt = now
                )
            }
            iterator.remove()
        }
    }

    private fun render(context: WorldRenderContext) {
        val config = ConfigManager.config
        if (!config.enabled) {
            return
        }

        val now = System.currentTimeMillis()
        loggedTrails.removeIf { it.isExpired(now, maxOf(config.delayMs, config.tntEspDelayMs)) }
        val visibleTrails = loggedTrails.filter { it.isTrailVisible(now, config.delayMs) }
        val lingeringEsp = if (config.tntEsp) {
            loggedTrails.filter { it.isEspVisible(now, config.tntEspDelayMs) }
        } else {
            emptyList()
        }
        val renderEsp = config.tntEsp && (activeTnts.isNotEmpty() || lingeringEsp.isNotEmpty())
        if (visibleTrails.isEmpty() && !renderEsp) {
            return
        }

        val camera = context.gameRenderer().mainCamera.position()
        val matrices = context.matrices()
        val lineConsumer = context.consumers().getBuffer(RenderTypes.linesTranslucent())

        visibleTrails.forEach { trail ->
            val life = trail.life(now, config.delayMs)
            val lineColor = config.argb(1f - life)
            renderTrail(matrices, lineConsumer, trail.positions, camera, lineColor, config.lineWidth)
        }

        if (renderEsp) {
            activeTnts.values.forEach { tracked ->
                renderBoxOutline(
                    matrices,
                    lineConsumer,
                    tracked.lastBox.move(camera.reverse()),
                    config.argb(0.75f),
                    config.lineWidth
                )
            }
            lingeringEsp.forEach { trail ->
                val visibility = trail.espVisibility(now, config.tntEspDelayMs)
                renderBoxOutline(
                    matrices,
                    lineConsumer,
                    trail.lastBox.move(camera.reverse()),
                    config.argb(0.75f * visibility),
                    config.lineWidth
                )
            }
        }

        if (renderEsp) {
            val fillConsumer = context.consumers().getBuffer(RenderTypes.debugFilledBox())
            activeTnts.values.forEach { tracked ->
                renderFilledBox(
                    matrices,
                    fillConsumer,
                    tracked.lastBox.move(camera.reverse()),
                    config.argb(0.24f)
                )
            }
            lingeringEsp.forEach { trail ->
                val visibility = trail.espVisibility(now, config.tntEspDelayMs)
                renderFilledBox(
                    matrices,
                    fillConsumer,
                    trail.lastBox.move(camera.reverse()),
                    config.argb(0.24f * visibility)
                )
            }
        }
    }

    private fun renderTrail(
        matrices: PoseStack,
        consumer: VertexConsumer,
        positions: List<Vec3>,
        camera: Vec3,
        color: Int,
        width: Float
    ) {
        positions.zipWithNext { from, to ->
            renderLine(
                matrices,
                consumer,
                from.subtract(camera),
                to.subtract(camera),
                color,
                width
            )
        }
    }

    private fun renderBoxOutline(
        matrices: PoseStack,
        lineConsumer: VertexConsumer,
        box: AABB,
        outlineColor: Int,
        width: Float
    ) {
        val min = Vec3(box.minX, box.minY, box.minZ)
        val x = Vec3(box.maxX, box.minY, box.minZ)
        val z = Vec3(box.minX, box.minY, box.maxZ)
        val xz = Vec3(box.maxX, box.minY, box.maxZ)
        val top = Vec3(box.minX, box.maxY, box.minZ)
        val topX = Vec3(box.maxX, box.maxY, box.minZ)
        val topZ = Vec3(box.minX, box.maxY, box.maxZ)
        val topXz = Vec3(box.maxX, box.maxY, box.maxZ)

        arrayOf(
            min to x, x to xz, xz to z, z to min,
            top to topX, topX to topXz, topXz to topZ, topZ to top,
            min to top, x to topX, z to topZ, xz to topXz
        ).forEach { (from, to) ->
            renderLine(matrices, lineConsumer, from, to, outlineColor, width)
        }
    }

    private fun renderFilledBox(
        matrices: PoseStack,
        consumer: VertexConsumer,
        box: AABB,
        color: Int
    ) {
        val pose = matrices.last()
        val vertices = arrayOf(
            Vec3(box.minX, box.minY, box.minZ), Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ), Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ), Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ), Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ), Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ), Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.maxZ), Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ), Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ), Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ), Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ), Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ), Vec3(box.maxX, box.minY, box.maxZ)
        )

        vertices.forEach { vertex ->
            consumer.addVertex(pose, vertex.x.toFloat(), vertex.y.toFloat(), vertex.z.toFloat())
                .setColor(color)
        }
    }

    private fun renderLine(
        matrices: PoseStack,
        consumer: VertexConsumer,
        from: Vec3,
        to: Vec3,
        color: Int,
        width: Float
    ) {
        val pose = matrices.last()
        val normal = Vector3f(
            (to.x - from.x).toFloat(),
            (to.y - from.y).toFloat(),
            (to.z - from.z).toFloat()
        ).normalize()

        consumer.addVertex(pose, from.x.toFloat(), from.y.toFloat(), from.z.toFloat())
            .setColor(color)
            .setNormal(pose, normal)
            .setLineWidth(width)
        consumer.addVertex(pose, to.x.toFloat(), to.y.toFloat(), to.z.toFloat())
            .setColor(color)
            .setNormal(pose, normal)
            .setLineWidth(width)
    }

    private fun clear() {
        activeTnts.clear()
        loggedTrails.clear()
    }

    private class TrackedTnt {
        val positions = ArrayList<Vec3>()
        var lastBox = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    private data class LoggedTntTrail(
        val positions: List<Vec3>,
        val lastBox: AABB,
        val createdAt: Long
    ) {
        fun isExpired(now: Long, delayMs: Int): Boolean = now - createdAt > delayMs

        fun isTrailVisible(now: Long, delayMs: Int): Boolean {
            return positions.size >= MIN_TRAIL_POINTS && now - createdAt <= delayMs
        }

        fun isEspVisible(now: Long, delayMs: Int): Boolean {
            return delayMs > 0 && now - createdAt <= delayMs
        }

        fun life(now: Long, delayMs: Int): Float {
            return ((now - createdAt).toFloat() / delayMs).coerceIn(0f, 1f)
        }

        fun espVisibility(now: Long, delayMs: Int): Float {
            if (delayMs <= 0) {
                return 0f
            }
            return 1f - ((now - createdAt).toFloat() / delayMs).coerceIn(0f, 1f)
        }
    }
}
