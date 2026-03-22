package com.likeazusa2.dgmodules.modules;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.ModuleCategory;
import com.brandon3055.draconicevolution.api.modules.ModuleType;
import com.brandon3055.draconicevolution.api.modules.data.ModuleProperties;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ChaosLaserModuleType implements ModuleType<NoData> {

    public static final ChaosLaserModuleType INSTANCE = new ChaosLaserModuleType();

    public static final ModuleProperties<NoData> PROPERTIES =
            new ModuleProperties<>(TechLevel.CHAOTIC, m -> new NoData());

    private ChaosLaserModuleType() {}

    @Override
    public @NotNull Set<ModuleCategory> getCategories() {
        return Set.of(
                ModuleCategory.RANGED_WEAPON,
                ModuleCategory.TOOL_AXE,
                ModuleCategory.TOOL_HOE,
                ModuleCategory.TOOL_SHOVEL
        );
    }

    @Override
    public int getDefaultWidth() {
        return 4;
    }

    @Override
    public int getDefaultHeight() {
        return 4;
    }

    @Override
    public String getName() {
        return "chaos_laser";
    }

    @Override
    public ModuleEntity<NoData> createEntity(Module<NoData> module) {
        return new ChaosLaserModuleEntity(module);
    }
}
