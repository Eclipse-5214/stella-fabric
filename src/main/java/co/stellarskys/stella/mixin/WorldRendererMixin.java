package co.stellarskys.stella.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import co.stellarskys.stella.features.msc.OverlayToggle;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaBlockOutline(CallbackInfo ci) {
        if (OverlayToggle.shouldDisableVanillaOutline()) {
            ci.cancel();
        }
    }
}