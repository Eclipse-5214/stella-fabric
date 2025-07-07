package co.stellarskys.stella.utils.compat

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity

object MinecraftCompat {

    val localPlayer: ClientPlayerEntity
        get() = localPlayerOrNull ?: error("localPlayer is null")

    val localPlayerOrNull: ClientPlayerEntity?
        get() = MinecraftClient.getInstance().player

    val Entity?.isLocalPlayer: Boolean
        get() = this == localPlayerOrNull && this != null

    val localPlayerExists: Boolean
        get() = localPlayerOrNull != null

    val localWorld: ClientWorld
        get() = localWorldOrNull ?: error("localWorld is null")

    val localWorldOrNull: ClientWorld?
        get() = MinecraftClient.getInstance().world

    val localWorldExists: Boolean
        get() = localWorldOrNull != null

    val showDebugHud: Boolean
        get() = MinecraftClient.getInstance().debugHud?.shouldShowDebugHud() == true
}