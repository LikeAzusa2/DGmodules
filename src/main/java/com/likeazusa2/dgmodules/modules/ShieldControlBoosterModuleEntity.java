package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;

public class ShieldControlBoosterModuleEntity extends ModuleEntity<NoData> {

    public ShieldControlBoosterModuleEntity(Module<NoData> module) {
        super(module);
    }

    public ShieldControlBoosterModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        return new ShieldControlBoosterModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
    }
}
