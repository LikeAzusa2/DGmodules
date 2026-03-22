package com.likeazusa2.dgmodules.logic;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext;
import com.likeazusa2.dgmodules.DGConfig;
import com.likeazusa2.dgmodules.DGDamageTypes;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.CataclysmArrowModuleEntity;
import com.likeazusa2.dgmodules.modules.CataclysmArrowModuleType;
import com.likeazusa2.dgmodules.network.NetworkHandler;
import com.likeazusa2.dgmodules.network.S2CCataclysmShockwave;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.LazyOptional;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CataclysmArrowEvents {

    private static final String TAG_ENABLED = "dg_cataclysm_arrow";
    private static final String TAG_PIERCE_ENABLED = "dg_cataclysm_arrow_pierce";
    private static final String TAG_SUMMON_FIREBALL = "dg_cataclysm_arrow_summon_fireball";

    private static final Map<UUID, PierceDotState> ACTIVE_DOTS = new HashMap<>();
    private static final Map<UUID, WaveState> ACTIVE_WAVES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> LAST_PROCESSED_TICK_BY_DIMENSION = new HashMap<>();

    private record PierceDotState(ResourceKey<Level> levelKey, float damagePerTick, long expireGameTime, UUID attackerId, long nextTick) {}
    private record WaveState(ResourceKey<Level> levelKey, Vec3 center, int tick, UUID shooterId) {}

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer shooter)) return;

        ItemStack hostStack = findRangedHostStack(shooter);
        if (hostStack.isEmpty()) return;

        CataclysmArrowModuleEntity module = findActiveModule(hostStack);
        if (module == null) return;
        if (!extractOp(hostStack, shooter, DGConfig.SERVER.cataclysmCostPerArrow.get())) return;

        arrow.getPersistentData().putBoolean(TAG_ENABLED, true);
        arrow.getPersistentData().putBoolean(TAG_PIERCE_ENABLED, module.isHighFrequencyPierceEnabled());
        arrow.getPersistentData().putBoolean(TAG_SUMMON_FIREBALL, module.shouldSummonFireball());
    }

    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!(arrow.level() instanceof ServerLevel level)) return;
        if (!arrow.getPersistentData().getBoolean(TAG_ENABLED)) return;

        HitResult hit = event.getRayTraceResult();
        Vec3 pos = hit.getLocation();
        ServerPlayer shooter = arrow.getOwner() instanceof ServerPlayer sp ? sp : null;
        Entity shooterEntity = arrow.getOwner();
        UUID targetId = CataclysmTargeting.resolveTrackedTargetId(level, hit, pos, shooterEntity);
        boolean pierceEnabled = arrow.getPersistentData().getBoolean(TAG_PIERCE_ENABLED);
        boolean summonFireball = arrow.getPersistentData().getBoolean(TAG_SUMMON_FIREBALL);

        startWave(level, pos, shooter);
        if (summonFireball) {
            CataclysmChaosOrbLogic.spawnChaosGuardianOrbs(level, pos, shooter, targetId);
        }
        applyImpactDamage(level, arrow, shooter, pos, hit, pierceEnabled, (float) arrow.getBaseDamage());

        event.setCanceled(true);
        arrow.discard();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();
        Long lastProcessedTick = LAST_PROCESSED_TICK_BY_DIMENSION.get(dimension);
        if (lastProcessedTick != null && lastProcessedTick == now) return;
        LAST_PROCESSED_TICK_BY_DIMENSION.put(dimension, now);

        processDots(level, now);
        processWaves(level);
        CataclysmChaosOrbLogic.processChaosOrbBarrages(level, now);
        CataclysmChaosOrbLogic.processChaosOrbs(level, now);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;

        net.minecraft.world.level.Explosion explosion = event.getExplosion();
        Entity direct = explosion.getDirectSourceEntity();
        Entity causing = explosion.getIndirectSourceEntity();
        Entity explosionEntity = direct != null ? direct : causing;

        ServerLevel level = (ServerLevel) event.getLevel();
        Vec3 center = CataclysmChaosOrbLogic.resolveExplosionCenter(explosion);
        if (!CataclysmChaosOrbLogic.isChaosOrbExplosion(level, explosionEntity, center)) return;

        event.getAffectedEntities().removeIf(e -> e instanceof ItemEntity);
        for (Entity e : event.getAffectedEntities()) {
            if (e instanceof LivingEntity living) {
                float max = living.getMaxHealth();
                if (max <= 0) continue;
                float newHealth = living.getHealth() - (max * 0.01F);
                if (newHealth < 0F) newHealth = 0F;
                living.setHealth(newHealth);
            }
        }
    }

    private static void processDots(ServerLevel currentLevel, long now) {
        Iterator<Map.Entry<UUID, PierceDotState>> it = ACTIVE_DOTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PierceDotState> e = it.next();
            PierceDotState state = e.getValue();

            if (!state.levelKey().equals(currentLevel.dimension()) || now < state.nextTick() || now > state.expireGameTime()) {
                if (now > state.expireGameTime()) it.remove();
                continue;
            }

            Entity victimRaw = currentLevel.getEntity(e.getKey());
            if (!(victimRaw instanceof LivingEntity target) || !target.isAlive()) {
                it.remove();
                continue;
            }

            Entity attacker = state.attackerId() == null ? null : currentLevel.getEntity(state.attackerId());
            DamageSource pierceSource = new DamageSource(getDamageType(currentLevel, DGDamageTypes.CATACLYSM_PIERCE), attacker, attacker);
            target.invulnerableTime = 0;
            target.hurt(pierceSource, state.damagePerTick());

            if (now + 1 > state.expireGameTime()) {
                it.remove();
            } else {
                e.setValue(new PierceDotState(state.levelKey(), state.damagePerTick(), state.expireGameTime(), state.attackerId(), now + 1));
            }
        }
    }

    private static void processWaves(ServerLevel currentLevel) {
        Iterator<Map.Entry<UUID, WaveState>> it = ACTIVE_WAVES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, WaveState> e = it.next();
            WaveState state = e.getValue();
            if (!state.levelKey().equals(currentLevel.dimension())) continue;

            int total = DGConfig.SERVER.cataclysmWaveTotalTicks.get();
            if (state.tick() >= total) {
                it.remove();
                continue;
            }

            if (state.tick() == total / 2) {
                currentLevel.playSound(null, state.center().x, state.center().y, state.center().z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.85f, 1.4f);
            }

            if (state.tick() == total - 1) {
                applyCollapseTrueDamage(currentLevel, state.center(), state.shooterId());
                it.remove();
                continue;
            }

            e.setValue(new WaveState(state.levelKey(), state.center(), state.tick() + 1, state.shooterId()));
        }
    }

    private static void startWave(ServerLevel level, Vec3 center, ServerPlayer shooter) {
        ACTIVE_WAVES.put(UUID.randomUUID(), new WaveState(level.dimension(), center, 0, shooter == null ? null : shooter.getUUID()));
        S2CCataclysmShockwave packet = new S2CCataclysmShockwave(
                center.x,
                center.y,
                center.z,
                DGConfig.SERVER.cataclysmRadiusXZ.get().floatValue(),
                DGConfig.SERVER.cataclysmWaveTotalTicks.get(),
                level.getGameTime()
        );
        double radius = Math.max(48.0D, DGConfig.SERVER.cataclysmRadiusXZ.get() * 4.0D);
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                NetworkHandler.sendToPlayer(player, packet);
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.65f);
    }

    private static void applyImpactDamage(ServerLevel level, AbstractArrow arrow, ServerPlayer shooter, Vec3 pos, HitResult hit, boolean pierceEnabled, float arrowDamage) {
        DamageSource source = new DamageSource(getDamageType(level, DGDamageTypes.CATACLYSM_ARROW), arrow, shooter);
        double rx = DGConfig.SERVER.cataclysmRadiusXZ.get();
        double ry = DGConfig.SERVER.cataclysmRadiusY.get();

        AABB box = new AABB(
                pos.x - rx, pos.y - ry, pos.z - rx,
                pos.x + rx, pos.y + ry, pos.z + rx
        );

        int hitCount = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != shooter)) {
            if (hitCount >= DGConfig.SERVER.cataclysmMaxTargets.get()) break;

            Vec3 delta = target.position().subtract(pos);
            double nx = delta.x / rx;
            double ny = delta.y / ry;
            double nz = delta.z / rx;
            double ellipse = nx * nx + ny * ny + nz * nz;
            if (ellipse > 1.0) continue;

            double t = 1.0 - Math.sqrt(ellipse);
            float impact = (float) ((13.0 + 22.0 * t * t + target.getMaxHealth() * DGConfig.SERVER.cataclysmImpactHpRatio.get() * t)
                    * DGConfig.SERVER.cataclysmImpactMultiplier.get());
            float damage = impact + Math.max(0f, arrowDamage);

            if (hit.getType() == HitResult.Type.ENTITY && hit instanceof net.minecraft.world.phys.EntityHitResult ehr && ehr.getEntity() == target) {
                damage += arrowDamage * DGConfig.SERVER.cataclysmImpactDirectMultiplier.get().floatValue();
            }

            if (target.hurt(source, damage)) {
                target.setRemainingFireTicks((int) Math.max(target.getRemainingFireTicks(), (2 + Math.ceil(2.5 * t)) * 20));
            }

            if (pierceEnabled) {
                float perTick = (float) Math.max(0.0D, arrowDamage * DGConfig.SERVER.cataclysmPierceBaseMultiplier.get());
                long now = level.getGameTime();
                ACTIVE_DOTS.put(
                        target.getUUID(),
                        new PierceDotState(
                                level.dimension(),
                                perTick,
                                now + DGConfig.SERVER.cataclysmPierceDurationTicks.get(),
                                shooter == null ? null : shooter.getUUID(),
                                now + 1
                        )
                );
            }

            hitCount++;
        }

        damageCrystalLikeEntities(level, shooter, pos, rx, ry);
    }

    private static void damageCrystalLikeEntities(ServerLevel level, ServerPlayer shooter, Vec3 center, double rx, double ry) {
        DamageSource source = new DamageSource(getDamageType(level, DGDamageTypes.CATACLYSM_ARROW), shooter, shooter);
        AABB box = new AABB(center.x - rx, center.y - ry, center.z - rx, center.x + rx, center.y + ry, center.z + rx);

        for (Entity e : level.getEntities((Entity) null, box, entity -> true)) {
            if (e == shooter || e instanceof LivingEntity) continue;

            boolean crystalLike = e instanceof EndCrystal || e.getType().toString().toLowerCase().contains("crystal");
            if (!crystalLike) continue;

            Vec3 delta = e.position().subtract(center);
            double nx = delta.x / rx;
            double ny = delta.y / ry;
            double nz = delta.z / rx;
            if (nx * nx + ny * ny + nz * nz > 1.0) continue;

            e.hurt(source, DGConfig.SERVER.cataclysmNonLivingDamage.get().floatValue());
        }
    }

    private static void applyCollapseTrueDamage(ServerLevel level, Vec3 center, UUID shooterId) {
        Entity attacker = shooterId == null ? null : level.getEntity(shooterId);

        double rx = DGConfig.SERVER.cataclysmRadiusXZ.get();
        double ry = DGConfig.SERVER.cataclysmRadiusY.get();
        AABB box = new AABB(center.x - rx, center.y - ry, center.z - rx, center.x + rx, center.y + ry, center.z + rx);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (attacker != null && target == attacker) continue;

            Vec3 delta = target.position().subtract(center);
            double nx = delta.x / rx;
            double ny = delta.y / ry;
            double nz = delta.z / rx;
            if (nx * nx + ny * ny + nz * nz > 1.0) continue;

            float hpCut = target.getMaxHealth() * DGConfig.SERVER.cataclysmCollapseHpRatio.get().floatValue();
            target.setHealth(Math.max(0.0f, target.getHealth() - hpCut));
        }
    }

    private static Holder<DamageType> getDamageType(ServerLevel level, ResourceKey<DamageType> key) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
    }

    private static ItemStack findRangedHostStack(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (isRangedWeapon(main)) return main;

        ItemStack off = player.getOffhandItem();
        if (isRangedWeapon(off)) return off;

        ItemStack using = player.getUseItem();
        if (isRangedWeapon(using)) return using;

        return ItemStack.EMPTY;
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem;
    }

    private static CataclysmArrowModuleEntity findActiveModule(ItemStack stack) {
        LazyOptional<ModuleHost> optionalHost = stack.getCapability(DECapabilities.MODULE_HOST_CAPABILITY);
        ModuleHost host = optionalHost.orElse(null);
        if (host == null) return null;
        return host.getEntitiesByType(CataclysmArrowModuleType.INSTANCE)
                .filter(ent -> ent instanceof CataclysmArrowModuleEntity)
                .map(ent -> (CataclysmArrowModuleEntity) ent)
                .filter(CataclysmArrowModuleEntity::isEnabled)
                .findFirst()
                .orElse(null);
    }

    private static boolean extractOp(ItemStack hostStack, ServerPlayer player, long cost) {
        EquipmentSlot slot = hostStack == player.getOffhandItem() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        IOPStorage op = new StackModuleContext(hostStack, player, slot).getOpStorage();
        if (op == null || op.getOPStored() < cost) return false;

        long paid = (op instanceof OPStorage ops)
                ? ops.modifyEnergyStored(-cost)
                : op.extractOP(cost, false);

        return paid >= cost;
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        if (CataclysmChaosOrbLogic.wasRecentChaosOrbExplosionNear(level, event.getEntity().position())) {
            event.setStrength(0F);
        }
    }
}
