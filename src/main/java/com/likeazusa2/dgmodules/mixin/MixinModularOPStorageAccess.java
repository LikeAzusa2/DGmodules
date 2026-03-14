package com.likeazusa2.dgmodules.mixin;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.lib.ModularOPStorage;
import com.likeazusa2.dgmodules.util.DGModularOPStorageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(value = ModularOPStorage.class, remap = false)
public interface MixinModularOPStorageAccess extends DGModularOPStorageAccess {

    @Override
    @Accessor("hostSupplier")
    Supplier<ModuleHost> dgmodules$getHostSupplier();
}
