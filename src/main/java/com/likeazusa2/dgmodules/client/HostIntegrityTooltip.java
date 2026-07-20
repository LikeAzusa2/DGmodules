package com.likeazusa2.dgmodules.client;

import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.likeazusa2.dgmodules.modules.HostIntegrityModuleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.UUID;

/** Adds the persistent stable identity to the protected host's tooltip. */
public final class HostIntegrityTooltip {

    private HostIntegrityTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        UUID uuid = findStableUuid(event.getItemStack());
        if (uuid == null) {
            return;
        }
        event.getToolTip().add(Component.translatable(
                        "tooltip.dgmodules.host_integrity.uuid", uuid.toString())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static UUID findStableUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        try (ModuleHost host = DECapabilities.getHost(stack)) {
            if (host == null) {
                return null;
            }
            for (var entity : host.getModuleEntities()) {
                if (entity instanceof HostIntegrityModuleEntity integrity) {
                    return integrity.getModuleUuid();
                }
            }
        } catch (Throwable ignored) {
            // Foreign or malformed module hosts should not break tooltips.
        }
        return null;
    }
}
