package com.likeazusa2.dgmodules.modules;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.ModuleCategory;
import com.brandon3055.draconicevolution.api.modules.ModuleType;
import com.brandon3055.draconicevolution.api.modules.data.ModuleProperties;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;

import java.util.Set;

public class ShieldControlBoosterModuleType implements ModuleType<NoData> {

    public static final ShieldControlBoosterModuleType INSTANCE = new ShieldControlBoosterModuleType();

    public static final ModuleProperties<NoData> PROPERTIES =
            new ModuleProperties<>(TechLevel.CHAOTIC, m -> new NoData());

    private ShieldControlBoosterModuleType() {}

    @Override
    public Set<ModuleCategory> getCategories() {
        return Set.of(
                ModuleCategory.ARMOR,
                ModuleCategory.ARMOR_CHEST,
                ModuleCategory.CHESTPIECE
        );
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
    public String getName() {
        return "shield_control_booster";
    }

    @Override
    public ModuleEntity<NoData> createEntity(Module<NoData> module) {
        return new ShieldControlBoosterModuleEntity(module);
    }
}
