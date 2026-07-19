package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.init.ItemData;
import com.likeazusa2.dgmodules.DGConfig;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.HostIntegrityModule;
import com.likeazusa2.dgmodules.modules.HostIntegrityModuleEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side, event-driven protection for a host carrying Host Integrity.
 *
 * <p>This class intentionally has no tick method. It keeps identity maps for
 * the currently active equipment/hand/Curios stacks and vetoes writes at the
 * write boundary. Reconciliation is only called by lifecycle or mutation
 * events.</p>
 */
public final class HostIntegrityGuard {

    private static final Object LOCK = new Object();
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static final IdentityHashMap<ItemStack, Binding> PROTECTED_STACKS = new IdentityHashMap<>();
    private static final IdentityHashMap<ItemStack, ActiveBinding> ACTIVE_STACKS = new IdentityHashMap<>();
    private static final Map<UUID, ItemStack> ACTIVE_MODULE_UUIDS = new HashMap<>();
    private static final IdentityHashMap<NonNullList<?>, ListBinding> INVENTORY_LISTS = new IdentityHashMap<>();
    private static final IdentityHashMap<Object, CurioBinding> CURIO_HANDLERS = new IdentityHashMap<>();
    private static final Map<UUID, Long> LAST_LOG_TICK = new HashMap<>();

    private static final ThreadLocal<ActionState> ACTION = new ThreadLocal<>();
    private static final ThreadLocal<Integer> INTERNAL_MUTATION_DEPTH =
            ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Deque<InventoryMutation>> INVENTORY_MUTATIONS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Boolean> RECONCILING =
            ThreadLocal.withInitial(() -> false);

    private HostIntegrityGuard() {
    }

