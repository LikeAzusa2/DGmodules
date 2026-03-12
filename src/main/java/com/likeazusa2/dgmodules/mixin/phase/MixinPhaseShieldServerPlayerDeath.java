package com.likeazusa2.dgmodules.mixin.phase;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.logic.PhaseShieldLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class MixinPhaseShieldServerPlayerDeath {

    private static boolean dg$tryInterceptDeathPath(ServerPlayer sp) {
        if (!(PhaseShieldLogic.isActive(sp) || PhaseShieldLogic.tryActivateEmergency(sp))) {
            return false;
        }
        PhaseShieldLogic.playShieldHit(sp);
        PhaseShieldLogic.stabilizeAfterDeathIntercept(sp);
        return true;
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void dg$die(DamageSource source, CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] MixinPhaseShieldServerPlayerDeath#die player={} hp={} deathTime={} active={} source={}",
                sp.getGameProfile().getName(),
                sp.getHealth(),
                sp.deathTime,
                PhaseShieldLogic.isActive(sp),
                source == null ? "null" : source.toString()
        );

        if (dg$tryInterceptDeathPath(sp)) {
            DGModules.LOGGER.info("[PhaseShield] MixinPhaseShieldServerPlayerDeath#die canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$kill(CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] MixinPhaseShieldServerPlayerDeath#kill player={} hp={} deathTime={} active={}",
                sp.getGameProfile().getName(),
                sp.getHealth(),
                sp.deathTime,
                PhaseShieldLogic.isActive(sp)
        );

        if (dg$tryInterceptDeathPath(sp)) {
            DGModules.LOGGER.info("[PhaseShield] MixinPhaseShieldServerPlayerDeath#kill canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$discard(CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] MixinPhaseShieldServerPlayerDeath#discard player={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );

        if (!(sp.isDeadOrDying() || sp.getHealth() <= 0.0F || sp.deathTime > 0)) return;

        if (dg$tryInterceptDeathPath(sp)) {
            DGModules.LOGGER.info("[PhaseShield] MixinPhaseShieldServerPlayerDeath#discard canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] MixinPhaseShieldServerPlayerDeath#remove player={} reason={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                reason,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return;
        if (!(sp.isDeadOrDying() || sp.getHealth() <= 0.0F || sp.deathTime > 0)) return;

        if (dg$tryInterceptDeathPath(sp)) {
            DGModules.LOGGER.info("[PhaseShield] MixinPhaseShieldServerPlayerDeath#remove canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$removeWithFlag(Entity.RemovalReason reason, boolean keepData, CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] MixinPhaseShieldServerPlayerDeath#remove2 player={} reason={} keepData={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                reason,
                keepData,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return;
        if (!(sp.isDeadOrDying() || sp.getHealth() <= 0.0F || sp.deathTime > 0)) return;

        if (dg$tryInterceptDeathPath(sp)) {
            DGModules.LOGGER.info("[PhaseShield] MixinPhaseShieldServerPlayerDeath#remove2 canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] MixinPhaseShieldServerPlayerDeath#setRemoved player={} reason={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                reason,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return;
        if (!(sp.isDeadOrDying() || sp.getHealth() <= 0.0F || sp.deathTime > 0)) return;

        if (dg$tryInterceptDeathPath(sp)) {
            DGModules.LOGGER.info("[PhaseShield] MixinPhaseShieldServerPlayerDeath#setRemoved canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }
}
