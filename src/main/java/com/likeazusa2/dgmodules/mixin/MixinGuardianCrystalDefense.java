package com.likeazusa2.dgmodules.mixin;

import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.likeazusa2.dgmodules.logic.ChaosCrystalBreakerLogic;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps a detonated island crystal at zero shield for the configured window.
 * DE normally regenerates its shield every tick, and its destabilize path
 * emits Minecraft particles, so the breaker uses a silent custom path.
 */
@Mixin(value = GuardianCrystalEntity.class, remap = false)
public abstract class MixinGuardianCrystalDefense {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/brandon3055/draconicevolution/entity/GuardianCrystalEntity;setShieldPower(F)V"
            ),
            remap = false
    )
    private void dg$keepShieldAtZero(GuardianCrystalEntity crystal, float regeneratedPower) {
        if (ChaosCrystalBreakerLogic.isDefenseDisabled(crystal)) {
            crystal.setShieldPower(0.0F);
        } else {
            crystal.setShieldPower(regeneratedPower);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), remap = false)
    private void dg$forceVulnerable(DamageSource source, float amount,
                                    CallbackInfoReturnable<Boolean> cir) {
        GuardianCrystalEntity crystal = (GuardianCrystalEntity) (Object) this;
        if (ChaosCrystalBreakerLogic.isDefenseDisabled(crystal)) {
            crystal.setInvulnerable(false);
            crystal.setShieldPower(0.0F);
        }
    }
}