    /** Rebuilds only the specified player's active host bindings. */
    public static void reconcilePlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide) {
            return;
        }
        if (RECONCILING.get()) {
            return;
        }

        RECONCILING.set(true);
        try {
            synchronized (LOCK) {
                detach(serverPlayer.getUUID());

                PlayerState state = new PlayerState(serverPlayer);
                STATES.put(serverPlayer.getUUID(), state);

                if (DGConfig.SERVER.hostIntegrityProtectArmor.get()) {
                    registerList(state, serverPlayer.getInventory().armor, ListKind.ARMOR);
                    registerActive(state, serverPlayer, EquipmentSlot.HEAD);
                    registerActive(state, serverPlayer, EquipmentSlot.CHEST);
                    registerActive(state, serverPlayer, EquipmentSlot.LEGS);
                    registerActive(state, serverPlayer, EquipmentSlot.FEET);
                }

                if (DGConfig.SERVER.hostIntegrityProtectHands.get()) {
                    registerList(state, serverPlayer.getInventory().items, ListKind.ITEMS);
                    registerList(state, serverPlayer.getInventory().offhand, ListKind.OFFHAND);
                    registerActive(state, serverPlayer, EquipmentSlot.MAINHAND);
                    registerActive(state, serverPlayer, EquipmentSlot.OFFHAND);
                } else if (DGConfig.SERVER.hostIntegrityProtectInventory.get()) {
                    registerList(state, serverPlayer.getInventory().items, ListKind.ITEMS);
                }

                if (DGConfig.SERVER.hostIntegrityProtectInventory.get()) {
                    if (!containsIdentity(state.lists, serverPlayer.getInventory().items)) {
                        registerList(state, serverPlayer.getInventory().items, ListKind.ITEMS);
                    }
                    registerAllInventoryItems(state, serverPlayer);
                }

                if (DGConfig.SERVER.hostIntegrityProtectCurios.get() && ModList.get().isLoaded("curios")) {
                    registerCurios(state, serverPlayer);
                }
            }
        } finally {
            RECONCILING.remove();
        }
    }

    public static void releasePlayer(Player player) {
        if (player != null) {
            synchronized (LOCK) {
                detach(player.getUUID());
            }
        }
    }

    public static void clearAll() {
        synchronized (LOCK) {
            for (UUID uuid : new ArrayList<>(STATES.keySet())) {
                detach(uuid);
            }
            ACTIVE_MODULE_UUIDS.clear();
            LAST_LOG_TICK.clear();
        }
    }

    /** Opens a call-stack scoped owner action for a serverbound player operation. */
    public static void beginOwnerAction(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ActionState state = ACTION.get();
        if (state == null) {
            ACTION.set(new ActionState(player.getUUID(), 1));
        } else if (state.owner.equals(player.getUUID())) {
            state.depth++;
        } else {
            // The server processes a player's packet on one thread. Do not
            // inherit an action from another player if a mod nests calls.
            ACTION.set(new ActionState(player.getUUID(), 1));
        }
    }

    public static void endOwnerAction(ServerPlayer player) {
        ActionState state = ACTION.get();
        if (state == null || player == null || !state.owner.equals(player.getUUID())) {
            return;
        }

        state.depth--;
        if (state.depth <= 0) {
            boolean reconcile = state.reconcileRequested;
            ACTION.remove();
            if (reconcile) {
                reconcilePlayer(player);
            }
        }
    }

    /** Requests one deferred binding rebuild for the current owner action. */
    public static void requestReconcile(ServerPlayer player) {
        if (player == null || RECONCILING.get()) {
            return;
        }

        ActionState state = ACTION.get();
        if (state != null && state.owner.equals(player.getUUID())) {
            state.reconcileRequested = true;
        } else {
            reconcilePlayer(player);
        }
    }

    public static boolean isOwnerAction(Player player) {
        ActionState state = ACTION.get();
        return state != null && player != null && state.owner.equals(player.getUUID());
    }

    public static void beginInternalMutation() {
        INTERNAL_MUTATION_DEPTH.set(INTERNAL_MUTATION_DEPTH.get() + 1);
    }

    public static void endInternalMutation() {
        int depth = INTERNAL_MUTATION_DEPTH.get() - 1;
        if (depth <= 0) {
            INTERNAL_MUTATION_DEPTH.remove();
        } else {
            INTERNAL_MUTATION_DEPTH.set(depth);
        }
    }

    public static boolean isInternalMutation() {
        return INTERNAL_MUTATION_DEPTH.get() > 0;
    }

    /** Called from the NonNullList#set write barrier. */
    public static boolean shouldBlockInventoryWrite(NonNullList<?> list, int index, ItemStack replacement) {
        if (!(replacement instanceof ItemStack newStack)) {
            return false;
        }

        ListBinding binding;
        ItemStack oldStack;
        synchronized (LOCK) {
            binding = INVENTORY_LISTS.get(list);
            if (binding == null || index < 0 || index >= list.size() || !isRelevant(binding, index)) {
                return false;
            }
            Object old = list.get(index);
            if (!(old instanceof ItemStack)) {
                return false;
            }
            oldStack = (ItemStack) old;
        }

        if (oldStack == newStack) {
            return false;
        }

        boolean protectedOld = isProtectedStack(oldStack) || hasIntegrityModule(oldStack);
        boolean protectedNew = hasIntegrityModule(newStack);
        if (!protectedOld && !protectedNew) {
            return false;
        }
        if (isOwnerAction(binding.player)) {
            return false;
        }

        logBlocked(binding.player, "inventory replacement", binding.slotName(index));
        return true;
    }

    public static boolean shouldBlockInventoryRemove(NonNullList<?> list, int index) {
        ListBinding binding;
        ItemStack oldStack;
        synchronized (LOCK) {
            binding = INVENTORY_LISTS.get(list);
            if (binding == null || index < 0 || index >= list.size() || !isRelevant(binding, index)) {
                return false;
            }
            Object old = list.get(index);
            if (!(old instanceof ItemStack)) {
                return false;
            }
            oldStack = (ItemStack) old;
        }

        if (!isProtectedStack(oldStack) && !hasIntegrityModule(oldStack)) {
            return false;
        }
        if (isOwnerAction(binding.player)) {
            return false;
        }

        logBlocked(binding.player, "inventory removal", binding.slotName(index));
        return true;
    }

    public static boolean shouldBlockInventoryClear(NonNullList<?> list) {
        ListBinding binding;
        synchronized (LOCK) {
            binding = INVENTORY_LISTS.get(list);
            if (binding == null) {
                return false;
            }
        }

        if (isOwnerAction(binding.player)) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            if (!isRelevant(binding, i)) {
                continue;
            }
            Object value = list.get(i);
            if (value instanceof ItemStack stack
                    && (isProtectedStack(stack) || hasIntegrityModule(stack))) {
                logBlocked(binding.player, "inventory clear", binding.slotName(i));
                return true;
            }
        }
        return false;
    }

    /** Records the old stack before a player inventory list write. */
    public static void beginInventoryMutation(NonNullList<?> list, int index, ItemStack replacement) {
        ListBinding binding;
        ItemStack oldStack;
        synchronized (LOCK) {
            binding = INVENTORY_LISTS.get(list);
            if (binding == null || index < 0 || index >= list.size() || !isRelevant(binding, index)) {
                INVENTORY_MUTATIONS.get().push(new InventoryMutation(null, false));
                return;
            }
            Object old = list.get(index);
            oldStack = old instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        }

        boolean watched = isProtectedStack(oldStack)
                || hasIntegrityModule(oldStack)
                || hasIntegrityModule(replacement);
        INVENTORY_MUTATIONS.get().push(new InventoryMutation(binding.player, watched));
    }

    /** Finishes a player inventory list write and only rebuilds when needed. */
    public static void endInventoryMutation() {
        Deque<InventoryMutation> mutations = INVENTORY_MUTATIONS.get();
        if (mutations.isEmpty()) {
            return;
        }

        InventoryMutation mutation = mutations.pop();
        if (mutations.isEmpty()) {
            INVENTORY_MUTATIONS.remove();
        }
        if (mutation.watched && mutation.player != null) {
            requestReconcile(mutation.player);
        }
    }

    /** Records whether clearing a player inventory list needs a rebind. */
    public static void beginInventoryClear(NonNullList<?> list) {
        ListBinding binding;
        synchronized (LOCK) {
            binding = INVENTORY_LISTS.get(list);
        }
        if (binding == null) {
            INVENTORY_MUTATIONS.get().push(new InventoryMutation(null, false));
            return;
        }

        boolean watched = false;
        for (int i = 0; i < list.size(); i++) {
            if (!isRelevant(binding, i)) {
                continue;
            }
            Object value = list.get(i);
            if (value instanceof ItemStack stack
                    && (isProtectedStack(stack) || hasIntegrityModule(stack))) {
                watched = true;
                break;
            }
        }
        INVENTORY_MUTATIONS.get().push(new InventoryMutation(binding.player, watched));
    }

    /** Called by ItemStack component mixins before the component map is changed. */
    public static boolean allowComponentMutation(ItemStack stack, DataComponentType<?> type) {
        Binding binding = getBinding(stack);
        if (binding == null) {
            return true;
        }
        if (isOwnerAction(binding.player) || isInternalMutation()) {
            return true;
        }
        if (isRuntimeComponent(type)) {
            return true;
        }

        logBlocked(binding.player, "item component/NBT mutation", binding.slotName);
        return false;
    }

    public static boolean allowComponentsPatch(ItemStack stack) {
        Binding binding = getBinding(stack);
        if (binding == null) {
            return true;
        }
        if (isOwnerAction(binding.player) || isInternalMutation()) {
            return true;
        }

        logBlocked(binding.player, "item component patch", binding.slotName);
        return false;
    }

    public static boolean allowCountMutation(ItemStack stack, int newCount) {
        Binding binding = getBinding(stack);
        if (binding == null || stack.getCount() == newCount) {
            return true;
        }
        if (isOwnerAction(binding.player) || isInternalMutation()) {
            return true;
        }

        logBlocked(binding.player, "item count mutation", binding.slotName);
        return false;
    }

    public static boolean shouldBlockSelectedDrop(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ItemStack selected = player.getInventory().getSelected();
        if (!isProtectedStack(selected) || isOwnerAction(player)) {
            return false;
        }

        logBlocked(player, "selected item drop", "mainhand");
        return true;
    }

    public static boolean shouldBlockPlayerDrop(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !isProtectedStack(stack)
                || isOwnerAction(serverPlayer)) {
            return false;
        }

        Binding binding = getBinding(stack);
        logBlocked(serverPlayer, "item entity drop", binding == null ? "unknown" : binding.slotName);
        return true;
    }

    public static boolean shouldBlockItemEntityAssignment(ItemStack stack) {
        Binding binding = getBinding(stack);
        if (binding == null || isOwnerAction(binding.player)) {
            return false;
        }

        logBlocked(binding.player, "item entity assignment", binding.slotName);
        return true;
    }

    /** Reconcile after an allowed structural component change. */
    public static void onComponentMutation(ItemStack stack, DataComponentType<?> type) {
        ActiveBinding binding;
        synchronized (LOCK) {
            binding = ACTIVE_STACKS.get(stack);
        }
        if (binding == null) {
            return;
        }

        boolean ownerAction = isOwnerAction(binding.player);
        if (isInternalMutation()) {
            // DE updates energy/durability internally outside a player
            // packet. Do not rebuild bindings for those high-frequency writes.
            // A module GUI operation is owner-authorized and must refresh the
            // binding after installing/removing the guard module.
            if (!ownerAction || isRuntimeComponent(type)) {
                return;
            }
        } else if (isRuntimeComponent(type)) {
            return;
        }

        requestReconcile(binding.player);
    }

    public static boolean isProtectedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        synchronized (LOCK) {
            return PROTECTED_STACKS.containsKey(stack);
        }
    }

    public static boolean shouldBlockCurioWrite(Object handler, int slot, ItemStack replacement) {
        CurioBinding binding;
        ItemStack oldStack;
        synchronized (LOCK) {
            binding = CURIO_HANDLERS.get(handler);
            if (binding == null || !(handler instanceof ItemStackHandler itemHandler)
                    || slot < 0 || slot >= itemHandler.getSlots()) {
                return false;
            }
            oldStack = itemHandler.getStackInSlot(slot);
        }

        if (oldStack == replacement) {
            return false;
        }
        boolean protectedOld = isProtectedStack(oldStack) || hasIntegrityModule(oldStack);
        boolean protectedNew = hasIntegrityModule(replacement);
        if (!protectedOld && !protectedNew) {
            return false;
        }
        if (isOwnerAction(binding.player)) {
            return false;
        }

        logBlocked(binding.player, "Curios replacement", binding.slotName + ":" + slot);
        return true;
    }

    public static boolean shouldBlockCurioExtract(Object handler, int slot) {
        CurioBinding binding;
        ItemStack stack;
        synchronized (LOCK) {
            binding = CURIO_HANDLERS.get(handler);
            if (binding == null || !(handler instanceof ItemStackHandler itemHandler)
                    || slot < 0 || slot >= itemHandler.getSlots()) {
                return false;
            }
            stack = itemHandler.getStackInSlot(slot);
        }

        if (!isProtectedStack(stack) && !hasIntegrityModule(stack)) {
            return false;
        }
        if (isOwnerAction(binding.player)) {
            return false;
        }

        logBlocked(binding.player, "Curios extraction", binding.slotName + ":" + slot);
        return true;
    }

    public static void onCurioMutation(Object handler) {
        CurioBinding binding;
        synchronized (LOCK) {
            binding = CURIO_HANDLERS.get(handler);
        }
        if (binding != null) {
            requestReconcile(binding.player);
        }
    }

    public static boolean shouldBlockCurioEvent(LivingEntity wearer, ItemStack from, ItemStack to) {
        if (!(wearer instanceof ServerPlayer player)
                || !DGConfig.SERVER.hostIntegrityProtectCurios.get()) {
            return false;
        }
        if (isOwnerAction(player)) {
            return false;
        }
        if (!isProtectedStack(from) && !hasIntegrityModule(from)
                && !hasIntegrityModule(to)) {
            return false;
        }
        logBlocked(player, "Curios change event", "curios");
        return true;
    }

    public static boolean shouldBlockCurioDrop(LivingEntity wearer, ItemStack stack) {
        if (!(wearer instanceof ServerPlayer player)
                || !DGConfig.SERVER.hostIntegrityProtectCurios.get()) {
            return false;
        }
        if (isOwnerAction(player)) {
            return false;
        }
        if (!isProtectedStack(stack) && !hasIntegrityModule(stack)) {
            return false;
        }
        logBlocked(player, "Curios drop", "curios");
        return true;
    }

    public static boolean isOwnerOfProtectedStack(Player player, ItemStack stack) {
        Binding binding = getBinding(stack);
        return binding != null && player != null && binding.player.getUUID().equals(player.getUUID());
    }

    private static Binding getBinding(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        synchronized (LOCK) {
            return PROTECTED_STACKS.get(stack);
        }
    }

    private static boolean hasIntegrityModule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) {
                return false;
            }
            for (var entity : host.getModuleEntities()) {
                if (entity instanceof HostIntegrityModuleEntity
                        || entity.getModule() instanceof HostIntegrityModule) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // A foreign/non-modular stack is simply not protected.
        }
        return false;
    }

    /**
     * Reads and, when needed, persists the UUID using the same DE host
     * instance. DE creates module-entity copies while loading a host, so
     * mutating an entity obtained from a separately opened host would be lost.
     */
    private static UUID prepareModuleUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) {
                return null;
            }

            HostIntegrityModuleEntity integrity = null;
            for (var entity : host.getModuleEntities()) {
                if (entity instanceof HostIntegrityModuleEntity hostIntegrity) {
                    integrity = hostIntegrity;
                    break;
                }
            }
            if (integrity == null) {
                return null;
            }

            UUID moduleUuid = integrity.getModuleUuid();
            ItemStack existing = ACTIVE_MODULE_UUIDS.get(moduleUuid);
            if (existing != null && existing != stack) {
                do {
                    integrity.regenerateUuid();
                    moduleUuid = integrity.getModuleUuid();
                } while (ACTIVE_MODULE_UUIDS.containsKey(moduleUuid));
            }

            if (integrity.needsUuidPersistence()) {
                beginInternalMutation();
                try {
                    host.markDirty();
                    host.save();
                    integrity.markUuidPersisted();
                } finally {
                    endInternalMutation();
                }
            }
            return moduleUuid;
        } catch (Throwable ignored) {
            // A malformed/foreign host must not break player inventory logic.
            return null;
        }
    }

    private static boolean isRuntimeComponent(DataComponentType<?> type) {
        if (type == DataComponents.DAMAGE) {
            return true;
        }
        try {
            return type == ItemData.MODULAR_ENERGY_CAPABILITY.get()
                    || type == ItemData.ENERGY_CAP_HOLDER.get()
                    || type == ItemData.ENERGY_MODULE_ENERGY.get()
                    || type == ItemData.SHIELD_MODULE_CAP.get()
                    || type == ItemData.SHIELD_MODULE_POINTS.get()
                    || type == ItemData.SHIELD_MODULE_COOLDWN.get()
                    || type == ItemData.UNDYING_MODULE_CHARGE.get()
                    || type == ItemData.AUTO_FEED_MODULE_FOOD.get()
                    || type == ItemData.MAGNET_ACTIVE.get();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isRelevant(ListBinding binding, int index) {
        return switch (binding.kind) {
            case ARMOR -> DGConfig.SERVER.hostIntegrityProtectArmor.get();
            case OFFHAND -> DGConfig.SERVER.hostIntegrityProtectHands.get();
            case ITEMS -> DGConfig.SERVER.hostIntegrityProtectInventory.get()
                    || (DGConfig.SERVER.hostIntegrityProtectHands.get()
                    && index == binding.player.getInventory().selected);
        };
    }

    private static void registerList(PlayerState state, NonNullList<?> list, ListKind kind) {
        if (!containsIdentity(state.lists, list)) {
            state.lists.add(list);
        }
        INVENTORY_LISTS.put(list, new ListBinding(state.player, kind));
    }

    private static boolean containsIdentity(List<NonNullList<?>> lists, NonNullList<?> target) {
        for (NonNullList<?> list : lists) {
            if (list == target) {
                return true;
            }
        }
        return false;
    }

    private static void registerActive(PlayerState state, ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        state.activeStacks.add(stack);
        UUID moduleUuid = prepareModuleUuid(stack);
        ActiveBinding active = new ActiveBinding(player, slot.getName(), moduleUuid);
        ACTIVE_STACKS.put(stack, active);
        if (moduleUuid != null || hasIntegrityModule(stack)) {
            PROTECTED_STACKS.put(stack, new Binding(player, slot.getName(), moduleUuid));
            if (moduleUuid != null) {
                ACTIVE_MODULE_UUIDS.put(moduleUuid, stack);
            }
        }
    }

    private static void registerAllInventoryItems(PlayerState state, ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack != null && !stack.isEmpty()) {
                state.activeStacks.add(stack);
                UUID moduleUuid = prepareModuleUuid(stack);
                ACTIVE_STACKS.put(stack, new ActiveBinding(player, "inventory", moduleUuid));
                if (moduleUuid != null || hasIntegrityModule(stack)) {
                    PROTECTED_STACKS.put(stack, new Binding(player, "inventory", moduleUuid));
                    if (moduleUuid != null) {
                        ACTIVE_MODULE_UUIDS.put(moduleUuid, stack);
                    }
                }
            }
        }
    }

    private static void registerCurios(PlayerState state, ServerPlayer player) {
        try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                for (var entry : handler.getCurios().entrySet()) {
                    String identifier = entry.getKey();
                    var stacks = entry.getValue().getStacks();
                    CurioBinding curioBinding = new CurioBinding(player, identifier);
                    state.curioHandlers.add(stacks);
                    CURIO_HANDLERS.put(stacks, curioBinding);

                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (stack == null || stack.isEmpty()) {
                            continue;
                        }
                        state.activeStacks.add(stack);
                        UUID moduleUuid = prepareModuleUuid(stack);
                        ACTIVE_STACKS.put(stack, new ActiveBinding(player, identifier + ":" + i, moduleUuid));
                        if (moduleUuid != null || hasIntegrityModule(stack)) {
                            PROTECTED_STACKS.put(stack, new Binding(player, identifier + ":" + i, moduleUuid));
                            if (moduleUuid != null) {
                                ACTIVE_MODULE_UUIDS.put(moduleUuid, stack);
                            }
                        }
                    }
                }
            });
        } catch (Throwable ignored) {
            // Curios can be unavailable during early player construction.
        }
    }

    private static void detach(UUID uuid) {
        PlayerState old = STATES.remove(uuid);
        if (old == null) {
            return;
        }

        for (ItemStack stack : old.activeStacks) {
            ACTIVE_STACKS.remove(stack);
            Binding protectedBinding = PROTECTED_STACKS.remove(stack);
            if (protectedBinding != null
                    && ACTIVE_MODULE_UUIDS.get(protectedBinding.moduleUuid) == stack) {
                ACTIVE_MODULE_UUIDS.remove(protectedBinding.moduleUuid);
            }
        }
        for (NonNullList<?> list : old.lists) {
            INVENTORY_LISTS.remove(list);
        }
        for (Object handler : old.curioHandlers) {
            CURIO_HANDLERS.remove(handler);
        }
    }

    private static void logBlocked(ServerPlayer player, String operation, String slot) {
        if (!DGConfig.SERVER.hostIntegrityLogBlockedOperations.get()) {
            return;
        }

        long now = player.serverLevel().getGameTime();
        synchronized (LOCK) {
            long last = LAST_LOG_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE);
            if (now - last < 20) {
                return;
            }
            LAST_LOG_TICK.put(player.getUUID(), now);
        }
        DGModules.LOGGER.warn("[HostIntegrity] blocked {} on {} for player {}", operation, slot,
                player.getGameProfile().getName());
    }

    private static final class PlayerState {
        private final ServerPlayer player;
        private final Set<ItemStack> activeStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<NonNullList<?>> lists = new ArrayList<>();
        private final List<Object> curioHandlers = new ArrayList<>();

        private PlayerState(ServerPlayer player) {
            this.player = player;
        }
    }

    private record Binding(ServerPlayer player, String slotName, UUID moduleUuid) {
    }

    private record ActiveBinding(ServerPlayer player, String slotName, UUID moduleUuid) {
    }

    private record ListBinding(ServerPlayer player, ListKind kind) {
        private String slotName(int index) {
            return kind.name().toLowerCase() + ":" + index;
        }
    }

    private record CurioBinding(ServerPlayer player, String slotName) {
    }

    private enum ListKind {
        ITEMS,
        ARMOR,
        OFFHAND
    }

    private static final class ActionState {
        private final UUID owner;
        private int depth;
        private boolean reconcileRequested;

        private ActionState(UUID owner, int depth) {
            this.owner = owner;
            this.depth = depth;
        }
    }

    private record InventoryMutation(ServerPlayer player, boolean watched) {
    }
}
