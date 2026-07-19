package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.brandon3055.draconicevolution.init.DEDamage;
import com.likeazusa2.dgmodules.DGConfig;
import com.likeazusa2.dgmodules.network.S2CChaosCrystalCountdown;
import com.likeazusa2.dgmodules.network.S2CChaosCrystalSingularity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative logic for the breaker installed below a DE guardian
 * crystal entity. The 14 crystals on the island are entities, not the
 * central {@code TileChaosCrystal} block, so this class never resolves the
 * central block as a target.
 */
public final class ChaosCrystalBreakerLogic {

    private static final Map<ServerLevel, Map<UUID, PendingCharge>> PENDING_CHARGES = new WeakHashMap<>();
    private static final Map<Level, Map<UUID, Long>> ACTIVE_DEFENSE = new WeakHashMap<>();

    private ChaosCrystalBreakerLogic() {
    }

    private record PendingCharge(long detonationTick, UUID installerId, BlockPos anchorPos) {
    }

    /**
     * Finds one of DE's 14 end-crystal-like entities. The block argument is
     * used for the normal right-click-block path, while the ray fallback is
     * used when the entity's collision box is what the player is aiming at.
     */
    public static GuardianCrystalEntity findTarget(Level level, Player player, BlockPos clickedPos) {
        if (level == null) return null;

        if (clickedPos != null) {
            GuardianCrystalEntity nearby = findCrystalNearBlock(level, clickedPos);
            if (nearby != null) return nearby;
        }

        if (player == null) return null;

        double reach = Math.max(1.0D, player.blockInteractionRange());
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));

        HitResult blockHit = level.clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 finalEnd = blockHit.getType() == HitResult.Type.MISS
                ? end
                : blockHit.getLocation();

        AABB scanBox = new AABB(eye, finalEnd).inflate(1.0D);
        GuardianCrystalEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (GuardianCrystalEntity crystal : level.getEntitiesOfClass(
                GuardianCrystalEntity.class,
                scanBox,
                entity -> entity.isAlive() && entity.isPickable())) {
            var hit = crystal.getBoundingBox().inflate(0.25D).clip(eye, finalEnd);
            if (hit.isEmpty()) continue;

            double distance = eye.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = crystal;
            }
        }
        return best;
    }

    private static GuardianCrystalEntity findCrystalNearBlock(Level level, BlockPos clickedPos) {
        AABB search = new AABB(clickedPos).inflate(0.85D, 2.5D, 0.85D);
        GuardianCrystalEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 blockCenter = Vec3.atCenterOf(clickedPos);

        for (GuardianCrystalEntity crystal : level.getEntitiesOfClass(
                GuardianCrystalEntity.class,
                search,
                entity -> entity.isAlive() && entity.isPickable())) {
            double distance = crystal.position().distanceToSqr(blockCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = crystal;
            }
        }
        return best;
    }

    public static int configuredCountdownTicks() {
        return Math.max(1, DGConfig.SERVER.chaosCrystalBreakerCountdownTicks.get());
    }

    public static int configuredDefenseDurationTicks() {
        return Math.max(1, DGConfig.SERVER.chaosCrystalBreakerDurationTicks.get());
    }

    public static double configuredBlastRadius() {
        return Math.max(0.5D, DGConfig.SERVER.chaosCrystalBreakerBlastRadius.get());
    }

    public static float configuredDamage() {
        return Math.max(0.0F, DGConfig.SERVER.chaosCrystalBreakerDamage.get().floatValue());
    }

    public static int configuredEffectTicks() {
        return Math.max(1, DGConfig.SERVER.chaosCrystalBreakerEffectTicks.get());
    }

    /**
     * Installs the breaker logically on the support block below the entity.
     * The block is only the installation anchor; the target is always the
     * selected GuardianCrystalEntity UUID.
     */
    public static boolean installAndConsume(Level level, Player player, ItemStack stack,
                                            GuardianCrystalEntity target, BlockPos clickedBlock) {
        if (!(level instanceof ServerLevel serverLevel)
                || player == null
                || stack == null
                || stack.isEmpty()
                || target == null
                || target.level() != serverLevel
                || !target.isAlive()) {
            return false;
        }

        BlockPos anchor = resolveAnchor(serverLevel, target, clickedBlock);
        PendingCharge charge = install(serverLevel, player, target, anchor);
        if (charge == null) return false;

        broadcastCountdown(serverLevel, target.getUUID(), charge);
        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }
        return true;
    }

    private static PendingCharge install(ServerLevel level, Player player, GuardianCrystalEntity target,
                                         BlockPos anchorPos) {
        Map<UUID, PendingCharge> charges = PENDING_CHARGES.computeIfAbsent(level, ignored -> new HashMap<>());
        UUID targetId = target.getUUID();
        if (charges.containsKey(targetId)) return null;

        PendingCharge charge = new PendingCharge(
                level.getGameTime() + configuredCountdownTicks(),
                player == null ? null : player.getUUID(),
                anchorPos.immutable()
        );
        charges.put(targetId, charge);
        return charge;
    }

    /** Called once per server-level tick by ChaosCrystalBreakerEvents. */
    public static void tick(ServerLevel level) {
        Map<UUID, PendingCharge> charges = PENDING_CHARGES.get(level);
        if (charges == null || charges.isEmpty()) return;

        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, PendingCharge>> iterator = charges.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingCharge> entry = iterator.next();
            UUID targetId = entry.getKey();
            PendingCharge charge = entry.getValue();

            if (now < charge.detonationTick()) {
                long remaining = charge.detonationTick() - now;
                if (remaining % 20 == 0) {
                    broadcastCountdown(level, targetId, charge);
                }
                continue;
            }

            iterator.remove();
            Entity entity = level.getEntity(targetId);
            if (entity instanceof GuardianCrystalEntity target && target.isAlive()) {
                detonate(level, target, targetId, charge);
            }
        }

        if (charges.isEmpty()) {
            PENDING_CHARGES.remove(level);
        }
    }

    private static void broadcastCountdown(ServerLevel level, UUID targetId, PendingCharge charge) {
        Vec3 center = Vec3.atCenterOf(charge.anchorPos());
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                Math.max(32.0D, configuredBlastRadius() * 4.0D),
                new S2CChaosCrystalCountdown(targetId, charge.detonationTick())
        );
    }

    private static void detonate(ServerLevel level, GuardianCrystalEntity target, UUID targetId,
                                 PendingCharge charge) {
        Vec3 center = Vec3.atCenterOf(charge.anchorPos());
        Entity attacker = charge.installerId() == null ? null : level.getEntity(charge.installerId());
        int defenseDuration = configuredDefenseDurationTicks();

        // Do not call GuardianCrystalEntity.destabilize(): DE implements that
        // path with Minecraft particles. Clearing the shield directly and
        // keeping it at zero through the mixin gives the same break result
        // while leaving the visual effect entirely to our renderer.
        openDefense(level, target, defenseDuration);
        applyChaosGuardianDamage(level, center, attacker);

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                Math.max(32.0D, configuredBlastRadius() * 4.0D),
                new S2CChaosCrystalSingularity(
                        center.x,
                        center.y,
                        center.z,
                        (float) configuredBlastRadius(),
                        configuredEffectTicks(),
                        level.getGameTime(),
                        targetId,
                        defenseDuration
                )
        );

        // Audio only; no vanilla explosion is created, so there are no
        // vanilla explosion particles or block destruction side effects.
        level.playSound(null, center.x, center.y, center.z,
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.75F);
    }

    private static void openDefense(ServerLevel level, GuardianCrystalEntity target, int durationTicks) {
        target.setInvulnerable(false);
        target.setBeamTarget(null);
        target.setShieldPower(0.0F);
        ACTIVE_DEFENSE.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(target.getUUID(), level.getGameTime() + durationTicks);
    }

    private static void applyChaosGuardianDamage(ServerLevel level, Vec3 center, Entity attacker) {
        double radius = configuredBlastRadius();
        double radiusSqr = radius * radius;
        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        DamageSource source = DEDamage.guardian(level, attacker);
        float damage = configuredDamage();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target == attacker) continue;
            if (target.position().distanceToSqr(center) > radiusSqr) continue;

            // This is the same chaotic damage type used by the Draconic
            // Guardian; clearing i-frames matches DE's own attack cadence.
            target.invulnerableTime = 0;
            target.hurt(source, damage);
        }
    }

    public static void activateClient(Level level, UUID targetId, int durationTicks) {
        if (level == null || !level.isClientSide() || targetId == null || durationTicks <= 0) return;
        ACTIVE_DEFENSE.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(targetId, level.getGameTime() + durationTicks);
    }

    public static boolean isDefenseDisabled(GuardianCrystalEntity target) {
        return target != null && isDefenseDisabled(target.level(), target.getUUID());
    }

    public static boolean isDefenseDisabled(Level level, UUID targetId) {
        if (level == null || targetId == null) return false;

        Map<UUID, Long> active = ACTIVE_DEFENSE.get(level);
        if (active == null) return false;

        long now = level.getGameTime();
        Long expiry = active.get(targetId);
        if (expiry == null) return false;
        if (expiry <= now) {
            active.remove(targetId);
            if (active.isEmpty()) ACTIVE_DEFENSE.remove(level);
            return false;
        }
        return true;
    }

    private static BlockPos resolveAnchor(Level level, GuardianCrystalEntity target, BlockPos clickedBlock) {
        if (clickedBlock != null) return clickedBlock.immutable();

        BlockPos entityPos = target.blockPosition();
        if (!level.getBlockState(entityPos).isAir()) return entityPos.immutable();

        BlockPos below = entityPos.below();
        if (!level.getBlockState(below).isAir()) return below.immutable();
        return entityPos.immutable();
    }
}
