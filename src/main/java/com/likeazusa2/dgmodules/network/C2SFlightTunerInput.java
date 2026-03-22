package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.logic.FlightTunerLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SFlightTunerInput(boolean jump, boolean sneak, float zza, float xxa) {
    public static C2SFlightTunerInput decode(FriendlyByteBuf buf) {
        return new C2SFlightTunerInput(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void encode(C2SFlightTunerInput msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.jump);
        buf.writeBoolean(msg.sneak);
        buf.writeFloat(msg.zza);
        buf.writeFloat(msg.xxa);
    }

    public static void handle(C2SFlightTunerInput msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                FlightTunerLogic.setClientInput(player, msg.jump(), msg.sneak(), msg.zza(), msg.xxa());
            }
        });
        context.setPacketHandled(true);
    }
}
