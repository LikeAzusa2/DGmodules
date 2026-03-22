package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.client.ClientPhaseShieldHud;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CPhaseShieldState(boolean active, int secondsRemaining) {
    public static S2CPhaseShieldState decode(FriendlyByteBuf buf) {
        return new S2CPhaseShieldState(buf.readBoolean(), buf.readInt());
    }

    public static void encode(S2CPhaseShieldState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.secondsRemaining);
    }

    public static void handle(S2CPhaseShieldState msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                ClientPhaseShieldHud.setState(msg.active(), msg.secondsRemaining());
            }
        });
        context.setPacketHandled(true);
    }
}
