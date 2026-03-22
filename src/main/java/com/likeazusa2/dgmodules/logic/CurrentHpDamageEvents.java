package com.likeazusa2.dgmodules.logic;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.CurrentHpDamageModule;
import com.likeazusa2.dgmodules.modules.CurrentHpDamageModuleType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurrentHpDamageEvents {
    public static final long OP_PER_DAMAGE = 2000L;

    @SubscribeEvent
    public static void onDamagePre(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();
        if (event.getAmount() <= 0) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;
        if (!(attacker instanceof ServerPlayer sp)) return;

        ItemStack weapon = sp.getMainHandItem();
        if (weapon.isEmpty()) return;

        float pct = 0f;
        ModuleHost host = DGHostHelper.getHost(weapon);
        if (host == null) return;

        for (var ent : host.getEntitiesByType(CurrentHpDamageModuleType.INSTANCE).toList()) {
            Module<?> module = ent.getModule();
            if (module instanceof CurrentHpDamageModule hpDamage) {
                pct += hpDamage.getPercent();
            }
        }
        if (pct <= 0) return;

        float extraWanted = victim.getHealth() * pct;
        if (extraWanted <= 0) return;

        IOPStorage op = getWeaponOpStorage(weapon, sp);
        if (op == null) return;

        long stored = op.getOPStored();
        if (stored <= 0) return;

        float payable = (float) (stored / (double) OP_PER_DAMAGE);
        if (payable <= 0) return;

        float extra = Math.min(extraWanted, payable);
        if (extra <= 0) return;

        long cost = (long) Math.ceil(extra) * OP_PER_DAMAGE;
        long paid = (op instanceof OPStorage ops) ? ops.modifyEnergyStored(-cost) : op.extractOP(cost, false);
        if (paid <= 0) return;

        float actualExtra = (float) (paid / (double) OP_PER_DAMAGE);
        if (actualExtra <= 0) return;

        event.setAmount(event.getAmount() + actualExtra);
    }

    private static IOPStorage getWeaponOpStorage(ItemStack weapon, ServerPlayer sp) {
        var ctx = new com.brandon3055.draconicevolution.api.modules.lib.StackModuleContext(weapon, sp, EquipmentSlot.MAINHAND);
        return ctx.getOpStorage();
    }
}
