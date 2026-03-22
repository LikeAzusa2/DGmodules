package com.likeazusa2.dgmodules.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        ChaosLaserLogic.tick(sp);
        FlightTunerLogic.tick(sp);
        PhaseShieldLogic.tick(sp);
    }
}
