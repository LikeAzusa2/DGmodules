package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.likeazusa2.dgmodules.DGConfig;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.HostIntegrityModuleEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative monitor for stable host identities.
 *
 * <p>Only real inventory, Curios, carried-stack and item-entity locations are
 * inspected. Temporary serialization and client copies are never registered
 * as authoritative instances.</p>
 */
public final class HostIntegrityMonitor {

    private static final Set<UUID> SCANNING_PLAYERS = new HashSet<>();
    private static final Set<UUID> QUEUED_PLAYERS = new HashSet<>();
    private static final Map<UUID, ItemEntity> GROUND_HOSTS = new HashMap<>();

    private HostIntegrityMonitor() {
    }

    public static void requestScan(ServerPlayer player) {
        if (player != null && player.getServer() != null && QUEUED_PLAYERS.add(player.getUUID())) {
            player.getServer().execute(() -> {
                QUEUED_PLAYERS.remove(player.getUUID());
                scanPlayer(player);
            });
        }
    }

    public static void scanPlayer(ServerPlayer player) {
        if (player == null || player.isRemoved() || !SCANNING_PLAYERS.add(player.getUUID())) {
            return;
        }

        boolean changed = false;
        try {
            HostIntegrityLedger ledger = HostIntegrityLedger.get(player.getServer());
            List<ObservedHost> observed = collectPlayerHosts(player, ledger);
            Map<UUID, List<ObservedHost>> grouped = new LinkedHashMap<>();
            for (ObservedHost host : observed) {
                grouped.computeIfAbsent(host.identity.stableUuid, ignored -> new ArrayList<>()).add(host);
            }

            for (Map.Entry<UUID, List<ObservedHost>> entry : grouped.entrySet()) {
                UUID stableUuid = entry.getKey();
                List<ObservedHost> hosts = entry.getValue();
                ItemEntity groundHost = GROUND_HOSTS.get(stableUuid);
                if (groundHost != null && !groundHost.isRemoved()) {
                    for (ObservedHost host : hosts) {
                        ItemStack removed = host.location.remove();
                        if (!removed.isEmpty()) {
                            changed = true;
                            logRemoval(stableUuid, player, host.location.name,
                                    "later duplicate of ground authority");
                        }
                    }
                    continue;
                }
                UUID owner = ledger.getOwner(stableUuid);
                if (owner == null) {
                    owner = ledger.resolveOwner(stableUuid, player.getUUID());
                }

                if (!owner.equals(player.getUUID())) {
                    for (ObservedHost host : hosts) {
                        ItemStack removed = host.location.remove();
                        if (!removed.isEmpty()) {
                            returnToOwner(player.getServer(), owner, removed, player.position());
                            changed = true;
                            logRemoval(stableUuid, player, host.location.name, "wrong owner");
                        }
                    }
                    continue;
                }

                ObservedHost canonical = selectCanonical(hosts, ledger.getCanonicalLocation(stableUuid));
                for (ObservedHost host : hosts) {
                    if (host == canonical) {
                        continue;
                    }
                    ItemStack removed = host.location.remove();
                    if (!removed.isEmpty()) {
                        changed = true;
                        logRemoval(stableUuid, player, host.location.name, "later duplicate");
                    }
                }
                ledger.observe(stableUuid, owner, canonical.location.name,
                        player.serverLevel().getGameTime());
            }
        } finally {
            SCANNING_PLAYERS.remove(player.getUUID());
        }

        HostIntegrityGuard.reconcilePlayer(player);
        if (changed) {
            syncPlayer(player);
        }
    }

