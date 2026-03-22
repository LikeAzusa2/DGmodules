package com.likeazusa2.dgmodules.client;

import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.modules.CurrentHpDamageModule;
import com.likeazusa2.dgmodules.modules.CurrentHpDamageModuleType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class WeaponHpCutTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        float hpCut = getHpCutPercent(stack);
        if (hpCut <= 0) return;

        int percent = Math.round(hpCut * 100.0f);

        event.getToolTip().add(
                Component.translatable("tooltip.dgmodules.hp_cut.total", percent)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.LIGHT_PURPLE)
        );
        event.getToolTip().add(
                Component.translatable("tooltip.dgmodules.hp_damage.energy")
                        .withStyle(ChatFormatting.DARK_AQUA)
        );
    }

    private static float getHpCutPercent(ItemStack stack) {
        LazyOptional<ModuleHost> optionalHost = stack.getCapability(DECapabilities.MODULE_HOST_CAPABILITY);
        ModuleHost host = optionalHost.orElse(null);
        if (host == null) return 0.0f;

        final float[] sum = {0.0f};
        host.getEntitiesByType(CurrentHpDamageModuleType.INSTANCE).forEach(entity -> {
            var module = entity.getModule();
            if (module instanceof CurrentHpDamageModule hp) {
                sum[0] += hp.getPercent();
            }
        });
        return sum[0];
    }
}
