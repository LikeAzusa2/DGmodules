package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.lib.ModularOPStorage;
import com.likeazusa2.dgmodules.modules.EnergySaverModule;
import com.likeazusa2.dgmodules.modules.EnergySaverModuleType;
import com.likeazusa2.dgmodules.util.DGModularOPStorageAccess;

import java.util.function.Supplier;

/**
 * 节能模块结算逻辑。
 */
public final class EnergySaverLogic {

    private EnergySaverLogic() {
    }

    /**
     * 统计宿主上全部节能模块的减耗比例总和。
     */
    public static float getTotalSaving(ModuleHost host) {
        final float[] total = {0.0f};
        host.getEntitiesByType(EnergySaverModuleType.INSTANCE).forEach(entity -> {
            if (entity.getModule() instanceof EnergySaverModule module) {
                total[0] += module.getSavingMultiplier();
            }
        });
        return total[0];
    }

    /**
     * 把原始耗能转换为应用节能模块后的最终耗能。
     * 这里采用“剩余倍率”结算：final = base * max(0, 1 - totalSaving)。
     */
    public static long applySaving(long rawCost, ModuleHost host) {
        if (rawCost <= 0) {
            return 0;
        }

        float remainingMultiplier = Math.max(0.0f, 1.0f - getTotalSaving(host));
        long adjusted = Math.round(rawCost * remainingMultiplier);
        return adjusted <= 0 ? 1 : adjusted;
    }

    /**
     * 对 ModularOPStorage 的负向扣能统一应用节能模块倍率。
     */
    public static long scaleModularOutgoingCost(ModularOPStorage storage, long rawCost) {
        if (rawCost <= 0) {
            return rawCost;
        }

        Supplier<ModuleHost> hostSupplier = ((DGModularOPStorageAccess) storage).dgmodules$getHostSupplier();
        if (hostSupplier == null) {
            return rawCost;
        }

        ModuleHost host = hostSupplier.get();
        if (host == null) {
            return rawCost;
        }

        try (host) {
            return applySaving(rawCost, host);
        }
    }
}
