package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = DGModules.MODID, bus = EventBusSubscriber.Bus.GAME)
public class PhaseShieldEvents {

    // 与 DragonGuardEvents 保持一致，用于避免同 tick 抢触发。
    private static final String TAG_LAST_GUARD_TICK = "dg_dragon_guard_tick";

    /**
     * 相位护盾已开启时，最早取消 incoming damage。
     * 这既保护玩家本人，也保护由该玩家防护模块负责的宠物 / allied 生物。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingWhileActive(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        ServerPlayer protector = DGProtectionHelper.findProtectingPlayer(target);
        if (protector == null) return;

        if (!PhaseShieldLogic.isActive(protector)) return;

        event.setCanceled(true);
        PhaseShieldLogic.playShieldHit(target);
    }

    /**
     * 被动应急启动。
     * 玩家自身与其宠物 / allied 生物在致死前都会尝试用玩家自己的相位护盾兜底。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePreEmergency(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        ServerPlayer protector = DGProtectionHelper.findProtectingPlayer(target);
        if (protector == null) return;

        if (PhaseShieldLogic.isActive(protector)) return;

        long now = protector.serverLevel().getGameTime();
        long lastGuard = protector.getPersistentData().getLong(TAG_LAST_GUARD_TICK);
        if (now - lastGuard <= 1) return;

        float finalDamage = event.getNewDamage();
        if (finalDamage <= 0) return;

        float hp = target.getHealth() + target.getAbsorptionAmount();
        if (finalDamage < hp) return;

        if (PhaseShieldLogic.tryActivateEmergency(protector)) {
            event.setNewDamage(0);
            PhaseShieldLogic.playShieldHit(target);
            target.invulnerableTime = Math.max(target.invulnerableTime, 2);
            target.hurtMarked = true;
        }
    }

    /**
     * 非玩家实体没有玩家那套死亡 mixin，这里补一个死亡事件兜底。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathFallback(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof ServerPlayer) return;

        ServerPlayer protector = DGProtectionHelper.findProtectingPlayer(target);
        if (protector == null) return;

        long now = protector.serverLevel().getGameTime();
        long lastGuard = protector.getPersistentData().getLong(TAG_LAST_GUARD_TICK);
        if (now - lastGuard <= 1) return;

        if (!(PhaseShieldLogic.isActive(protector) || PhaseShieldLogic.tryActivateEmergency(protector))) {
            return;
        }

        event.setCanceled(true);
        target.setHealth(Math.min(target.getMaxHealth(), 1.0F));
        target.invulnerableTime = Math.max(target.invulnerableTime, 2);
        target.hurtMarked = true;
        PhaseShieldLogic.playShieldHit(target);
    }
}
