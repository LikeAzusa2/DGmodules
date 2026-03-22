package com.likeazusa2.dgmodules.network;

import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    public static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DGModules.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void init(IEventBus modBus) {
        int id = 0;
        CHANNEL.registerMessage(id++, C2SChaosLaser.class, C2SChaosLaser::encode, C2SChaosLaser::decode, C2SChaosLaser::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, C2SFlightTunerInput.class, C2SFlightTunerInput::encode, C2SFlightTunerInput::decode, C2SFlightTunerInput::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, C2SPhaseShieldToggle.class, C2SPhaseShieldToggle::encode, C2SPhaseShieldToggle::decode, C2SPhaseShieldToggle::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, S2CLaserState.class, S2CLaserState::encode, S2CLaserState::decode, S2CLaserState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CDragonGuardWarn.class, S2CDragonGuardWarn::encode, S2CDragonGuardWarn::decode, S2CDragonGuardWarn::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CPhaseShieldState.class, S2CPhaseShieldState::encode, S2CPhaseShieldState::decode, S2CPhaseShieldState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CPhaseShieldLoopSound.class, S2CPhaseShieldLoopSound::encode, S2CPhaseShieldLoopSound::decode, S2CPhaseShieldLoopSound::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CCataclysmShockwave.class, S2CCataclysmShockwave::encode, S2CCataclysmShockwave::decode, S2CCataclysmShockwave::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
