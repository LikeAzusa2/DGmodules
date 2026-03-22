package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;

public class PhaseShieldModuleEntity extends ModuleEntity<NoData> {

    public PhaseShieldModuleEntity(Module<NoData> module) {
        super(module);
    }

    public PhaseShieldModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        return new PhaseShieldModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
    }

    public static boolean hostHasPhaseShield(ModuleHost host) {
        for (var ent : host.getModuleEntities()) {
            if (ent instanceof PhaseShieldModuleEntity) return true;
        }
        return false;
    }
}
