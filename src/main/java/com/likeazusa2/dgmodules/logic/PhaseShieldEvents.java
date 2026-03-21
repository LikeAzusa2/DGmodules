package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = DGModules.MODID, bus = EventBusSubscriber.Bus.GAME)
public class PhaseShieldEvents {

    private static final String TAG_LAST_GUARD_TICK = "dg_dragon_guard_tick";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingWhileActive(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            return;
        }
        if (!PhaseShieldLogic.isActive(target)) {
            return;
        }

        event.setCanceled(true);
        PhaseShieldLogic.playShieldHit(target);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePreEmergency(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            return;
        }
        if (PhaseShieldLogic.isActive(target)) {
            return;
        }

        long now = target.serverLevel().getGameTime();
        long lastGuard = target.getPersistentData().getLong(TAG_LAST_GUARD_TICK);
        if (now - lastGuard <= 1) {
            return;
        }

        float finalDamage = event.getNewDamage();
        if (finalDamage <= 0) {
            return;
        }

        float hp = target.getHealth() + target.getAbsorptionAmount();
        if (finalDamage < hp) {
            return;
        }

        if (PhaseShieldLogic.tryActivateEmergency(target)) {
            event.setNewDamage(0);
            PhaseShieldLogic.playShieldHit(target);
            target.invulnerableTime = Math.max(target.invulnerableTime, 2);
            target.hurtMarked = true;
        }
    }
}