    public static void deliverPendingReturns(ServerPlayer player) {
        HostIntegrityLedger ledger = HostIntegrityLedger.get(player.getServer());
        Set<UUID> delivered = new HashSet<>();
        for (ItemStack stack : ledger.takeReturns(player.getUUID())) {
            UUID stableUuid = readStableUuid(stack);
            if (stableUuid != null && (!delivered.add(stableUuid) || playerHasUuid(player, stableUuid))) {
                DGModules.LOGGER.warn(
                        "[HostIntegrity] removed pending duplicate {} because owner {} already has it",
                        stableUuid, player.getGameProfile().getName());
                continue;
            }
            if (!insertIntoInventory(player, stack)) {
                spawnFloatingReturn(player, stack);
            }
        }
        syncPlayer(player);
        requestScan(player);
    }

    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        UUID stableUuid = readStableUuid(stack);
        if (stableUuid == null) {
            return;
        }
        if (stack.getCount() != 1) {
            stack.setCount(1);
        }

        HostIntegrityLedger ledger = HostIntegrityLedger.get(level.getServer());
        UUID owner = readOwnerUuid(stack);
        UUID registeredOwner = ledger.getOwner(stableUuid);
        if (registeredOwner != null) {
            owner = registeredOwner;
        } else if (owner != null) {
            owner = ledger.resolveOwner(stableUuid, owner);
        }

        if (owner != null) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
            if (ownerPlayer != null && playerHasUuid(ownerPlayer, stableUuid)) {
                event.setCanceled(true);
                DGModules.LOGGER.warn(
                        "[HostIntegrity] rejected ground duplicate {} because owner {} still has the host",
                        stableUuid, ownerPlayer.getGameProfile().getName());
                return;
            }

            String canonicalLocation = ledger.getCanonicalLocation(stableUuid);
            String entityLocation = "item_entity:" + itemEntity.getUUID();
            if (canonicalLocation.startsWith("item_entity:")
                    && !canonicalLocation.equals(entityLocation)) {
                event.setCanceled(true);
                DGModules.LOGGER.warn(
                        "[HostIntegrity] rejected later ground instance {} entity={} canonical={}",
                        stableUuid, itemEntity.getUUID(), canonicalLocation);
                return;
            }

