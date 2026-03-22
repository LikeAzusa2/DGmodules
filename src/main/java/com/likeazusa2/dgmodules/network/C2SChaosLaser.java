package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.logic.ChaosLaserLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SChaosLaser(boolean active) {
    public static C2SChaosLaser decode(FriendlyByteBuf buf) {
        return new C2SChaosLaser(buf.readBoolean());
    }

    public static void encode(C2SChaosLaser msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static void handle(C2SChaosLaser msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ChaosLaserLogic.setFiring(player, msg.active());
            }
        });
        context.setPacketHandled(true);
    }
}
