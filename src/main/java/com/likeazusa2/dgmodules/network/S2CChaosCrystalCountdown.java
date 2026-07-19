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

/** Synchronizes one installed device's world-space countdown to nearby clients. */
public record S2CChaosCrystalCountdown(UUID crystalId, long detonationGameTime)
        implements CustomPacketPayload {

    public static final Type<S2CChaosCrystalCountdown> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DGModules.MODID, "s2c_chaos_crystal_countdown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CChaosCrystalCountdown> STREAM_CODEC =
            StreamCodec.of(S2CChaosCrystalCountdown::encode, S2CChaosCrystalCountdown::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, S2CChaosCrystalCountdown message) {
        buffer.writeLong(message.crystalId().getMostSignificantBits());
        buffer.writeLong(message.crystalId().getLeastSignificantBits());
        ByteBufCodecs.VAR_LONG.encode(buffer, message.detonationGameTime());
    }

    private static S2CChaosCrystalCountdown decode(RegistryFriendlyByteBuf buffer) {
        return new S2CChaosCrystalCountdown(
                new UUID(buffer.readLong(), buffer.readLong()),
                ByteBufCodecs.VAR_LONG.decode(buffer)
        );
    }

    public static void handle(S2CChaosCrystalCountdown message, IPayloadContext context) {
        context.enqueueWork(() -> ChaosCrystalSingularityRenderer.handleCountdown(
                message.crystalId(), message.detonationGameTime()
        ));
    }
}
