package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Shared target filter for dangerous module damage.
 *
 * The switch is intentionally server-side and is checked at the moment a
 * target is selected or damaged, so config reloads also affect active effects.
 */
public final class DangerousModuleProtection {

    private DangerousModuleProtection() {
    }

    public static boolean canAffect(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (DGConfig.SERVER.dangerousModulesAffectCreativePlayers.get()) return true;
        return !(target instanceof Player player) || !player.isCreative();
    }
}