            ItemEntity existingEntity = GROUND_HOSTS.putIfAbsent(stableUuid, itemEntity);
            if (existingEntity != null && existingEntity != itemEntity && !existingEntity.isRemoved()) {
                event.setCanceled(true);
                DGModules.LOGGER.warn(
                        "[HostIntegrity] rejected duplicate ground instance {} entity={} existing={}",
                        stableUuid, itemEntity.getUUID(), existingEntity.getUUID());
                return;
            }
            GROUND_HOSTS.put(stableUuid, itemEntity);
            itemEntity.setTarget(owner);
            itemEntity.setExtendedLifetime();
            ledger.observe(stableUuid, owner, entityLocation, level.getGameTime());
        }
    }

    public static void onItemEntityLeave(ItemEntity itemEntity) {
        UUID stableUuid = readStableUuid(itemEntity.getItem());
        if (stableUuid != null) {
            GROUND_HOSTS.remove(stableUuid, itemEntity);
        }
    }

    public static void clearRuntimeState() {
        SCANNING_PLAYERS.clear();
        QUEUED_PLAYERS.clear();
        GROUND_HOSTS.clear();
    }

    public static void onPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        UUID stableUuid = readStableUuid(stack);
        if (stableUuid == null) {
            return;
        }

        HostIntegrityLedger ledger = HostIntegrityLedger.get(player.getServer());
        UUID owner = ledger.getOwner(stableUuid);
        if (owner == null) {
            UUID storedOwner = readOwnerUuid(stack);
            owner = ledger.resolveOwner(stableUuid,
                    storedOwner == null ? player.getUUID() : storedOwner);
            persistOwner(stack, owner);
        }

        if (!owner.equals(player.getUUID())) {
            event.setCanPickup(TriState.FALSE);
            ItemStack returning = stack.copy();
            itemEntity.discard();
            returnToOwner(player.getServer(), owner, returning, player.position());
            DGModules.LOGGER.warn(
                    "[HostIntegrity] denied pickup of {} by non-owner {}",
                    stableUuid, player.getGameProfile().getName());
            return;
        }

        if (playerHasUuid(player, stableUuid)) {
            event.setCanPickup(TriState.FALSE);
            itemEntity.discard();
            DGModules.LOGGER.warn(
                    "[HostIntegrity] removed later ground duplicate {} for owner {}",
                    stableUuid, player.getGameProfile().getName());
            return;
        }

        // Stable hosts are moved manually so a foreign pickup implementation
        // can not add the stack without consuming the ItemEntity (or vice
        // versa). A full inventory leaves the protected entity on the ground.
        event.setCanPickup(TriState.FALSE);
        if (hasEmptyInventorySlot(player)) {
            itemEntity.discard();
            if (insertIntoInventory(player, stack)) {
                ledger.observe(stableUuid, owner, "inventory:pickup",
                        player.serverLevel().getGameTime());
                syncPlayer(player);
                requestScan(player);
            } else {
                spawnFloatingReturn(player, stack);
            }
        }
    }

    public static UUID readStableUuid(ItemStack stack) {
        Identity identity = readIdentity(stack);
        return identity == null ? null : identity.stableUuid;
    }

    private static UUID readOwnerUuid(ItemStack stack) {
        Identity identity = readIdentity(stack);
        return identity == null ? null : identity.ownerUuid;
    }

    private static Identity readIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) {
                return null;
            }
            for (var entity : host.getModuleEntities()) {
                if (entity instanceof HostIntegrityModuleEntity integrity) {
                    return new Identity(integrity.getModuleUuid(), integrity.getOwnerUuid());
                }
            }
        } catch (Throwable ignored) {
            // Foreign or malformed module hosts are not stable hosts.
        }
        return null;
    }

    private static List<ObservedHost> collectPlayerHosts(ServerPlayer player,
                                                          HostIntegrityLedger ledger) {
        List<ObservedHost> result = new ArrayList<>();
        Set<ItemStack> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        addList(result, visited, player, ledger, player.getInventory().items, "inventory");
        addList(result, visited, player, ledger, player.getInventory().armor, "armor");
        addList(result, visited, player, ledger, player.getInventory().offhand, "offhand");

        ItemStack carried = player.containerMenu.getCarried();
        addObserved(result, visited, player, ledger, carried,
                new HostLocation("carried", () -> player.containerMenu.getCarried(),
                        stack -> player.containerMenu.setCarried(stack)));

        if (DGConfig.SERVER.hostIntegrityProtectCurios.get() && ModList.get().isLoaded("curios")) {
            try {
                CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                    for (var entry : handler.getCurios().entrySet()) {
                        String identifier = entry.getKey();
                        addCurios(result, visited, player, ledger, entry.getValue().getStacks(),
                                "curios:" + identifier);
                        addCurios(result, visited, player, ledger, entry.getValue().getCosmeticStacks(),
                                "curios_cosmetic:" + identifier);
                    }
                });
            } catch (Throwable ignored) {
                // Curios may be unavailable during player construction.
            }
        }
        return result;
    }

    private static void addList(List<ObservedHost> result, Set<ItemStack> visited,
                                ServerPlayer player, HostIntegrityLedger ledger,
                                NonNullList<ItemStack> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            int slot = i;
            addObserved(result, visited, player, ledger, list.get(i),
                    new HostLocation(name + ":" + i, () -> list.get(slot), stack -> list.set(slot, stack)));
        }
    }

    private static void addCurios(List<ObservedHost> result, Set<ItemStack> visited,
                                  ServerPlayer player, HostIntegrityLedger ledger,
                                  IDynamicStackHandler handler, String name) {
        for (int i = 0; i < handler.getSlots(); i++) {
            int slot = i;
            addObserved(result, visited, player, ledger, handler.getStackInSlot(i),
                    new HostLocation(name + ":" + i, () -> handler.getStackInSlot(slot),
                            stack -> handler.setStackInSlot(slot, stack)));
        }
    }

    private static void addObserved(List<ObservedHost> result, Set<ItemStack> visited,
                                    ServerPlayer player, HostIntegrityLedger ledger,
                                    ItemStack stack, HostLocation location) {
        if (stack == null || stack.isEmpty() || !visited.add(stack)) {
            return;
        }
        Identity identity = bindIdentity(stack, player, ledger);
        if (identity != null) {
            if (stack.getCount() != 1) {
                HostIntegrityGuard.beginInternalMutation();
                try {
                    stack.setCount(1);
                } finally {
                    HostIntegrityGuard.endInternalMutation();
                }
            }
            result.add(new ObservedHost(identity, location));
        }
    }

    private static Identity bindIdentity(ItemStack stack, ServerPlayer holder,
                                         HostIntegrityLedger ledger) {
        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) {
                return null;
            }
            for (var entity : host.getModuleEntities()) {
                if (!(entity instanceof HostIntegrityModuleEntity integrity)) {
                    continue;
                }
                UUID stableUuid = integrity.getModuleUuid();
                UUID ledgerOwner = ledger.getOwner(stableUuid);
                UUID owner = ledgerOwner != null
                        ? ledgerOwner
                        : ledger.resolveOwner(stableUuid,
                        integrity.getOwnerUuid() == null ? holder.getUUID() : integrity.getOwnerUuid());
                if (!owner.equals(integrity.getOwnerUuid())) {
                    integrity.setOwnerUuid(owner);
                    HostIntegrityGuard.beginInternalMutation();
                    try {
                        host.markDirty();
                        host.save();
                        integrity.markUuidPersisted();
                    } finally {
                        HostIntegrityGuard.endInternalMutation();
                    }
                }
                return new Identity(stableUuid, owner);
            }
        } catch (Throwable ignored) {
            // Malformed hosts are ignored without breaking inventory scans.
        }
        return null;
    }

    private static void persistOwner(ItemStack stack, UUID owner) {
        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) {
                return;
            }
            for (var entity : host.getModuleEntities()) {
                if (entity instanceof HostIntegrityModuleEntity integrity
                        && !owner.equals(integrity.getOwnerUuid())) {
                    integrity.setOwnerUuid(owner);
                    HostIntegrityGuard.beginInternalMutation();
                    try {
                        host.markDirty();
                        host.save();
                        integrity.markUuidPersisted();
                    } finally {
                        HostIntegrityGuard.endInternalMutation();
                    }
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static ObservedHost selectCanonical(List<ObservedHost> hosts, String canonicalLocation) {
        for (ObservedHost host : hosts) {
            if (host.location.name.equals(canonicalLocation)) {
                return host;
            }
        }
        return hosts.getFirst();
    }

    private static void returnToOwner(MinecraftServer server, UUID ownerUuid,
                                      ItemStack stack, Vec3 fallbackPosition) {
        UUID stableUuid = readStableUuid(stack);
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            HostIntegrityLedger.get(server).queueReturn(ownerUuid, stack);
            return;
        }
        if (stableUuid != null && playerHasUuid(owner, stableUuid)) {
            DGModules.LOGGER.warn(
                    "[HostIntegrity] cleared returned duplicate {} because owner {} already has it",
                    stableUuid, owner.getGameProfile().getName());
            return;
        }
        if (!insertIntoInventory(owner, stack)) {
            spawnFloatingReturn(owner, stack);
        }
        syncPlayer(owner);
        requestScan(owner);
    }

    private static boolean insertIntoInventory(ServerPlayer player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (player.getInventory().items.get(i).isEmpty()) {
                HostIntegrityGuard.beginInternalMutation();
                try {
                    player.getInventory().items.set(i, stack);
                    player.getInventory().setChanged();
                } finally {
                    HostIntegrityGuard.endInternalMutation();
                }
                return true;
            }
        }
        return false;
    }

    private static boolean hasEmptyInventorySlot(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void spawnFloatingReturn(ServerPlayer owner, ItemStack stack) {
        ServerLevel level = owner.serverLevel();
        UUID stableUuid = readStableUuid(stack);
        if (stableUuid != null) {
            HostIntegrityLedger.get(owner.getServer()).observe(
                    stableUuid, owner.getUUID(), "returning:" + owner.getUUID(), level.getGameTime());
        }
        Vec3 position = owner.getEyePosition().add(owner.getLookAngle().scale(1.75));
        HostIntegrityGuard.beginInternalMutation();
        try {
            ItemEntity itemEntity = new ItemEntity(level, position.x, position.y, position.z, stack);
            itemEntity.setNoGravity(true);
            itemEntity.setDeltaMovement(Vec3.ZERO);
            itemEntity.setGlowingTag(true);
            itemEntity.setTarget(owner.getUUID());
            itemEntity.setPickUpDelay(10);
            itemEntity.setUnlimitedLifetime();
            level.addFreshEntity(itemEntity);
        } finally {
            HostIntegrityGuard.endInternalMutation();
        }
    }

    private static boolean playerHasUuid(ServerPlayer player, UUID stableUuid) {
        Set<ItemStack> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack stack : player.getInventory().items) {
            if (matchesUuid(visited, stack, stableUuid)) return true;
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (matchesUuid(visited, stack, stableUuid)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (matchesUuid(visited, stack, stableUuid)) return true;
        }
        if (matchesUuid(visited, player.containerMenu.getCarried(), stableUuid)) return true;

        if (DGConfig.SERVER.hostIntegrityProtectCurios.get() && ModList.get().isLoaded("curios")) {
            try {
                var optional = CuriosApi.getCuriosInventory(player);
                if (optional.isPresent()) {
                    for (var entry : optional.get().getCurios().values()) {
                        if (handlerHasUuid(visited, entry.getStacks(), stableUuid)
                                || handlerHasUuid(visited, entry.getCosmeticStacks(), stableUuid)) {
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean handlerHasUuid(Set<ItemStack> visited, IDynamicStackHandler handler,
                                          UUID stableUuid) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (matchesUuid(visited, handler.getStackInSlot(i), stableUuid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesUuid(Set<ItemStack> visited, ItemStack stack, UUID stableUuid) {
        return stack != null && !stack.isEmpty() && visited.add(stack)
                && stableUuid.equals(readStableUuid(stack));
    }

    private static void syncPlayer(ServerPlayer player) {
        player.containerMenu.broadcastFullState();
        if (player.inventoryMenu != player.containerMenu) {
            player.inventoryMenu.broadcastFullState();
        }
        player.getInventory().setChanged();
    }

    private static void logRemoval(UUID stableUuid, ServerPlayer player,
                                   String location, String reason) {
        DGModules.LOGGER.warn(
                "[HostIntegrity] removed {} stable host uuid={} player={} location={}",
                reason, stableUuid, player.getGameProfile().getName(), location);
    }

    private record Identity(UUID stableUuid, UUID ownerUuid) {
    }

    private record ObservedHost(Identity identity, HostLocation location) {
    }

    @FunctionalInterface
    private interface StackSetter {
        void set(ItemStack stack);
    }

    @FunctionalInterface
    private interface StackGetter {
        ItemStack get();
    }

    private static final class HostLocation {
        private final String name;
        private final StackGetter getter;
        private final StackSetter setter;

        private HostLocation(String name, StackGetter getter, StackSetter setter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
        }

        private ItemStack remove() {
            ItemStack current = getter.get();
            if (current == null || current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            HostIntegrityGuard.beginInternalMutation();
            try {
                setter.set(ItemStack.EMPTY);
            } finally {
                HostIntegrityGuard.endInternalMutation();
            }
            return current;
        }
    }
}
