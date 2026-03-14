package com.likeazusa2.dgmodules.util;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;

import java.util.function.Supplier;

public interface DGModularOPStorageAccess {
    Supplier<ModuleHost> dgmodules$getHostSupplier();
}
