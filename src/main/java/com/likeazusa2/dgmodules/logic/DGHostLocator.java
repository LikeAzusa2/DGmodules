package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

/**
 * 统一查找已经穿在身上、并且真的装了目标模块的宿主物品。
 */
public final class DGHostLocator {
    private DGHostLocator() {
    }

    /**
     * 优先检查胸甲槽，再检查 Curios。
     */
    public static ItemStack findChestLikeHost(LivingEntity entity, Predicate<ModuleHost> hostMatcher) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (matchesHost(chest, hostMatcher)) {
            return chest;
        }

        if (!ModList.get().isLoaded("curios")) {
            return ItemStack.EMPTY;
        }

        return CuriosApi.getCuriosHelper()
                .findFirstCurio(entity, stack -> matchesHost(stack, hostMatcher))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }

    /**
     * 安全判断一个物品是否真的带有宿主，并且宿主满足指定条件。
     */
    public static boolean matchesHost(ItemStack stack, Predicate<ModuleHost> hostMatcher) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        try {
            ModuleHost host = DGHostHelper.getHost(stack);
            return host != null && hostMatcher.test(host);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 判断宿主中是否安装了本模组提供的任意模块。
     */
    public static boolean hostHasAnyDGModule(ModuleHost host) {
        try {
            return host.getModuleEntities().stream().anyMatch(entity ->
                    BuiltInRegistries.ITEM.getKey(entity.getModule().getItem()).getNamespace().equals(DGModules.MODID)
            );
        } catch (Throwable ignored) {
            return false;
        }
    }
}
