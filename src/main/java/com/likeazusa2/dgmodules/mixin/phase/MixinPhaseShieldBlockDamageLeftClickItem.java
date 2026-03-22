package com.likeazusa2.dgmodules.mixin.phase;

import com.likeazusa2.dgmodules.logic.PhaseShieldLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinPhaseShieldBlockDamageLeftClickItem {

    @Inject(method = "onLeftClickEntity", at = @At("HEAD"), cancellable = true)
    private void dgmodules$blockDamageLeftClick(ItemStack stack, Player attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (attacker.level().isClientSide) return;
        if (!(target instanceof ServerPlayer sp)) return;
        if (!PhaseShieldLogic.isActive(sp)) return;

        PhaseShieldLogic.playShieldHit(sp);
        cir.setReturnValue(true);
    }
}
