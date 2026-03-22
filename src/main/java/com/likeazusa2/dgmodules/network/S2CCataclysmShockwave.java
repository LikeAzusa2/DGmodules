package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.client.render.CataclysmShockwaveRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CCataclysmShockwave(double x, double y, double z, float radius, int durationTicks, long startGameTime) {
    public static S2CCataclysmShockwave decode(FriendlyByteBuf buf) {
        return new S2CCataclysmShockwave(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarLong()
        );
    }

    public static void encode(S2CCataclysmShockwave msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.radius);
        buf.writeVarInt(msg.durationTicks);
        buf.writeVarLong(msg.startGameTime);
    }

    public static void handle(S2CCataclysmShockwave msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CataclysmShockwaveRenderer.spawn(
                msg.x(),
                msg.y(),
                msg.z(),
                msg.radius(),
                msg.durationTicks(),
                msg.startGameTime()
        ));
        context.setPacketHandled(true);
    }
}
