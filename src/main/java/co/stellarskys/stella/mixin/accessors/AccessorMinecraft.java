package co.stellarskys.stella.mixin.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface AccessorMinecraft {

	@Accessor("renderTickCounter")
	RenderTickCounter.Dynamic getTimer();
}