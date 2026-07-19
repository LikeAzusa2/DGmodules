package com.likeazusa2.dgmodules.modules;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.ModuleCategory;
import com.brandon3055.draconicevolution.api.modules.ModuleType;
import com.brandon3055.draconicevolution.api.modules.data.ModuleProperties;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/** Module type for host integrity protection. */
public class HostIntegrityModuleType implements ModuleType<NoData> {

    public static final HostIntegrityModuleType INSTANCE = new HostIntegrityModuleType();

    public static final ModuleProperties<NoData> PROPERTIES =
            new ModuleProperties<>(TechLevel.CHAOTIC, 2, 2, m -> new NoData());

    private HostIntegrityModuleType() {
    }

    @Override
    public @NotNull Set<ModuleCategory> getCategories() {
        return Set.of(ModuleCategory.ALL);
    }

    @Override
    public int getDefaultWidth() {
        return 2;
    }

    @Override
    public int getDefaultHeight() {
        return 2;
    }

    @Override
    public int maxInstallable() {
        return 1;
    }

    @Override
    public String getName() {
        return "host_integrity";
    }

    @Override
    public ModuleEntity<NoData> createEntity(Module<NoData> module) {
        return new HostIntegrityModuleEntity(module);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Codec<ModuleEntity<?>> entityCodec() {
        return (Codec<ModuleEntity<?>>) (Codec<?>) HostIntegrityModuleEntity.CODEC;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamCodec<RegistryFriendlyByteBuf, ModuleEntity<?>> entityStreamCodec() {
        return (StreamCodec<RegistryFriendlyByteBuf, ModuleEntity<?>>) (StreamCodec<?, ?>)
                HostIntegrityModuleEntity.STREAM_CODEC;
    }
}
