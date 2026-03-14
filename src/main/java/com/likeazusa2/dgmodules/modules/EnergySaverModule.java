package com.likeazusa2.dgmodules.modules;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.modules.data.ModuleProperties;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.BaseModule;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 节能模块本体。
 * 每个模块提供一个固定的“耗能减免比例”，多个模块线性叠加后统一作用于宿主的 OP 消耗。
 */
public class EnergySaverModule extends BaseModule<NoData> {

    private final Item item;
    private final float savingMultiplier;

    public EnergySaverModule(Item item, TechLevel techLevel, float savingMultiplier) {
        super(EnergySaverModuleType.INSTANCE, new ModuleProperties<>(techLevel, m -> new NoData()));
        this.item = item;
        this.savingMultiplier = savingMultiplier;
    }

    @Override
    public Item getItem() {
        return item;
    }

    public float getSavingMultiplier() {
        return savingMultiplier;
    }

    @Override
    public void addInformation(List<Component> info, ModuleContext context) {
        super.addInformation(info, context);
        info.add(Component.translatable("module.dgmodules.energy_saver.desc")
                .withStyle(ChatFormatting.GRAY));
        info.add(Component.translatable(
                        "tooltip.dgmodules.energy_saver.rate",
                        Math.round(savingMultiplier * 100.0f)
                )
                .withStyle(ChatFormatting.AQUA));
        info.add(Component.translatable("tooltip.dgmodules.energy_saver.stack")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
