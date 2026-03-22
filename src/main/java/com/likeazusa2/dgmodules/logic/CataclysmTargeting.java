package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

final class CataclysmTargeting {

    private CataclysmTargeting() {}

    @Nullable
    static UUID resolveTrackedTargetId(ServerLevel level, HitResult hit, Vec3 impactPos, @Nullable Entity shooter) {
        if (hit instanceof net.minecraft.world.phys.EntityHitResult ehr) {
            Entity normalized = normalizeTrackedTarget(ehr.getEntity());
            if (normalized == null) return null;
            if (shooter != null && normalized.getUUID().equals(shooter.getUUID())) return null;
            return normalized.getUUID();
        }

        Entity fallback = findPriorityTarget(level, impactPos, shooter);
        return fallback == null ? null : fallback.getUUID();
    }

    @Nullable
    static Entity resolveTrackedTargetEntity(ServerLevel level, @Nullable UUID targetId) {
        if (targetId == null) return null;
        return normalizeTrackedTarget(level.getEntity(targetId));
    }

    static Vec3 resolveOrbAimPos(@Nullable Entity tracked, Vec3 fallbackPos) {
        if (tracked == null) return fallbackPos;
        return tracked.getBoundingBox().getCenter();
    }

    @Nullable
    private static Entity findPriorityTarget(ServerLevel level, Vec3 impactPos, @Nullable Entity shooter) {
        double range = Math.max(8.0, DGConfig.SERVER.cataclysmRadiusXZ.get());
        AABB box = new AABB(
                impactPos.x - range, impactPos.y - range, impactPos.z - range,
                impactPos.x + range, impactPos.y + range, impactPos.z + range
        );

        Entity best = null;
        double bestDist2 = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            Entity normalized = normalizeTrackedTarget(entity);
            if (normalized == null) continue;
            if (shooter != null && normalized.getUUID().equals(shooter.getUUID())) continue;
            double dist2 = normalized.distanceToSqr(impactPos);
            boolean priority = isPriorityBoss(normalized);
            if (priority) {
                if (best == null || !isPriorityBoss(best) || dist2 < bestDist2) {
                    best = normalized;
                    bestDist2 = dist2;
                }
                continue;
            }

            if (best == null || (!isPriorityBoss(best) && dist2 < bestDist2)) {
                best = normalized;
                bestDist2 = dist2;
            }
        }

        return best;
    }

    private static boolean isPriorityBoss(Entity entity) {
        if (entity instanceof EnderDragon) return true;

        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String reg = key == null ? "" : (key.getNamespace() + ":" + key.getPath()).toLowerCase();
        String cls = entity.getClass().getName().toLowerCase();

        return reg.contains("chaos_guardian")
                || reg.contains("chaosguardian")
                || reg.contains("draconic_guardian")
                || cls.contains("draconicguardian")
                || cls.contains("draconic_guardian");
    }

    @Nullable
    private static Entity normalizeTrackedTarget(@Nullable Entity raw) {
        if (raw == null || !raw.isAlive()) return null;
        Entity reflectedDragon = resolveDragonFromPartReflective(raw);
        if (reflectedDragon != null && reflectedDragon.isAlive()) {
            return reflectedDragon;
        }

        if (isDragonPartEntity(raw) && raw.level() instanceof ServerLevel level) {
            EnderDragon nearestDragon = null;
            double bestDist2 = Double.MAX_VALUE;
            for (EnderDragon dragon : level.getEntitiesOfClass(EnderDragon.class, raw.getBoundingBox().inflate(96.0), LivingEntity::isAlive)) {
                double dist2 = dragon.distanceToSqr(raw);
                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    nearestDragon = dragon;
                }
            }
            if (nearestDragon != null) {
                return nearestDragon;
            }
        }
        return raw;
    }

    private static boolean isDragonPartEntity(Entity raw) {
        String id = raw.getType().toString().toLowerCase();
        String className = raw.getClass().getName().toLowerCase();
        return id.contains("ender_dragon_part")
                || id.contains("dragon_part")
                || className.contains("enderdragonpart")
                || className.contains("dragonpart");
    }

    @Nullable
    private static Entity resolveDragonFromPartReflective(Entity raw) {
        Class<?> c = raw.getClass();
        while (c != null && c != Object.class) {
            for (String fieldName : List.of("parentMob", "parent")) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object parent = f.get(raw);
                    if (parent instanceof EnderDragon dragon) {
                        return dragon;
                    }
                    if (parent instanceof LivingEntity living) {
                        return living;
                    }
                    if (parent instanceof Entity entity) {
                        return entity;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            c = c.getSuperclass();
        }

        for (String methodName : List.of("getParent", "parentMob", "getParentMob")) {
            try {
                Method m = raw.getClass().getMethod(methodName);
                m.setAccessible(true);
                Object parent = m.invoke(raw);
                if (parent instanceof EnderDragon dragon) {
                    return dragon;
                }
                if (parent instanceof Entity entity && entity.getType().toString().toLowerCase().contains("ender_dragon")) {
                    return entity;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }
}
