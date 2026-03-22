package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.ShieldControlBoosterModuleEntity;
import com.likeazusa2.dgmodules.util.DGShieldIFrames;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShieldControlBoosterEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackIFrames(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasBooster(player) || !DGShieldIFrames.inIFrames(player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurtIFrames(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasBooster(player) || !DGShieldIFrames.inIFrames(player)) {
            return;
        }

        event.setAmount(0.0F);
    }

    private static boolean hasBooster(ServerPlayer player) {
        return !DGHostLocator.findChestLikeHost(player, host -> {
            for (var ent : host.getModuleEntities()) {
                if (ent instanceof ShieldControlBoosterModuleEntity) {
                    return true;
                }
            }
            return false;
        }).isEmpty();
    }
}
