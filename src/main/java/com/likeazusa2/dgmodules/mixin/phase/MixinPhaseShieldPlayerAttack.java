package com.likeazusa2.dgmodules.mixin.phase;

import com.likeazusa2.dgmodules.logic.PhaseShieldLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPhaseShieldPlayerAttack {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void dg$attack(Entity target, CallbackInfo ci) {
        Player attacker = (Player) (Object) this;
        if (attacker.level().isClientSide) return;
        if (!(target instanceof ServerPlayer sp)) return;
        if (!PhaseShieldLogic.isActive(sp)) return;

        PhaseShieldLogic.playShieldHit(sp);
        PhaseShieldLogic.stabilizeAfterDeathIntercept(sp);
        ci.cancel();
    }
}
