package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.entity.DraconicShieldDomeCoreEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;

public class DraconicShieldDomeEvents {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        DraconicShieldDomeCoreEntity dome = findDomeContainingPlayer(player);
        if (dome == null) {
            return;
        }

        if (dome.absorbDamageFromShield(event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    public static DraconicShieldDomeCoreEntity findDomeContainingPlayer(Player player) {
        AABB searchBox = player.getBoundingBox().inflate(16.0D);
        return player.level().getEntitiesOfClass(DraconicShieldDomeCoreEntity.class, searchBox, DraconicShieldDomeCoreEntity::isAlive)
                .stream()
                .filter(dome -> player.distanceTo(dome) <= dome.getDomeRadius())
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }
}
