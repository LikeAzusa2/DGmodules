package com.likeazusa2.dgmodules.logic;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext;
import com.likeazusa2.dgmodules.modules.FrameBreakerModuleEntity;
import com.likeazusa2.dgmodules.modules.FrameBreakerModuleType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FrameBreakerLogic {

    public static final String TAG_FRAME_BREAKER = "dg_frame_breaker";
    public static final String TAG_FRAME_BREAKER_AFFECT_PLAYERS = "dg_frame_breaker_affect_players";
    public static final long FRAME_BREAKER_COST_PER_HIT = 80_000L;

    public static boolean hasFrameBreakerMelee(Player player, LivingEntity target) {
        ActiveModule main = findActiveModule(player.getMainHandItem(), EquipmentSlot.MAINHAND);
        if (main != null && canAffect(main.module(), target)) {
            return consumeOp(player, main.stack(), main.slot(), FRAME_BREAKER_COST_PER_HIT);
        }

        ActiveModule off = findActiveModule(player.getOffhandItem(), EquipmentSlot.OFFHAND);
        return off != null
                && canAffect(off.module(), target)
                && consumeOp(player, off.stack(), off.slot(), FRAME_BREAKER_COST_PER_HIT);
    }

    public static FrameBreakerModuleEntity findHeldFrameBreaker(Player player) {
        FrameBreakerModuleEntity main = findActiveModule(player.getMainHandItem());
        if (main != null) return main;
        return findActiveModule(player.getOffhandItem());
    }

    public static boolean consumeForProjectile(Player owner, LivingEntity target) {
        ActiveModule main = findActiveModule(owner.getMainHandItem(), EquipmentSlot.MAINHAND);
        if (main != null && canAffect(main.module(), target)) {
            return consumeOp(owner, main.stack(), main.slot(), FRAME_BREAKER_COST_PER_HIT);
        }

        ActiveModule off = findActiveModule(owner.getOffhandItem(), EquipmentSlot.OFFHAND);
        return off != null
                && canAffect(off.module(), target)
                && consumeOp(owner, off.stack(), off.slot(), FRAME_BREAKER_COST_PER_HIT);
    }

    private static boolean canAffect(FrameBreakerModuleEntity module, LivingEntity target) {
        if (target instanceof Player && !module.isAffectPlayers()) {
            return false;
        }
        return module.isEnabled();
    }

    private static FrameBreakerModuleEntity findActiveModule(ItemStack stack) {
        if (stack.isEmpty()) return null;

        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) return null;
            return host.getEntitiesByType(FrameBreakerModuleType.INSTANCE)
                    .filter(ent -> ent instanceof FrameBreakerModuleEntity)
                    .map(ent -> (FrameBreakerModuleEntity) ent)
                    .filter(FrameBreakerModuleEntity::isEnabled)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static ActiveModule findActiveModule(ItemStack stack, EquipmentSlot slot) {
        FrameBreakerModuleEntity module = findActiveModule(stack);
        return module == null ? null : new ActiveModule(stack, slot, module);
    }

    private static boolean consumeOp(Player player, ItemStack hostStack, EquipmentSlot slot, long cost) {
        IOPStorage op = new StackModuleContext(hostStack, player, slot).getOpStorage();
        if (op == null || op.getOPStored() < cost) return false;

        long paid = (op instanceof OPStorage ops)
                ? ops.modifyEnergyStored(-cost)
                : op.extractOP(cost, false);
        return paid >= cost;
    }

    private record ActiveModule(ItemStack stack, EquipmentSlot slot, FrameBreakerModuleEntity module) {}
}
