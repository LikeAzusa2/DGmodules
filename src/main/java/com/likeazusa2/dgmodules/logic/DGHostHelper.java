package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import net.minecraft.world.item.ItemStack;

public final class DGHostHelper {
    private DGHostHelper() {
    }

    public static ModuleHost getHost(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(DECapabilities.MODULE_HOST_CAPABILITY).orElse(null);
    }
}
