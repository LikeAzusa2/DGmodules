package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.client.render.ChaosCrystalSingularityRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record S2CChaosCrystalSingularity(
        double x,
        double y,
        double z,
        float radius,
        int effectTicks,
        long startGameTime,
        UUID crystalId,
        int defenseDurationTicks
) implements CustomPacketPayload {

    public static final Type<S2CChaosCrystalSingularity> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DGModules.MODID, "s2c_chaos_crystal_singularity"));

    // A hand-written codec keeps the payload explicit and avoids relying on
    // StreamCodec.composite's field-count overload limit.
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CChaosCrystalSingularity> STREAM_CODEC =
            StreamCodec.of(S2CChaosCrystalSingularity::encode, S2CChaosCrystalSingularity::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, S2CChaosCrystalSingularity message) {
        ByteBufCodecs.DOUBLE.encode(buffer, message.x());
        ByteBufCodecs.DOUBLE.encode(buffer, message.y());
        ByteBufCodecs.DOUBLE.encode(buffer, message.z());
        ByteBufCodecs.FLOAT.encode(buffer, message.radius());
        ByteBufCodecs.VAR_INT.encode(buffer, message.effectTicks());
        ByteBufCodecs.VAR_LONG.encode(buffer, message.startGameTime());
        buffer.writeLong(message.crystalId().getMostSignificantBits());
        buffer.writeLong(message.crystalId().getLeastSignificantBits());
        ByteBufCodecs.VAR_INT.encode(buffer, message.defenseDurationTicks());
    }

    private static S2CChaosCrystalSingularity decode(RegistryFriendlyByteBuf buffer) {
        return new S2CChaosCrystalSingularity(
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.DOUBLE.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_LONG.decode(buffer),
                new UUID(buffer.readLong(), buffer.readLong()),
                ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }

    public static void handle(S2CChaosCrystalSingularity message, IPayloadContext context) {
        context.enqueueWork(() -> ChaosCrystalSingularityRenderer.handle(
                message.x(),
                message.y(),
                message.z(),
                message.radius(),
                message.effectTicks(),
                message.startGameTime(),
                message.crystalId(),
                message.defenseDurationTicks()
        ));
    }
}
