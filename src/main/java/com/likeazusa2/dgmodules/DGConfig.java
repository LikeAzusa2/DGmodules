package com.likeazusa2.dgmodules;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DGConfig {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SERVER = new Server(b);
        SERVER_SPEC = b.build();
    }

    public static final class Server {
        public final ForgeConfigSpec.LongValue chaosLaserCostNormalPerTick;
        public final ForgeConfigSpec.LongValue chaosLaserCostExecutePerTick;
        public final ForgeConfigSpec.IntValue chaosLaserRange;
        public final ForgeConfigSpec.LongValue dragonGuardCost;
        public final ForgeConfigSpec.DoubleValue chaosLaserNormalBaseDamage;
        public static ForgeConfigSpec.LongValue phaseShieldCostPerTick;
        public final ForgeConfigSpec.LongValue cataclysmCostPerArrow;
        public final ForgeConfigSpec.DoubleValue cataclysmRadiusXZ;
        public final ForgeConfigSpec.DoubleValue cataclysmRadiusY;
        public final ForgeConfigSpec.IntValue cataclysmMaxTargets;
        public final ForgeConfigSpec.IntValue cataclysmPierceDurationTicks;
        public final ForgeConfigSpec.DoubleValue cataclysmPierceBaseMultiplier;
        public final ForgeConfigSpec.DoubleValue cataclysmImpactMultiplier;
        public final ForgeConfigSpec.DoubleValue cataclysmImpactDirectMultiplier;
        public final ForgeConfigSpec.DoubleValue cataclysmImpactHpRatio;
        public final ForgeConfigSpec.DoubleValue cataclysmCollapseHpRatio;
        public final ForgeConfigSpec.IntValue cataclysmWaveTotalTicks;
        public final ForgeConfigSpec.IntValue cataclysmWavePoints;
        public final ForgeConfigSpec.DoubleValue cataclysmNonLivingDamage;

        Server(ForgeConfigSpec.Builder b) {
            chaosLaserCostNormalPerTick = b.defineInRange("cost_normal_per_tick", 2_500_000L, 0L, Long.MAX_VALUE);
            chaosLaserCostExecutePerTick = b.defineInRange("cost_execute_per_tick", 10_000_000L, 0L, Long.MAX_VALUE);
            chaosLaserRange = b.defineInRange("range", 128, 1, 512);
            chaosLaserNormalBaseDamage = b.defineInRange("normal_base_damage", 16.0D, 0.0D, 2048.0D);

            b.push("dragon_guard");
            dragonGuardCost = b.defineInRange("cost", 10_000_000L, 0L, Long.MAX_VALUE);
            b.pop();

            b.push("phase_shield");
            phaseShieldCostPerTick = b.defineInRange("cost_per_tick", 8_000_000L, 0L, Long.MAX_VALUE);
            b.pop();

            b.push("cataclysm_arrow");
            cataclysmCostPerArrow = b.defineInRange("cost_per_arrow", 650_000L, 0L, Long.MAX_VALUE);
            cataclysmRadiusXZ = b.defineInRange("radius_xz", 6.2D, 0.1D, 128D);
            cataclysmRadiusY = b.defineInRange("radius_y", 3.4D, 0.1D, 128D);
            cataclysmMaxTargets = b.defineInRange("max_targets", 16, 1, 512);
            cataclysmPierceDurationTicks = b.defineInRange("pierce_duration_ticks", 24, 1, 200);
            cataclysmPierceBaseMultiplier = b.defineInRange("pierce_base_multiplier", 0.10D, 0.0D, 100.0D);
            cataclysmImpactMultiplier = b.defineInRange("impact_multiplier", 3D, 0.0D, 20.0D);
            cataclysmImpactDirectMultiplier = b.defineInRange("impact_direct_multiplier", 1D, 0.0D, 100.0D);
            cataclysmImpactHpRatio = b.defineInRange("impact_hp_ratio", 0.15D, 0.0D, 1.0D);
            cataclysmCollapseHpRatio = b.defineInRange("collapse_hp_ratio", 0.1D, 0.0D, 1.0D);
            cataclysmWaveTotalTicks = b.defineInRange("wave_total_ticks", 16, 2, 200);
            cataclysmWavePoints = b.defineInRange("wave_points", 42, 8, 512);
            cataclysmNonLivingDamage = b.defineInRange("non_living_damage", 80.0D, 0.0D, 4096D);
            b.pop();
        }
    }

    private DGConfig() {}
}
