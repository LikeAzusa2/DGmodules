package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;

/**
 * 节能模块实体。
 * 该模块只提供静态减耗数据，不需要在 tick 中执行额外逻辑。
 */
public class EnergySaverModuleEntity extends ModuleEntity<NoData> {

    public EnergySaverModuleEntity(Module<NoData> module) {
        super(module);
    }

    public EnergySaverModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        return new EnergySaverModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
    }

    @Override
    public void tick(ModuleContext ctx) {
        // 节能效果由 OPStorage mixin 在实际扣能时统一处理，这里不需要额外逻辑。
    }
}
