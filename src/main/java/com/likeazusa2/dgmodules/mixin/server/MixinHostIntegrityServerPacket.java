package com.likeazusa2.dgmodules.mixin.server;

import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks serverbound player operations as owner-authorized for this call stack.
 * Commands and other server code do not pass through these handlers.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinHostIntegrityServerPacket {

    @Shadow public net.minecraft.server.level.ServerPlayer player;

    @Inject(method = {
            "handlePlayerAction",
            "handleContainerClick",
            "handleUseItemOn",
            "handleUseItem",
            "handleInteract",
            "handleSetCreativeModeSlot",
            "handlePickItem",
            "handleSetCarriedItem"
    }, at = @At("HEAD"))
    private void dgmodules$beginOwnerAction(CallbackInfo ci) {
        HostIntegrityGuard.beginOwnerAction(player);
    }

    @Inject(method = {
            "handlePlayerAction",
            "handleContainerClick",
            "handleUseItemOn",
            "handleUseItem",
            "handleInteract",
            "handleSetCreativeModeSlot",
            "handlePickItem",
            "handleSetCarriedItem"
    }, at = @At("RETURN"))
    private void dgmodules$endOwnerAction(CallbackInfo ci) {
        HostIntegrityGuard.endOwnerAction(player);
    }

    /** The selected hotbar slot can change without replacing a NonNullList entry. */
    @Inject(method = {"handleSetCarriedItem", "handlePickItem"}, at = @At("RETURN"))
    private void dgmodules$refreshSelectedBinding(CallbackInfo ci) {
        HostIntegrityGuard.requestReconcile(player);
    }
}
