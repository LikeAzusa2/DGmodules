package com.likeazusa2.dgmodules.entity;

import com.likeazusa2.dgmodules.ModContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class DomeEmitterProjectileEntity extends ThrowableItemProjectile {

    public DomeEmitterProjectileEntity(EntityType<? extends DomeEmitterProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public DomeEmitterProjectileEntity(Level level, LivingEntity shooter) {
        super(ModContent.DOME_EMITTER_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModContent.DRACONIC_SHIELD_DOME_EMITTER.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        spawnDomeCore(result.getLocation().x, result.getLocation().y, result.getLocation().z);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        spawnDomeCore(result.getLocation().x, result.getLocation().y, result.getLocation().z);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    private void spawnDomeCore(double x, double y, double z) {
        if (this.level().isClientSide) {
            return;
        }
        DraconicShieldDomeCoreEntity dome = new DraconicShieldDomeCoreEntity(ModContent.DOME_CORE.get(), this.level());
        dome.moveTo(x, y + 0.1D, z, this.getYRot(), this.getXRot());
        this.level().addFreshEntity(dome);

        ServerLevel serverLevel = (ServerLevel) this.level();
        for (int i = 0; i < 12; i++) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y + 0.1D, z, 1, 0.12D, 0.12D, 0.12D, 0.01D);
        }
    }
}
