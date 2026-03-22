package com.likeazusa2.dgmodules.modules;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext;
import com.likeazusa2.dgmodules.logic.DGHostHelper;
import com.likeazusa2.dgmodules.logic.DGHostLocator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class DragonGuardModuleEntity extends ModuleEntity<NoData> {
    public static final long COST = 10_000_000L;
    private static final String TAG_LAST_GUARD_TICK = "dg_dragon_guard_tick";

    public DragonGuardModuleEntity(Module<NoData> module) {
        super(module);
    }

    public DragonGuardModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        return new DragonGuardModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
    }

    @Override
    public void tick(ModuleContext ctx) {
    }

    public static void tryGuardPlayer(ServerPlayer sp) {
        if (sp == null || sp.level().isClientSide) return;

        long now = sp.serverLevel().getGameTime();
        if (alreadyTriggeredRecently(sp, now)) return;

        ItemStack chest = DGHostLocator.findChestLikeHost(sp, DragonGuardModuleEntity::hostHasDragonGuard);
        if (chest.isEmpty()) return;

        ModuleHost host = DGHostHelper.getHost(chest);
        if (host == null || !hostHasDragonGuard(host)) return;

        StackModuleContext ctx = new StackModuleContext(chest, sp, EquipmentSlot.CHEST);
        if (!extractOp(ctx, COST)) return;

        markTriggered(sp, now);
        sp.setHealth(1.0F);
        sp.hurtMarked = true;
        sp.invulnerableTime = Math.max(sp.invulnerableTime, 2);
    }

    public static boolean hostHasDragonGuard(ModuleHost host) {
        try {
            for (var ent : host.getModuleEntities()) {
                var module = ent.getModule();
                if (module instanceof DragonGuardModule) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean extractOp(StackModuleContext ctx, long cost) {
        IOPStorage op = ctx.getOpStorage();
        if (op == null || op.getOPStored() < cost) return false;

        long paid = (op instanceof OPStorage ops) ? ops.modifyEnergyStored(-cost) : op.extractOP(cost, false);
        return paid >= cost;
    }

    private static boolean alreadyTriggeredRecently(ServerPlayer sp, long now) {
        long last = sp.getPersistentData().getLong(TAG_LAST_GUARD_TICK);
        return (now - last) <= 1;
    }

    private static void markTriggered(ServerPlayer sp, long now) {
        sp.getPersistentData().putLong(TAG_LAST_GUARD_TICK, now);
    }
}
