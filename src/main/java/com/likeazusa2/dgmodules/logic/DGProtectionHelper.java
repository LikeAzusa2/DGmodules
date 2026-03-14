package com.likeazusa2.dgmodules.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;

/**
 * 为一个受击生物解析负责提供防护模块的玩家。
 */
public final class DGProtectionHelper {

    private static final double ALLY_SEARCH_RANGE = 32.0D;

    private DGProtectionHelper() {
    }

    public static ServerPlayer findProtectingPlayer(LivingEntity target) {
        if (target instanceof ServerPlayer sp && !sp.isSpectator()) {
            return sp;
        }

        if (target instanceof TamableAnimal tamable && tamable.getOwner() instanceof ServerPlayer owner && !owner.isSpectator()) {
            return owner;
        }

        AABB searchBox = target.getBoundingBox().inflate(ALLY_SEARCH_RANGE);
        return target.level()
                .getEntitiesOfClass(ServerPlayer.class, searchBox, player ->
                        !player.isSpectator() &&
                                player.isAlive() &&
                                (target.isAlliedTo(player) || player.isAlliedTo(target))
                )
                .stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(target)))
                .orElse(null);
    }
}
