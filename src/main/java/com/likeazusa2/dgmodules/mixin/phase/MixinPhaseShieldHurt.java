package com.likeazusa2.dgmodules.mixin.phase;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.logic.PhaseShieldLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase Shield lowest-level interception.
 */
@Mixin(LivingEntity.class)
public abstract class MixinPhaseShieldHurt {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void dg$hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        if (!(self instanceof ServerPlayer sp)) return;

        if (PhaseShieldLogic.isActive(sp)) {
            PhaseShieldLogic.playShieldHit(sp);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void dg$setHealth(float newHealth, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        if (!(self instanceof ServerPlayer sp)) return;

        float cur = self.getHealth();
        if (newHealth >= cur) return;

        boolean active = PhaseShieldLogic.isActive(sp);
        boolean lethal = newHealth <= 0.0F;
        DGModules.LOGGER.debug(
                "[PhaseShield] MixinPhaseShieldHurt#setHealth player={} cur={} new={} lethal={} active={}",
                sp.getGameProfile().getName(),
                cur,
                newHealth,
                lethal,
                active
        );

        if (active || (lethal && PhaseShieldLogic.tryActivateEmergency(sp))) {
            PhaseShieldLogic.playShieldHit(sp);
            if (lethal || active) {
                PhaseShieldLogic.stabilizeAfterDeathIntercept(sp);
            }
            DGModules.LOGGER.debug(
                    "[PhaseShield] MixinPhaseShieldHurt#setHealth canceled player={} lethal={} active={}",
                    sp.getGameProfile().getName(),
                    lethal,
                    active
            );
            ci.cancel();
        }
    }

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$tickDeath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        if (!(self instanceof ServerPlayer sp)) return;

        DGModules.LOGGER.debug(
                "[PhaseShield] MixinPhaseShieldHurt#tickDeath player={} hp={} deathTime={} active={}",
                sp.getGameProfile().getName(),
                sp.getHealth(),
                sp.deathTime,
                PhaseShieldLogic.isActive(sp)
        );
        if (PhaseShieldLogic.tryInterceptLethalOperation(sp)) {
            DGModules.LOGGER.debug("[PhaseShield] MixinPhaseShieldHurt#tickDeath canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

}

