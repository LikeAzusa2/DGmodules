package com.likeazusa2.dgmodules.mixin;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Catches direct writes to Player inventory NonNullLists. Several vanilla
 * paths use the public lists directly instead of Inventory#setItem.
 */
@Mixin(NonNullList.class)
public abstract class MixinHostIntegrityNonNullList {

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeSet(int index, Object replacement,
                                     CallbackInfoReturnable<Object> cir) {
        NonNullList<?> list = (NonNullList<?>) (Object) this;
        if (replacement instanceof ItemStack stack
                && HostIntegrityGuard.shouldBlockInventoryWrite(list, index, stack)) {
            cir.setReturnValue(list.get(index));
            return;
        }
        if (replacement instanceof ItemStack stack) {
            HostIntegrityGuard.beginInventoryMutation(list, index, stack);
        }
    }

    @Inject(method = "set", at = @At("RETURN"))
    private void dgmodules$afterSet(int index, Object replacement,
                                    CallbackInfoReturnable<Object> cir) {
        HostIntegrityGuard.endInventoryMutation();
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeRemove(int index, CallbackInfoReturnable<Object> cir) {
        NonNullList<?> list = (NonNullList<?>) (Object) this;
        if (HostIntegrityGuard.shouldBlockInventoryRemove(list, index)) {
            cir.setReturnValue(list.get(index));
            return;
        }
        HostIntegrityGuard.beginInventoryMutation(list, index, ItemStack.EMPTY);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void dgmodules$afterRemove(int index, CallbackInfoReturnable<Object> cir) {
        HostIntegrityGuard.endInventoryMutation();
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeClear(CallbackInfo ci) {
        if (HostIntegrityGuard.shouldBlockInventoryClear((NonNullList<?>) (Object) this)) {
            ci.cancel();
            return;
        }
        HostIntegrityGuard.beginInventoryClear((NonNullList<?>) (Object) this);
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void dgmodules$afterClear(CallbackInfo ci) {
        HostIntegrityGuard.endInventoryMutation();
    }
}
