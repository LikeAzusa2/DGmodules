package com.likeazusa2.dgmodules.mixin;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Low-level component barrier for an active protected host stack. */
@Mixin(ItemStack.class)
public abstract class MixinHostIntegrityItemStack {

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeSet(DataComponentType<?> type, Object value,
                                     CallbackInfoReturnable<Object> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!HostIntegrityGuard.allowComponentMutation(stack, type)) {
            cir.setReturnValue(stack.get((DataComponentType<Object>) type));
        }
    }

    @Inject(method = "set", at = @At("RETURN"))
    private void dgmodules$afterSet(DataComponentType<?> type, Object value,
                                    CallbackInfoReturnable<Object> cir) {
        HostIntegrityGuard.onComponentMutation((ItemStack) (Object) this, type);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeRemove(DataComponentType<?> type,
                                         CallbackInfoReturnable<Object> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!HostIntegrityGuard.allowComponentMutation(stack, type)) {
            cir.setReturnValue(stack.get((DataComponentType<Object>) type));
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void dgmodules$afterRemove(DataComponentType<?> type,
                                       CallbackInfoReturnable<Object> cir) {
        HostIntegrityGuard.onComponentMutation((ItemStack) (Object) this, type);
    }

    @Inject(method = "setCount", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeSetCount(int count, CallbackInfo ci) {
        if (!HostIntegrityGuard.allowCountMutation((ItemStack) (Object) this, count)) {
            ci.cancel();
        }
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"))
    private void dgmodules$beginDurabilityMutation(CallbackInfo ci) {
        HostIntegrityGuard.beginInternalMutation();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("RETURN"))
    private void dgmodules$endDurabilityMutation(CallbackInfo ci) {
        HostIntegrityGuard.endInternalMutation();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"))
    private void dgmodules$beginDurabilityMutationLiving(CallbackInfo ci) {
        HostIntegrityGuard.beginInternalMutation();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("RETURN"))
    private void dgmodules$endDurabilityMutationLiving(CallbackInfo ci) {
        HostIntegrityGuard.endInternalMutation();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"))
    private void dgmodules$beginDurabilityMutationSlot(CallbackInfo ci) {
        HostIntegrityGuard.beginInternalMutation();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("RETURN"))
    private void dgmodules$endDurabilityMutationSlot(CallbackInfo ci) {
        HostIntegrityGuard.endInternalMutation();
    }

    @Inject(method = "applyComponentsAndValidate", at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeApplyPatch(DataComponentPatch patch, CallbackInfo ci) {
        if (!HostIntegrityGuard.allowComponentsPatch((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyComponentsAndValidate", at = @At("RETURN"))
    private void dgmodules$afterApplyPatch(DataComponentPatch patch, CallbackInfo ci) {
        HostIntegrityGuard.onComponentMutation((ItemStack) (Object) this, null);
    }

    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V",
            at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeApplyPatchDirect(DataComponentPatch patch, CallbackInfo ci) {
        if (!HostIntegrityGuard.allowComponentsPatch((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V",
            at = @At("RETURN"))
    private void dgmodules$afterApplyPatchDirect(DataComponentPatch patch, CallbackInfo ci) {
        HostIntegrityGuard.onComponentMutation((ItemStack) (Object) this, null);
    }

    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V",
            at = @At("HEAD"), cancellable = true)
    private void dgmodules$beforeApplyMap(DataComponentMap map, CallbackInfo ci) {
        if (!HostIntegrityGuard.allowComponentsPatch((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V",
            at = @At("RETURN"))
    private void dgmodules$afterApplyMap(DataComponentMap map, CallbackInfo ci) {
        HostIntegrityGuard.onComponentMutation((ItemStack) (Object) this, null);
    }
}
