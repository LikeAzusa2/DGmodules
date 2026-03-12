package com.likeazusa2.dgmodules.logic;

import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CataclysmChaosOrbLogic {

    private static final int CHAOS_ORB_COUNT = 3;
    private static final int CHAOS_ORB_INTERVAL_TICKS = 3;
    private static final int CHAOS_ORB_MAX_LIFETIME_TICKS = 40;
    private static final int CHAOS_ORB_EXPLOSION_STAMP_TTL_TICKS = 2;
    private static final double CHAOS_ORB_SPAWN_HEIGHT = 18.0;
    private static final double CHAOS_ORB_SPAWN_RADIUS = 10.0;
    private static final double CHAOS_ORB_SPEED = 1.45;
    private static final double CHAOS_ORB_TARGET_SPEED_FACTOR = 1.35;
    private static final double CHAOS_ORB_TARGET_SPEED_BONUS = 1.15;
    private static final double CHAOS_ORB_TARGET_SPEED_BONUS_MAX = 1.25;
    private static final double CHAOS_ORB_TARGET_LEAD_TICKS = 3.0;
    private static final List<String> CHAOS_ORB_ENTITY_IDS = List.of(
            "draconicevolution:guardian_projectile",
            "draconicevolution:chaos_guardian_projectile",
            "draconicevolution:chaos_guardian_fireball"
    );

    private static final Map<UUID, ChaosOrbBarrageState> ACTIVE_CHAOS_BARRAGES = new HashMap<>();
    private static final Map<UUID, ChaosOrbFlightState> ACTIVE_CHAOS_ORBS = new HashMap<>();
    private static final Deque<ChaosOrbExplosionStamp> RECENT_CHAOS_ORB_EXPLOSIONS = new ArrayDeque<>();

    private static boolean warnedMissingChaosOrbType = false;

    private record ChaosOrbExplosionStamp(ResourceKey<Level> dimension, Vec3 pos, long tick) {}
    private record ChaosOrbBarrageState(ResourceKey<Level> levelKey, Vec3 impactPos, UUID shooterId, UUID targetId, long nextSpawnTick, int spawnedCount) {}
    private record ChaosOrbFlightState(ResourceKey<Level> levelKey, Vec3 fallbackPos, UUID targetId, double speed, long expireGameTime) {}

    private CataclysmChaosOrbLogic() {}

    static void spawnChaosGuardianOrbs(ServerLevel level, Vec3 impactPos, @Nullable ServerPlayer shooter, @Nullable UUID targetId) {
        ACTIVE_CHAOS_BARRAGES.put(
                UUID.randomUUID(),
                new ChaosOrbBarrageState(
                        level.dimension(),
                        impactPos,
                        shooter == null ? null : shooter.getUUID(),
                        targetId,
                        level.getGameTime(),
                        0
                )
        );
    }

    static void processChaosOrbBarrages(ServerLevel currentLevel, long now) {
        EntityType<?> orbType = resolveChaosOrbType();
        if (orbType == null) {
            if (!warnedMissingChaosOrbType) {
                warnedMissingChaosOrbType = true;
                DGModules.LOGGER.warn("Cataclysm: cannot find chaos guardian projectile entity type. Tried {}", CHAOS_ORB_ENTITY_IDS);
            }
            return;
        }

        Iterator<Map.Entry<UUID, ChaosOrbBarrageState>> it = ACTIVE_CHAOS_BARRAGES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ChaosOrbBarrageState> e = it.next();
            ChaosOrbBarrageState state = e.getValue();
            if (!state.levelKey().equals(currentLevel.dimension())) continue;

            if (state.spawnedCount() >= CHAOS_ORB_COUNT) {
                it.remove();
                continue;
            }
            if (now < state.nextSpawnTick()) continue;

            Entity shooter = state.shooterId() == null ? null : currentLevel.getEntity(state.shooterId());
            spawnSingleChaosOrb(currentLevel, orbType, state.impactPos(), shooter, state.targetId(), state.spawnedCount());

            int spawned = state.spawnedCount() + 1;
            if (spawned >= CHAOS_ORB_COUNT) {
                it.remove();
            } else {
                e.setValue(new ChaosOrbBarrageState(
                        state.levelKey(),
                        state.impactPos(),
                        state.shooterId(),
                        state.targetId(),
                        now + CHAOS_ORB_INTERVAL_TICKS,
                        spawned
                ));
            }
        }
    }

    static void processChaosOrbs(ServerLevel currentLevel, long now) {
        Iterator<Map.Entry<UUID, ChaosOrbFlightState>> it = ACTIVE_CHAOS_ORBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ChaosOrbFlightState> e = it.next();
            ChaosOrbFlightState state = e.getValue();
            if (!state.levelKey().equals(currentLevel.dimension())) continue;

            Entity orb = currentLevel.getEntity(e.getKey());
            if (orb == null || !orb.isAlive()) {
                it.remove();
                continue;
            }

            if (now >= state.expireGameTime()) {
                orb.discard();
                it.remove();
                continue;
            }

            Entity trackedTarget = CataclysmTargeting.resolveTrackedTargetEntity(currentLevel, state.targetId());
            Vec3 aimPos = CataclysmTargeting.resolveOrbAimPos(trackedTarget, state.fallbackPos());
            Vec3 targetVel = trackedTarget == null ? Vec3.ZERO : trackedTarget.getDeltaMovement();
            Vec3 toAim = aimPos.subtract(orb.position());
            Vec3 velocity;
            double boostedSpeed = state.speed() + Math.min(CHAOS_ORB_TARGET_SPEED_BONUS_MAX, targetVel.length() * CHAOS_ORB_TARGET_SPEED_BONUS);
            if (toAim.lengthSqr() <= 1.0E-6) {
                velocity = orb.getDeltaMovement().lengthSqr() > 1.0E-6
                        ? orb.getDeltaMovement().normalize().scale(boostedSpeed)
                        : new Vec3(0, -boostedSpeed, 0);
            } else {
                velocity = toAim.normalize().scale(boostedSpeed).add(targetVel.scale(CHAOS_ORB_TARGET_SPEED_FACTOR));
            }

            orb.setDeltaMovement(velocity);
            orb.hurtMarked = true;
        }
    }

    static boolean isChaosOrbExplosion(ServerLevel level, @Nullable Entity explosionEntity, @Nullable Vec3 center) {
        long now = level.getGameTime();

        if (explosionEntity != null && ACTIVE_CHAOS_ORBS.containsKey(explosionEntity.getUUID())) {
            recordExplosion(level, center != null ? center : explosionEntity.position(), now);
            return true;
        }

        if (center == null) return false;

        final double maxDistSqr = 3.0 * 3.0;
        for (var entry : ACTIVE_CHAOS_ORBS.entrySet()) {
            UUID orbId = entry.getKey();
            ChaosOrbFlightState state = entry.getValue();
            if (!state.levelKey().equals(level.dimension())) continue;
            if (now > state.expireGameTime()) continue;

            Entity orb = level.getEntity(orbId);
            if (orb == null) continue;

            if (orb.position().distanceToSqr(center) <= maxDistSqr) {
                recordExplosion(level, center, now);
                return true;
            }
        }
        return false;
    }

    static boolean wasRecentChaosOrbExplosionNear(ServerLevel level, Vec3 pos) {
        long now = level.getGameTime();
        pruneChaosOrbExplosionStamps(now);

        double maxDistSqr = 6.0D * 6.0D;
        for (ChaosOrbExplosionStamp stamp : RECENT_CHAOS_ORB_EXPLOSIONS) {
            if (stamp.dimension() != level.dimension()) continue;
            if (now - stamp.tick() > 1) continue;
            if (stamp.pos().distanceToSqr(pos) <= maxDistSqr) return true;
        }
        return false;
    }

    static Vec3 resolveExplosionCenter(net.minecraft.world.level.Explosion explosion) {
        try {
            var m = explosion.getClass().getMethod("center");
            Object o = m.invoke(explosion);
            if (o instanceof Vec3 v) return v;
        } catch (Throwable ignored) {
        }
        try {
            var m = explosion.getClass().getMethod("getPosition");
            Object o = m.invoke(explosion);
            if (o instanceof Vec3 v) return v;
        } catch (Throwable ignored) {
        }
        try {
            for (String fName : new String[]{"center", "pos", "position"}) {
                try {
                    var f = explosion.getClass().getDeclaredField(fName);
                    f.setAccessible(true);
                    Object o = f.get(explosion);
                    if (o instanceof Vec3 v) return v;
                } catch (Throwable ignored2) {
                }
            }
        } catch (Throwable ignored) {
        }
        return Vec3.ZERO;
    }

    @Nullable
    private static EntityType<?> resolveChaosOrbType() {
        for (String id : CHAOS_ORB_ENTITY_IDS) {
            ResourceLocation key = ResourceLocation.parse(id);
            var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(key);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    private static void spawnSingleChaosOrb(ServerLevel level, EntityType<?> orbType, Vec3 impactPos, @Nullable Entity shooter, @Nullable UUID targetId, int index) {
        double angle = (Math.PI * 2.0 * index / CHAOS_ORB_COUNT) + (level.random.nextDouble() - 0.5) * 0.35;
        double radius = CHAOS_ORB_SPAWN_RADIUS * (0.75 + level.random.nextDouble() * 0.45);

        Vec3 spawn = new Vec3(
                impactPos.x + Math.cos(angle) * radius,
                impactPos.y + CHAOS_ORB_SPAWN_HEIGHT + level.random.nextDouble() * 4.0,
                impactPos.z + Math.sin(angle) * radius
        );

        Entity orb = orbType.create(level);
        if (orb == null) return;

        orb.moveTo(spawn.x, spawn.y, spawn.z, level.random.nextFloat() * 360f, 0f);

        Entity trackedTarget = CataclysmTargeting.resolveTrackedTargetEntity(level, targetId);
        Vec3 target = CataclysmTargeting.resolveOrbAimPos(trackedTarget, impactPos);
        Vec3 targetVel = trackedTarget == null ? Vec3.ZERO : trackedTarget.getDeltaMovement();
        target = target.add(targetVel.scale(CHAOS_ORB_TARGET_LEAD_TICKS));
        Vec3 dir = target.subtract(spawn).normalize();

        double speed;
        if (orb instanceof Projectile projectile) {
            if (shooter != null) projectile.setOwner(shooter);
            projectile.shoot(dir.x, dir.y, dir.z, (float) CHAOS_ORB_SPEED, 0f);
            speed = projectile.getDeltaMovement().length();
        } else {
            speed = CHAOS_ORB_SPEED;
            orb.setDeltaMovement(dir.scale(speed));
        }

        level.addFreshEntity(orb);
        ACTIVE_CHAOS_ORBS.put(
                orb.getUUID(),
                new ChaosOrbFlightState(
                        level.dimension(),
                        impactPos,
                        targetId,
                        speed,
                        level.getGameTime() + CHAOS_ORB_MAX_LIFETIME_TICKS
                )
        );
    }

    private static void recordExplosion(ServerLevel level, Vec3 pos, long now) {
        RECENT_CHAOS_ORB_EXPLOSIONS.addLast(new ChaosOrbExplosionStamp(level.dimension(), pos, now));
        pruneChaosOrbExplosionStamps(now);
    }

    private static void pruneChaosOrbExplosionStamps(long now) {
        while (!RECENT_CHAOS_ORB_EXPLOSIONS.isEmpty()) {
            ChaosOrbExplosionStamp first = RECENT_CHAOS_ORB_EXPLOSIONS.peekFirst();
            if (first == null) break;
            if (now - first.tick() > CHAOS_ORB_EXPLOSION_STAMP_TTL_TICKS) {
                RECENT_CHAOS_ORB_EXPLOSIONS.removeFirst();
            } else {
                break;
            }
        }
    }
}
