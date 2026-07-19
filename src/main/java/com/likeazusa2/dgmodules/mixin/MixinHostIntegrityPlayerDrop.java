package com.likeazusa2.dgmodules.mixin;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents direct item-entity creation from a protected host. */
@Mixin(Player.class)
public abstract class MixinHostIntegrityPlayerDrop {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void dgmodules$blockDrop(ItemStack stack, boolean throwRandomly,
                                      CallbackInfoReturnable<ItemEntity> cir) {
        if (HostIntegrityGuard.shouldBlockPlayerDrop((Player) (Object) this, stack)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void dgmodules$blockDropWithThrower(ItemStack stack, boolean throwRandomly, boolean includeThrower,
                                                  CallbackInfoReturnable<ItemEntity> cir) {
        if (HostIntegrityGuard.shouldBlockPlayerDrop((Player) (Object) this, stack)) {
            cir.setReturnValue(null);
        }
    }
}
