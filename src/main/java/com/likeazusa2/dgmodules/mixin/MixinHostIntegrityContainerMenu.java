package com.likeazusa2.dgmodules.mixin;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Covers the complete menu click call, including modded menus that invoke
 * clicked() without going through the vanilla packet listener.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class MixinHostIntegrityContainerMenu {

    @Inject(method = "clicked", at = @At("HEAD"))
    private void dgmodules$beginMenuClick(int slotId, int button, ClickType clickType,
                                          Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            HostIntegrityGuard.beginOwnerAction(serverPlayer);
        }
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void dgmodules$endMenuClick(int slotId, int button, ClickType clickType,
                                        Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            HostIntegrityGuard.endOwnerAction(serverPlayer);
        }
    }
}
