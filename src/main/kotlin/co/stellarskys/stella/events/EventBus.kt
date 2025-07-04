package co.stellarskys.stella.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screen.Screen
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.*
import org.lwjgl.glfw.GLFW
import java.util.concurrent.ConcurrentHashMap

object EventBus {
    val listeners = ConcurrentHashMap<Class<*>, MutableSet<Any>>()
    var totalTicks = 0

    init {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            post(TickEvent())
            totalTicks ++
        }
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            post(EntityJoinEvent(entity))
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            post(EntityLeaveEvent(entity))
        }
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { mc, world ->
            post(WorldChangeEvent(mc, world))
        }
        ClientReceiveMessageEvents.ALLOW_GAME.register { msg, show ->
            !post(ChatReceiveEvent(msg, show))
        }
        WorldRenderEvents.LAST.register { context ->
            post(RenderWorldEvent(context))
        }
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            post(RenderWorldPostEntitiesEvent(context))
        }
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.allowMouseClick(screen).register { _, mx, my, mbtn ->
                !post(GuiClickEvent(mx, my, mbtn, true, screen))
            }

            ScreenMouseEvents.allowMouseRelease(screen).register { _, mx, my, mbtn ->
                !post(GuiClickEvent(mx, my, mbtn, false, screen))
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key, scancode, _ ->
                !post(GuiKeyEvent(GLFW.glfwGetKeyName(key, scancode), key, scancode, screen))
            }
            ScreenEvents.remove(screen).register { screen ->
                post(GuiCloseEvent(screen))
            }
            ScreenEvents.afterRender(screen).register { _, context, mouseX, mouseY, tickDelta ->
                post(GuiAfterRenderEvent(screen))
            }
        }
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (screen != null) post(GuiOpenEvent(screen))
        }
        WorldRenderEvents.BLOCK_OUTLINE.register { worldContext, blockContext ->
            !post(BlockOutlineEvent(worldContext, blockContext))
        }
    }

    fun onPacketReceived(packet: Packet<*>) {
        post(PacketEvent.Received(packet))
        PacketReceived(packet)
    }

    fun onPacketSent(packet: Packet<*>) {
        post(PacketEvent.Sent(packet))
    }

    private fun PacketReceived(packet: Packet<*>) {
        when (packet) {
            is EntityTrackerUpdateS2CPacket -> {
                post(EntityMetadataEvent(packet))
            }
            is ScoreboardObjectiveUpdateS2CPacket, is ScoreboardScoreUpdateS2CPacket, is ScoreboardDisplayS2CPacket, is TeamS2CPacket -> {
                post(ScoreboardEvent(packet))
            }
            is PlayerListS2CPacket -> {
                when (packet.actions.firstOrNull()) {
                    PlayerListS2CPacket.Action.ADD_PLAYER, PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME -> {
                        post(TablistEvent(packet))
                    }
                    else -> {}
                }
            }
        }
    }

    inline fun <reified T : Event> register(noinline callback: (T) -> Unit, add: Boolean = true): EventCall {
        val eventClass = T::class.java
        val handlers = listeners.getOrPut(eventClass) { ConcurrentHashMap.newKeySet() }
        if (add) handlers.add(callback)
        return EventCallImpl(callback, handlers)
    }

    fun <T : Event> post(event: T): Boolean {
        val eventClass = event::class.java
        val handlers = listeners[eventClass] ?: return false
        if (handlers.isEmpty()) return false

        for (handler in handlers) {
            try {
                @Suppress("UNCHECKED_CAST")
                (handler as (T) -> Unit)(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return if (event is CancellableEvent) event.isCancelled() else false
    }

    class EventCallImpl(
        private val callback: Any,
        private val handlers: MutableSet<Any>
    ) : EventCall {
        override fun unregister(): Boolean = handlers.remove(callback)
        override fun register(): Boolean = handlers.add(callback)
    }

    interface EventCall {
        fun unregister(): Boolean
        fun register(): Boolean
    }
}