package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.client.ClientPhaseShieldSound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CPhaseShieldLoopSound(int entityId, boolean active) {
    public static S2CPhaseShieldLoopSound decode(FriendlyByteBuf buf) {
        return new S2CPhaseShieldLoopSound(buf.readInt(), buf.readBoolean());
    }

    public static void encode(S2CPhaseShieldLoopSound msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.active);
    }

    public static void handle(S2CPhaseShieldLoopSound msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (msg.active()) {
                ClientPhaseShieldSound.startForEntity(msg.entityId());
            } else {
                ClientPhaseShieldSound.stopForEntity(msg.entityId());
            }
        });
        context.setPacketHandled(true);
    }
}
