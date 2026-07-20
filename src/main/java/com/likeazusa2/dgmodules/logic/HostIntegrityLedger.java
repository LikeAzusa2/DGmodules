package com.likeazusa2.dgmodules.logic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent authority ledger for stable host UUIDs and deferred returns. */
public final class HostIntegrityLedger extends SavedData {

    private static final String DATA_NAME = "dgmodules_host_integrity";
    private static final Factory<HostIntegrityLedger> FACTORY =
            new Factory<>(HostIntegrityLedger::new, HostIntegrityLedger::load);

    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<UUID, List<ItemStack>> pendingReturns = new HashMap<>();
    private long nextSequence = 1L;

    public static HostIntegrityLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public UUID resolveOwner(UUID stableUuid, UUID proposedOwner) {
        Entry entry = entries.get(stableUuid);
        if (entry == null) {
            entries.put(stableUuid, new Entry(proposedOwner, nextSequence++, "", 0L));
            setDirty();
            return proposedOwner;
        }
        return entry.owner;
    }

    public UUID getOwner(UUID stableUuid) {
        Entry entry = entries.get(stableUuid);
        return entry == null ? null : entry.owner;
    }

    public String getCanonicalLocation(UUID stableUuid) {
        Entry entry = entries.get(stableUuid);
        return entry == null ? "" : entry.location;
    }

    public void observe(UUID stableUuid, UUID owner, String location, long gameTime) {
        Entry entry = entries.get(stableUuid);
        if (entry == null) {
            entries.put(stableUuid, new Entry(owner, nextSequence++, location, gameTime));
            setDirty();
        } else {
            boolean persistentChange = !entry.owner.equals(owner)
                    || !entry.location.equals(location)
                    || gameTime - entry.lastSeen >= 1200L;
            entry.owner = owner;
            entry.location = location;
            if (persistentChange) {
                entry.lastSeen = gameTime;
                setDirty();
            }
        }
    }

    public void queueReturn(UUID owner, ItemStack stack) {
        pendingReturns.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(stack.copy());
        setDirty();
    }

    public List<ItemStack> takeReturns(UUID owner) {
        List<ItemStack> returns = pendingReturns.remove(owner);
        if (returns == null) {
            return List.of();
        }
        setDirty();
        return returns;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("next_sequence", nextSequence);

        ListTag entryList = new ListTag();
        for (Map.Entry<UUID, Entry> mapEntry : entries.entrySet()) {
            CompoundTag value = new CompoundTag();
            value.putUUID("uuid", mapEntry.getKey());
            value.putUUID("owner", mapEntry.getValue().owner);
            value.putLong("sequence", mapEntry.getValue().sequence);
            value.putString("location", mapEntry.getValue().location);
            value.putLong("last_seen", mapEntry.getValue().lastSeen);
            entryList.add(value);
        }
        tag.put("entries", entryList);

        ListTag returnList = new ListTag();
        for (Map.Entry<UUID, List<ItemStack>> pending : pendingReturns.entrySet()) {
            for (ItemStack stack : pending.getValue()) {
                CompoundTag value = new CompoundTag();
                value.putUUID("owner", pending.getKey());
                value.put("stack", stack.save(registries));
                returnList.add(value);
            }
        }
        tag.put("pending_returns", returnList);
        return tag;
    }

    private static HostIntegrityLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        HostIntegrityLedger ledger = new HostIntegrityLedger();
        ledger.nextSequence = Math.max(1L, tag.getLong("next_sequence"));

        for (Tag raw : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) raw;
            if (!value.hasUUID("uuid") || !value.hasUUID("owner")) {
                continue;
            }
            UUID uuid = value.getUUID("uuid");
            UUID owner = value.getUUID("owner");
            ledger.entries.put(uuid, new Entry(
                    owner,
                    value.getLong("sequence"),
                    value.getString("location"),
                    value.getLong("last_seen")));
        }

        for (Tag raw : tag.getList("pending_returns", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) raw;
            if (!value.hasUUID("owner") || !value.contains("stack")) {
                continue;
            }
            ItemStack stack = ItemStack.parse(registries, value.get("stack")).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                ledger.pendingReturns.computeIfAbsent(value.getUUID("owner"), ignored -> new ArrayList<>())
                        .add(stack);
            }
        }
        return ledger;
    }

    private static final class Entry {
        private UUID owner;
        private final long sequence;
        private String location;
        private long lastSeen;

        private Entry(UUID owner, long sequence, String location, long lastSeen) {
            this.owner = owner;
            this.sequence = sequence;
            this.location = location;
            this.lastSeen = lastSeen;
        }
    }
}
