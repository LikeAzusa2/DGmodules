package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.ModuleCategory;
import com.brandon3055.draconicevolution.api.modules.ModuleType;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * 节能模块类型。
 * 覆盖所有常见可穿戴防具位，以及手持武器、工具、弓等模块宿主。
 */
public class EnergySaverModuleType implements ModuleType<NoData> {

    public static final EnergySaverModuleType INSTANCE = new EnergySaverModuleType();

    private EnergySaverModuleType() {
    }

    @Override
    public int maxInstallable() {
        // 给足堆叠空间，但仍保留一个合理上限，避免宿主被纯节能模块完全塞满。
        return 9;
    }

    @Override
    public @NotNull Set<ModuleCategory> getCategories() {
        return Set.of(
                ModuleCategory.ARMOR,
                ModuleCategory.ARMOR_HEAD,
                ModuleCategory.ARMOR_CHEST,
                ModuleCategory.ARMOR_LEGS,
                ModuleCategory.ARMOR_FEET,
                ModuleCategory.CHESTPIECE,
                ModuleCategory.MELEE_WEAPON,
                ModuleCategory.RANGED_WEAPON,
                ModuleCategory.MINING_TOOL,
                ModuleCategory.TOOL_AXE,
                ModuleCategory.TOOL_SHOVEL,
                ModuleCategory.TOOL_HOE
        );
    }

    @Override
    public int getDefaultWidth() {
        return 1;
    }

    @Override
    public int getDefaultHeight() {
        return 1;
    }

    @Override
    public String getName() {
        return "energy_saver";
    }

    @Override
    public ModuleEntity<NoData> createEntity(Module<NoData> module) {
        return new EnergySaverModuleEntity(module);
    }
}
