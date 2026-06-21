package davi.lopes.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import davi.lopes.TNTTrails
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
    private const val ACTIVE_WALL_ALPHA = 0.65f
    private const val DEBUG_LOG_INTERVAL_MS = 1000L

    private val activeTnts = HashMap<Int, TrackedTnt>()
    private val loggedTrails = ArrayList<LoggedTntTrail>()
    private val wallProjections = ContinuousTrajectoryWall.WallProjectionCache()
    private var level: ClientLevel? = null
    private var lastContinuousOverlaySetting = false
    private var lastDebugLogAt = 0L

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

        if (config.continuousOverlay != lastContinuousOverlaySetting) {
            wallProjections.clear()
            lastContinuousOverlaySetting = config.continuousOverlay
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

        val currentLevel = level ?: return
        val now = System.currentTimeMillis()
        loggedTrails.removeIf { it.isExpired(now, maxOf(config.delayMs, config.tntEspDelayMs)) }
        val visibleTrails = loggedTrails.filter { it.isTrailVisible(now, config.delayMs) }
        val lingeringEsp = if (config.tntEsp) {
            loggedTrails.filter { it.isEspVisible(now, config.tntEspDelayMs) }
        } else {
            emptyList()
        }
        val renderEsp = config.tntEsp && (activeTnts.isNotEmpty() || lingeringEsp.isNotEmpty())

        val hasActiveOverlay = config.continuousOverlay &&
            activeTnts.values.any { it.positions.size >= MIN_TRAIL_POINTS }

        if (visibleTrails.isEmpty() && !renderEsp && !hasActiveOverlay) {
            return
        }

        val camera = context.gameRenderer().mainCamera.position()
        val matrices = context.matrices()

        if (config.continuousOverlay) {
            renderContinuousOverlay(matrices, context, visibleTrails, camera, now)
        } else {
            val lineConsumer = context.consumers().getBuffer(RenderTypes.linesTranslucent())
            visibleTrails.forEach { trail ->
                val life = trail.life(now, config.delayMs)
                val lineColor = config.argb(1f - life)
                renderTrail(matrices, lineConsumer, trail.positions, camera, lineColor, config.lineWidth)
            }
        }

        if (renderEsp) {
            val lineConsumer = context.consumers().getBuffer(RenderTypes.linesTranslucent())
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

    private fun renderContinuousOverlay(
        matrices: PoseStack,
        context: WorldRenderContext,
        visibleTrails: List<LoggedTntTrail>,
        camera: Vec3,
        now: Long
    ) {
        val config = ConfigManager.config
        val currentLevel = level ?: return
        val wallConsumer = context.consumers().getBuffer(RenderTypes.debugFilledBox())

        activeTnts.values.forEach { tracked ->
            renderWallForPositions(
                matrices = matrices,
                consumer = wallConsumer,
                level = currentLevel,
                cacheKey = tracked,
                positions = tracked.positions,
                camera = camera,
                color = config.argb(ACTIVE_WALL_ALPHA),
                useCache = false
            )
        }

        visibleTrails.forEach { trail ->
            val life = trail.life(now, config.delayMs)
            val visibility = 1f - life
            renderWallForPositions(
                matrices = matrices,
                consumer = wallConsumer,
                level = currentLevel,
                cacheKey = trail,
                positions = trail.positions,
                camera = camera,
                color = config.argb(ACTIVE_WALL_ALPHA * visibility),
                useCache = true
            )
        }

        maybeLogOverlayDebug(now, activeTnts.size, visibleTrails.size)
    }

    private fun renderWallForPositions(
        matrices: PoseStack,
        consumer: VertexConsumer,
        level: ClientLevel,
        cacheKey: Any,
        positions: List<Vec3>,
        camera: Vec3,
        color: Int,
        useCache: Boolean
    ) {
        if (positions.size < MIN_TRAIL_POINTS) {
            return
        }

        val groundPoints = if (useCache) {
            wallProjections.get(cacheKey, level, positions)
        } else {
            ContinuousTrajectoryWall.projectToGround(level, positions)
        }

        if (groundPoints.size != positions.size) {
            return
        }

        ContinuousTrajectoryWall.renderWall(
            matrices,
            consumer,
            positions,
            groundPoints,
            camera,
            color
        )
    }

    private fun maybeLogOverlayDebug(now: Long, activeCount: Int, visibleCount: Int) {
        val config = ConfigManager.config
        if (!config.continuousOverlay) {
            return
        }
        if (now - lastDebugLogAt < DEBUG_LOG_INTERVAL_MS) {
            return
        }
        lastDebugLogAt = now

        val activeTop = activeTnts.values
            .filter { it.positions.size >= MIN_TRAIL_POINTS }
            .sumOf { it.positions.size }
        val activeBottom = activeTnts.values
            .filter { it.positions.size >= MIN_TRAIL_POINTS }
            .sumOf {
                ContinuousTrajectoryWall.projectToGround(level ?: return, it.positions).size
            }

        TNTTrails.logger.info(
            "Continuous Overlay: enabled={} activeTnts={} visibleTrails={} activeTop={} activeBottom={}",
            config.continuousOverlay,
            activeCount,
            visibleCount,
            activeTop,
            activeBottom
        )
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
        wallProjections.clear()
        lastContinuousOverlaySetting = false
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
