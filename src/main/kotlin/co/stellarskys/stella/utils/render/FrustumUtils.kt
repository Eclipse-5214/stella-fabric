package co.stellarskys.stella.utils.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Frustum
import net.minecraft.util.math.Box
import org.joml.FrustumIntersection
import co.stellarskys.stella.mixin.accessors.WorldRendererAccessor
import co.stellarskys.stella.mixin.accessors.FrustumInvoker

object FrustumUtils {
    private val frustum: Frustum
        get() = (MinecraftClient.getInstance().worldRenderer as WorldRendererAccessor).frustum

    fun isVisible(box: Box): Boolean =
        frustum.isVisible(box)

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        val plane = (frustum as FrustumInvoker)
            .invokeIntersectAab(minX, minY, minZ, maxX, maxY, maxZ)
        return plane == FrustumIntersection.INSIDE || plane == FrustumIntersection.INTERSECT
    }
}