package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext;
import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 为非玩家生物补上模块宿主 tick。
 * 这样像负面效果免疫这类“宿主自己生效”的模块，只要生物自己穿戴了宿主装备，就能在该生物身上工作。
 */
@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NonPlayerModuleHostTicker {

    private NonPlayerModuleHostTicker() {
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }
        if (living instanceof net.minecraft.server.level.ServerPlayer) {
            return;
        }

        tickSlot(living, EquipmentSlot.HEAD);
        tickSlot(living, EquipmentSlot.CHEST);
        tickSlot(living, EquipmentSlot.LEGS);
        tickSlot(living, EquipmentSlot.FEET);
        tickSlot(living, EquipmentSlot.MAINHAND);
        tickSlot(living, EquipmentSlot.OFFHAND);
    }

    private static void tickSlot(LivingEntity living, EquipmentSlot slot) {
        ItemStack stack = living.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return;
        }

        try {
            ModuleHost host = DGHostHelper.getHost(stack);
            if (host == null || !DGHostLocator.hostHasAnyDGModule(host)) {
                return;
            }

            host.handleTick(new StackModuleContext(stack, living, slot));
        } catch (Throwable ignored) {
        }
    }
}
