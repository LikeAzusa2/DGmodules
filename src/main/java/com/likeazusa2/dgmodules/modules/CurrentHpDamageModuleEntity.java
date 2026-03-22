package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;

/**
 * 当前生命值伤害模块实体（只负责序列化/复制，不在 tick 里消耗能量）
 */
public class CurrentHpDamageModuleEntity extends ModuleEntity<NoData> {

    // ==== CODEC / STREAM_CODEC（跟 DefaultModuleEntity 同格式：module + gridx + gridy）====
    public CurrentHpDamageModuleEntity(Module<NoData> module) {
        super(module);
    }

    public CurrentHpDamageModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        return new CurrentHpDamageModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
    }

    @Override
    public void tick(ModuleContext ctx) {
        // ✅ 事件驱动模块：tick 不做任何事
        // 伤害与扣能量逻辑放在 LivingDamageEvent.Pre 里
    }
}
