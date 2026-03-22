package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.client.ClientDragonGuardHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CDragonGuardWarn(int durationTicks) {
    public static S2CDragonGuardWarn decode(FriendlyByteBuf buf) {
        return new S2CDragonGuardWarn(buf.readVarInt());
    }

    public static void encode(S2CDragonGuardWarn msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.durationTicks);
    }

    public static void handle(S2CDragonGuardWarn msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientDragonGuardHud.trigger(msg.durationTicks()));
        context.setPacketHandled(true);
    }
}
