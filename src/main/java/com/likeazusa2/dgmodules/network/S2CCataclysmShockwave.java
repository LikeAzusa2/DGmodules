package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.client.render.CataclysmShockwaveRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CCataclysmShockwave(double x, double y, double z, float radius, int durationTicks, long startGameTime) implements CustomPacketPayload {

    public static final Type<S2CCataclysmShockwave> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DGModules.MODID, "s2c_cataclysm_shockwave"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCataclysmShockwave> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, S2CCataclysmShockwave::x,
                    ByteBufCodecs.DOUBLE, S2CCataclysmShockwave::y,
                    ByteBufCodecs.DOUBLE, S2CCataclysmShockwave::z,
                    ByteBufCodecs.FLOAT, S2CCataclysmShockwave::radius,
                    ByteBufCodecs.VAR_INT, S2CCataclysmShockwave::durationTicks,
                    ByteBufCodecs.VAR_LONG, S2CCataclysmShockwave::startGameTime,
                    S2CCataclysmShockwave::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CCataclysmShockwave msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CataclysmShockwaveRenderer.spawn(
                msg.x(),
                msg.y(),
                msg.z(),
                msg.radius(),
                msg.durationTicks(),
                msg.startGameTime()
        ));
    }
}
