package com.likeazusa2.dgmodules.mixin;

import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.draconicevolution.api.modules.lib.ModularOPStorage;
import com.likeazusa2.dgmodules.logic.EnergySaverLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在通用 OPStorage 扣能入口上拦截 ModularOPStorage 的负向改动，
 * 这样可以覆盖防具、武器、工具、弓以及本模组内部通过 ctx.getOpStorage() 的统一扣能逻辑。
 */
@Mixin(value = OPStorage.class, remap = false)
public abstract class MixinOPStorageEnergySaver {

    @Unique
    private long dgmodules$rawOutgoingCost = -1;

    @Unique
    private long dgmodules$adjustedOutgoingCost = -1;

    @ModifyVariable(method = "modifyEnergyStored", at = @At("HEAD"), argsOnly = true)
    private long dgmodules$applyEnergySaver(long amount) {
        dgmodules$rawOutgoingCost = -1;
        dgmodules$adjustedOutgoingCost = -1;

        if (amount >= 0) {
            return amount;
        }

        if (!((Object) this instanceof ModularOPStorage modularStorage)) {
            return amount;
        }

        dgmodules$rawOutgoingCost = -amount;
        dgmodules$adjustedOutgoingCost = EnergySaverLogic.scaleModularOutgoingCost(modularStorage, -amount);
        return -dgmodules$adjustedOutgoingCost;
    }

    @Inject(method = "modifyEnergyStored", at = @At("RETURN"), cancellable = true)
    private void dgmodules$normalizeReportedCost(long amount, CallbackInfoReturnable<Long> cir) {
        if (dgmodules$rawOutgoingCost <= 0 || dgmodules$adjustedOutgoingCost <= 0) {
            return;
        }

        long paid = cir.getReturnValue();

        // 只在“折扣后的实际扣能完整支付成功”时，把返回值恢复成原始请求值。
        // 这样上层逻辑仍会认为本次支付成功，但底层实际扣掉的是节能后的能量。
        if (paid >= dgmodules$adjustedOutgoingCost) {
            cir.setReturnValue(dgmodules$rawOutgoingCost);
        }
    }
}
