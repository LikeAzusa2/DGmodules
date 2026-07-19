package com.likeazusa2.dgmodules.mixin;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents external code from assigning an active protected stack to an item entity. */
@Mixin(ItemEntity.class)
public abstract class MixinHostIntegrityItemEntity {

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void dgmodules$blockAssignment(ItemStack stack, CallbackInfo ci) {
        if (HostIntegrityGuard.shouldBlockItemEntityAssignment(stack)) {
            ci.cancel();
        }
    }
}
