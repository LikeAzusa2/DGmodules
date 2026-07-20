package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import com.brandon3055.draconicevolution.init.DEModules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * State holder for {@link HostIntegrityModule}.
 *
 * <p>The entity deliberately has no per-tick work. Its presence in a host is
 * the opt-in marker used by {@code HostIntegrityGuard}.</p>
 */
public class HostIntegrityModuleEntity extends ModuleEntity<NoData> {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(UUID.fromString(value));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Invalid host integrity UUID: " + value);
                }
            },
            UUID::toString
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_STREAM_CODEC =
            StreamCodec.of(
                    (buffer, uuid) -> {
                        buffer.writeLong(uuid.getMostSignificantBits());
                        buffer.writeLong(uuid.getLeastSignificantBits());
                    },
                    buffer -> new UUID(buffer.readLong(), buffer.readLong())
            );

    private UUID moduleUuid;
    private UUID ownerUuid;
    private boolean uuidNeedsPersistence;

    public static final Codec<HostIntegrityModuleEntity> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DEModules.codec().fieldOf("module").forGetter(e -> (Module<?>) e.getModule()),
            Codec.INT.fieldOf("gridx").forGetter(ModuleEntity::getGridX),
            Codec.INT.fieldOf("gridy").forGetter(ModuleEntity::getGridY),
            UUID_CODEC.optionalFieldOf("uuid").forGetter(e -> Optional.of(e.getModuleUuid())),
            UUID_CODEC.optionalFieldOf("owner").forGetter(e -> Optional.ofNullable(e.getOwnerUuid()))
    ).apply(inst, (m, x, y, uuid, owner) -> new HostIntegrityModuleEntity(
            (Module<NoData>) m, x, y, uuid.orElse(null), owner.orElse(null), uuid.isEmpty())));

    public static final StreamCodec<RegistryFriendlyByteBuf, HostIntegrityModuleEntity> STREAM_CODEC =
            StreamCodec.composite(
                    DEModules.streamCodec(), e -> (Module<?>) e.getModule(),
                    ByteBufCodecs.INT, ModuleEntity::getGridX,
                    ByteBufCodecs.INT, ModuleEntity::getGridY,
                    UUID_STREAM_CODEC, HostIntegrityModuleEntity::getModuleUuid,
                    ByteBufCodecs.optional(UUID_STREAM_CODEC), e -> Optional.ofNullable(e.getOwnerUuid()),
                    (m, x, y, uuid, owner) -> new HostIntegrityModuleEntity(
                            (Module<NoData>) m, x, y, uuid, owner.orElse(null), false)
            );

    public HostIntegrityModuleEntity(Module<NoData> module) {
        this(module, 0, 0, UUID.randomUUID(), null, false);
    }

    public HostIntegrityModuleEntity(Module<NoData> module, int gridX, int gridY) {
        this(module, gridX, gridY, UUID.randomUUID(), null, false);
    }

    private HostIntegrityModuleEntity(Module<NoData> module, int gridX, int gridY,
                                      UUID moduleUuid, UUID ownerUuid, boolean uuidNeedsPersistence) {
        super(module, gridX, gridY);
        this.moduleUuid = moduleUuid == null ? UUID.randomUUID() : moduleUuid;
        this.ownerUuid = ownerUuid;
        this.uuidNeedsPersistence = uuidNeedsPersistence || moduleUuid == null;
    }

    public UUID getModuleUuid() {
        return moduleUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        if (!java.util.Objects.equals(this.ownerUuid, ownerUuid)) {
            this.ownerUuid = ownerUuid;
            uuidNeedsPersistence = true;
            markDirty();
        }
    }

    /**
     * Gives a copied/legacy host a new identity without replacing the module
     * entity itself. The host is marked dirty by ModuleEntity.markDirty().
     */
    public void regenerateUuid() {
        moduleUuid = UUID.randomUUID();
        uuidNeedsPersistence = true;
        markDirty();
    }

    public boolean needsUuidPersistence() {
        return uuidNeedsPersistence;
    }

    public void markUuidPersisted() {
        uuidNeedsPersistence = false;
    }

    @Override
    public ModuleEntity<?> copy() {
        return new HostIntegrityModuleEntity((Module<NoData>) this.module, getGridX(), getGridY(),
                moduleUuid, ownerUuid, uuidNeedsPersistence);
    }

    @Override
    public void tick(ModuleContext ctx) {
        // Event/mixin driven by design. Do not add polling here.
    }
}
