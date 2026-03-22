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

/**
 * Dragon Guard (龙之守护) ModuleType for DE 1.21.1.
 *
 * Pattern copied from {@link ChaosLaserModuleType}.
 */
public class DragonGuardModuleType implements ModuleType<NoData> {

    public static final DragonGuardModuleType INSTANCE = new DragonGuardModuleType();

    public static final ModuleProperties<NoData> PROPERTIES =
            new ModuleProperties<>(TechLevel.CHAOTIC, m -> new NoData());

    private DragonGuardModuleType() {}

    @Override
    public @NotNull Set<ModuleCategory> getCategories() {
        return Set.of(
                ModuleCategory.ARMOR,
                ModuleCategory.ARMOR_CHEST,
                ModuleCategory.CHESTPIECE
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
        return "dragon_guard";
    }

    @Override
    public ModuleEntity<NoData> createEntity(Module<NoData> module) {
        return new DragonGuardModuleEntity(module);
    }
}
