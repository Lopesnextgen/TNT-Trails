package davi.lopes.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

object ContinuousTrajectoryWall {
    private const val GROUND_OFFSET = 0.002
    private const val START_DROP = 0.001

    fun projectToGround(level: ClientLevel, topPoints: List<Vec3>): List<Vec3> {
        if (topPoints.isEmpty()) {
            return emptyList()
        }
        if (level == null) {
            return List(topPoints.size) { Vec3(0.0, 0.0, 0.0) }
        }

        val minY = level.minY
        return ArrayList<Vec3>(topPoints.size).apply {
            for (top in topPoints) {
                if (!isFinite(top)) {
                    add(Vec3(0.0, minY.toDouble(), 0.0))
                    continue
                }
                val groundY = projectSinglePoint(level, top, minY)
                add(Vec3(top.x, groundY, top.z))
            }
        }
    }

    private fun projectSinglePoint(level: ClientLevel, point: Vec3, fallbackY: Int): Double {
        val start = Vec3(point.x, point.y - START_DROP, point.z)
        val end = Vec3(point.x, fallbackY.toDouble(), point.z)
        return runCatching {
            val hit = level.clip(
                ClipContext(
                    start,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
                )
            )
            if (hit.type == HitResult.Type.MISS) {
                fallbackY.toDouble()
            } else {
                hit.location.y + GROUND_OFFSET
            }
        }.getOrDefault(fallbackY.toDouble())
    }

    fun renderWall(
        matrices: PoseStack,
        consumer: VertexConsumer,
        topPoints: List<Vec3>,
        bottomPoints: List<Vec3>,
        camera: Vec3,
        color: Int
    ) {
        if (topPoints.isEmpty() || bottomPoints.isEmpty() || camera == null) {
            return
        }
        val count = minOf(topPoints.size, bottomPoints.size)
        if (count < 2) {
            return
        }

        val pose = matrices.last()
        for (index in 0 until count - 1) {
            val topA = topPoints[index]
            val topB = topPoints[index + 1]
            val bottomA = bottomPoints[index]
            val bottomB = bottomPoints[index + 1]

            if (!isFinite(topA) || !isFinite(topB) || !isFinite(bottomA) || !isFinite(bottomB)) {
                continue
            }

            addVertex(pose, consumer, topA, camera, color)
            addVertex(pose, consumer, topB, camera, color)
            addVertex(pose, consumer, bottomB, camera, color)
            addVertex(pose, consumer, bottomA, camera, color)

            addVertex(pose, consumer, bottomA, camera, color)
            addVertex(pose, consumer, bottomB, camera, color)
            addVertex(pose, consumer, topB, camera, color)
            addVertex(pose, consumer, topA, camera, color)
        }
    }

    private fun addVertex(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        worldPos: Vec3,
        camera: Vec3,
        color: Int
    ) {
        consumer.addVertex(
            pose,
            (worldPos.x - camera.x).toFloat(),
            (worldPos.y - camera.y).toFloat(),
            (worldPos.z - camera.z).toFloat()
        ).setColor(color)
    }

    private fun isFinite(v: Vec3?): Boolean {
        return v != null &&
            v.x.isFinite() &&
            v.y.isFinite() &&
            v.z.isFinite()
    }

    class WallProjectionCache {
        private val cache = HashMap<Any, List<Vec3>>()

        fun get(key: Any?, level: ClientLevel, topPoints: List<Vec3>): List<Vec3> {
            if (key == null) {
                return projectToGround(level, topPoints)
            }
            val cached = cache[key]
            if (cached != null && cached.size == topPoints.size) {
                return cached
            }
            val computed = projectToGround(level, topPoints)
            cache[key] = computed
            return computed
        }

        fun invalidate(key: Any) {
            cache.remove(key)
        }

        fun clear() {
            cache.clear()
        }
    }
}
