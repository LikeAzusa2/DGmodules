package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.BaseModule;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 宿主完整性守护模块。
 *
 * <p>模块本身不在 tick 中执行逻辑，保护由宿主物品的写入入口和网络/容器操作入口完成。</p>
 */
public class HostIntegrityModule extends BaseModule<NoData> {

    private final Item item;

    public HostIntegrityModule(Item item) {
        super(HostIntegrityModuleType.INSTANCE, HostIntegrityModuleType.PROPERTIES);
        this.item = item;
    }

    @Override
    public Item getItem() {
        return item;
    }

    @Override
    public int maxInstallable() {
        return 1;
    }

    @Override
    public void addInformation(List<Component> info, ModuleContext context) {
        super.addInformation(info, context);
        info.add(Component.translatable("module.dgmodules.host_integrity.desc")
                .withStyle(ChatFormatting.GRAY));
        info.add(Component.translatable("module.dgmodules.host_integrity.scope")
                .withStyle(ChatFormatting.AQUA));
        info.add(Component.translatable("module.dgmodules.host_integrity.identity")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
