package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.client.ClientTickHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CLaserState(boolean firing, byte phase, long cooldownEndTick) {
    public static S2CLaserState decode(FriendlyByteBuf buf) {
        return new S2CLaserState(buf.readBoolean(), buf.readByte(), buf.readVarLong());
    }

    public static void encode(S2CLaserState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.firing);
        buf.writeByte(msg.phase);
        buf.writeVarLong(msg.cooldownEndTick);
    }

    public static void handle(S2CLaserState msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientTickHandler.applyServerState(msg.firing(), msg.phase(), msg.cooldownEndTick()));
        context.setPacketHandled(true);
    }
}
