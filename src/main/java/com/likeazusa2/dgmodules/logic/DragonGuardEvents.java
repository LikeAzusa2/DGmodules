package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext;
import com.brandon3055.draconicevolution.handlers.DESounds;
import com.likeazusa2.dgmodules.DGConfig;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.DragonGuardModuleEntity;
import com.likeazusa2.dgmodules.network.NetworkHandler;
import com.likeazusa2.dgmodules.network.S2CDragonGuardWarn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonGuardEvents {
    private static final String TAG_LAST_GUARD_TICK = "dg_dragon_guard_tick";

    private DragonGuardEvents() {
    }

    private static long getCost() {
        return DGConfig.SERVER.dragonGuardCost.get();
    }

    @SubscribeEvent
    public static void onDamagePre(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            return;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
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

        long now = serverLevel.getGameTime();
        if (alreadyTriggeredRecently(target, now)) {
            return;
        }

        ItemStack hostStack = DGHostLocator.findChestLikeHost(target, DragonGuardModuleEntity::hostHasDragonGuard);
        if (hostStack.isEmpty()) {
            return;
        }

        ModuleHost host = DGHostHelper.getHost(hostStack);
        if (host == null || !DragonGuardModuleEntity.hostHasDragonGuard(host)) {
            return;
        }

        StackModuleContext ctx = new StackModuleContext(hostStack, target, EquipmentSlot.CHEST);
        if (!DragonGuardModuleEntity.extractOp(ctx, getCost())) {
            return;
        }

        markTriggered(target, now);
        target.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                DESounds.SHIELD_STRIKE.get(),
                SoundSource.PLAYERS,
                1.0f,
                0.85f
        );
        NetworkHandler.sendToPlayer(target, new S2CDragonGuardWarn(20));

        event.setAmount(0);
        target.setHealth(Math.min(target.getMaxHealth(), 1.0F));
        target.hurtMarked = true;
        target.invulnerableTime = Math.max(target.invulnerableTime, 2);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            return;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        long now = serverLevel.getGameTime();
        boolean alreadyPaid = alreadyTriggeredRecently(target, now);

        ItemStack hostStack = DGHostLocator.findChestLikeHost(target, DragonGuardModuleEntity::hostHasDragonGuard);
        if (hostStack.isEmpty()) {
            return;
        }

        ModuleHost host = DGHostHelper.getHost(hostStack);
        if (host == null || !DragonGuardModuleEntity.hostHasDragonGuard(host)) {
            return;
        }

        if (!alreadyPaid) {
            StackModuleContext ctx = new StackModuleContext(hostStack, target, EquipmentSlot.CHEST);
            if (!DragonGuardModuleEntity.extractOp(ctx, getCost())) {
                return;
            }
            markTriggered(target, now);
            NetworkHandler.sendToPlayer(target, new S2CDragonGuardWarn(20));
        }

        event.setCanceled(true);
        target.setHealth(Math.min(target.getMaxHealth(), 1.0F));
        target.hurtMarked = true;
        target.invulnerableTime = Math.max(target.invulnerableTime, 2);
    }

    private static boolean alreadyTriggeredRecently(ServerPlayer entity, long now) {
        long last = entity.getPersistentData().getLong(TAG_LAST_GUARD_TICK);
        return (now - last) <= 1;
    }

    private static void markTriggered(ServerPlayer entity, long now) {
        entity.getPersistentData().putLong(TAG_LAST_GUARD_TICK, now);
    }
}
