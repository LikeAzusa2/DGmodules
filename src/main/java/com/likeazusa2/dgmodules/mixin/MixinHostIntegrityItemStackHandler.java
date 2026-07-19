package com.likeazusa2.dgmodules.mixin;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Curios handler barrier. The target is harmless for non-Curios handlers. */
@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class MixinHostIntegrityItemStackHandler {

    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeSet(int slot, ItemStack stack, CallbackInfo ci) {
        if (HostIntegrityGuard.shouldBlockCurioWrite(this, slot, stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "setStackInSlot", at = @At("RETURN"))
    private void dgmodules$afterSet(int slot, ItemStack stack, CallbackInfo ci) {
        HostIntegrityGuard.onCurioMutation(this);
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeExtract(int slot, int amount, boolean simulate,
                                          CallbackInfoReturnable<ItemStack> cir) {
        if (!simulate && HostIntegrityGuard.shouldBlockCurioExtract(this, slot)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeInsert(int slot, ItemStack stack, boolean simulate,
                                         CallbackInfoReturnable<ItemStack> cir) {
        if (!simulate && HostIntegrityGuard.shouldBlockCurioWrite(this, slot, stack)) {
            cir.setReturnValue(stack);
        }
    }
}
