package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

public class NegativeEffectImmunityModuleEntity extends ModuleEntity<NoData> {

    public NegativeEffectImmunityModuleEntity(Module<NoData> module) {
        super(module);
    }

    public NegativeEffectImmunityModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        return new NegativeEffectImmunityModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
    }

    @Override
    public void tick(ModuleContext context) {
        if (!(context instanceof StackModuleContext ctx)) return;

        LivingEntity living = ctx.getEntity();
        if (living == null || living.level().isClientSide) return;

        for (MobEffectInstance effect : new ArrayList<>(living.getActiveEffects())) {
            MobEffect mobEffect = effect.getEffect();
            if (mobEffect.getCategory() == MobEffectCategory.HARMFUL) {
                living.removeEffect(mobEffect);
            }
        }
    }
}
