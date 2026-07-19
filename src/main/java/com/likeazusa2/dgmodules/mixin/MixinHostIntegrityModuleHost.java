package com.likeazusa2.dgmodules.mixin;

import com.brandon3055.draconicevolution.api.modules.lib.ModuleHostImpl;
import com.likeazusa2.dgmodules.logic.HostIntegrityGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Allows DE's own host serialization to update a protected host atomically. */
@Mixin(value = ModuleHostImpl.class, remap = false)
public abstract class MixinHostIntegrityModuleHost {

    @Inject(method = "saveData", at = @At("HEAD"))
    private void dgmodules$beginSave(CallbackInfo ci) {
        HostIntegrityGuard.beginInternalMutation();
    }

    @Inject(method = "saveData", at = @At("RETURN"))
    private void dgmodules$endSave(CallbackInfo ci) {
        HostIntegrityGuard.endInternalMutation();
    }
}
