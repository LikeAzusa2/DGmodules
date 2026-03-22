package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PhaseShieldEvents {

    private static final String TAG_LAST_GUARD_TICK = "dg_dragon_guard_tick";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingWhileActive(LivingAttackEvent event) {
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
    public static void onDamagePreEmergency(LivingHurtEvent event) {
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

        float finalDamage = event.getAmount();
        if (finalDamage <= 0) {
            return;
        }

        float hp = target.getHealth() + target.getAbsorptionAmount();
        if (finalDamage < hp) {
            return;
        }

        if (PhaseShieldLogic.tryActivateEmergency(target)) {
            event.setAmount(0);
            PhaseShieldLogic.playShieldHit(target);
            target.invulnerableTime = Math.max(target.invulnerableTime, 2);
            target.hurtMarked = true;
        }
    }
}
