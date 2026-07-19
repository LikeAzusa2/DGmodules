package com.likeazusa2.dgmodules.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.event.CurioCanUnequipEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.CurioDropsEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Lifecycle and Curios event bridge for the host integrity guard. */
public final class HostIntegrityEvents {

    private HostIntegrityEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        reconcile(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        reconcile(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        reconcile(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        HostIntegrityGuard.releasePlayer(event.getOriginal());
        reconcile(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HostIntegrityGuard.releasePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        reconcile(event.getEntity());
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (HostIntegrityGuard.shouldBlockCurioEvent(event.getEntity(), event.getFrom(), event.getTo())) {
            // CurioChangeEvent is informational in this Curios version. The
            // actual veto happens in the DynamicStackHandler mixin.
            return;
        }
        reconcile(event.getEntity());
    }

    @SubscribeEvent
    public static void onCurioCanEquip(CurioCanEquipEvent event) {
        if (HostIntegrityGuard.shouldBlockCurioEvent(event.getEntity(), ItemStack.EMPTY, event.getStack())) {
            event.setEquipResult(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onCurioCanUnequip(CurioCanUnequipEvent event) {
        if (HostIntegrityGuard.shouldBlockCurioEvent(event.getEntity(), event.getStack(), ItemStack.EMPTY)) {
            event.setUnequipResult(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onCurioDrops(CurioDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (var itemEntity : event.getDrops()) {
            if (HostIntegrityGuard.shouldBlockCurioDrop(player, itemEntity.getItem())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        HostIntegrityGuard.clearAll();
    }

    private static void reconcile(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            HostIntegrityGuard.requestReconcile(serverPlayer);
        }
    }
}
