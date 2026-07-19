package com.likeazusa2.dgmodules.mixin.server;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blocks the selected-item drop path before it splits the protected stack. */
@Mixin(ServerPlayer.class)
public abstract class MixinHostIntegrityServerPlayerDrop {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void dgmodules$blockSelectedDrop(boolean dropAll, CallbackInfoReturnable<Boolean> cir) {
        if (HostIntegrityGuard.shouldBlockSelectedDrop((ServerPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
