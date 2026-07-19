package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.item.ChaosCrystalBreakerItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Routes both direct entity clicks and clicks on the block below a DE guardian
 * crystal into the same installation path.
 */
@EventBusSubscriber(modid = DGModules.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ChaosCrystalBreakerEvents {

    private ChaosCrystalBreakerEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ChaosCrystalBreakerItem)) return;
        if (!(event.getTarget() instanceof GuardianCrystalEntity target)) return;

        var level = event.getLevel();
        var player = event.getEntity();
        if (!level.isClientSide()
                && !ChaosCrystalBreakerLogic.installAndConsume(level, player, stack, target, null)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ChaosCrystalBreakerItem)) return;

        var level = event.getLevel();
        var player = event.getEntity();
        var target = ChaosCrystalBreakerLogic.findTarget(level, player, event.getPos());
        if (target == null) return;

        if (!level.isClientSide()
                && !ChaosCrystalBreakerLogic.installAndConsume(level, player, stack, target, event.getPos())) {
            return;
        }

        // Stop the support block and item interaction chain after installing
        // the charge, preventing a second use/consumption in Item.useOn.
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChaosCrystalBreakerLogic.tick(level);
        }
    }
}
