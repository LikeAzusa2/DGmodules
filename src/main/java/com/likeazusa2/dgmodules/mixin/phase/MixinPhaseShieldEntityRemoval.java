package com.likeazusa2.dgmodules.mixin.phase;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.logic.PhaseShieldLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinPhaseShieldEntityRemoval {

    private static boolean dg$isLethalState(ServerPlayer sp) {
        return sp.isDeadOrDying() || sp.getHealth() <= 0.0F || sp.deathTime > 0;
    }

    private static boolean dg$tryIntercept(ServerPlayer sp) {
        boolean active = PhaseShieldLogic.isActive(sp);
        boolean emergency = active || PhaseShieldLogic.tryActivateEmergency(sp);
        DGModules.LOGGER.info(
                "[PhaseShield] EntityRemoval intercept player={} active={} emergency={} hp={} deathTime={} deadOrDying={}",
                sp.getGameProfile().getName(),
                active,
                emergency,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying()
        );
        if (!emergency) return false;
        PhaseShieldLogic.playShieldHit(sp);
        PhaseShieldLogic.stabilizeAfterDeathIntercept(sp);
        return true;
    }

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$discard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] EntityRemoval#discard player={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (!dg$isLethalState(sp)) return;
        if (dg$tryIntercept(sp)) {
            DGModules.LOGGER.info("[PhaseShield] EntityRemoval#discard canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] EntityRemoval#remove player={} reason={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                reason,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED && !dg$isLethalState(sp)) return;
        if (dg$tryIntercept(sp)) {
            DGModules.LOGGER.info("[PhaseShield] EntityRemoval#remove canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$remove2(Entity.RemovalReason reason, boolean keepData, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] EntityRemoval#remove2 player={} reason={} keepData={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                reason,
                keepData,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED && !dg$isLethalState(sp)) return;
        if (dg$tryIntercept(sp)) {
            DGModules.LOGGER.info("[PhaseShield] EntityRemoval#remove2 canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true, require = 0)
    private void dg$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        DGModules.LOGGER.info(
                "[PhaseShield] EntityRemoval#setRemoved player={} reason={} hp={} deathTime={} deadOrDying={} active={}",
                sp.getGameProfile().getName(),
                reason,
                sp.getHealth(),
                sp.deathTime,
                sp.isDeadOrDying(),
                PhaseShieldLogic.isActive(sp)
        );
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED && !dg$isLethalState(sp)) return;
        if (dg$tryIntercept(sp)) {
            DGModules.LOGGER.info("[PhaseShield] EntityRemoval#setRemoved canceled player={}", sp.getGameProfile().getName());
            ci.cancel();
        }
    }
}
