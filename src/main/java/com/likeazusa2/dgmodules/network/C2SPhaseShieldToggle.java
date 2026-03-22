package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.logic.PhaseShieldLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SPhaseShieldToggle() {
    public static C2SPhaseShieldToggle decode(FriendlyByteBuf buf) {
        return new C2SPhaseShieldToggle();
    }

    public static void encode(C2SPhaseShieldToggle msg, FriendlyByteBuf buf) {
    }

    public static void handle(C2SPhaseShieldToggle msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PhaseShieldLogic.toggle(player);
            }
        });
        context.setPacketHandled(true);
    }
}
