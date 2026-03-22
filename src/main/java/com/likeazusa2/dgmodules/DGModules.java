package com.likeazusa2.dgmodules;

import com.brandon3055.draconicevolution.init.DEModules;
import com.likeazusa2.dgmodules.client.WeaponHpCutTooltip;
import com.likeazusa2.dgmodules.logic.DraconicShieldDomeEvents;
import com.likeazusa2.dgmodules.logic.ServerTickHandler;
import com.likeazusa2.dgmodules.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(DGModules.MODID)
public class DGModules {
    public static final String MODID = "dgmodules";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DGModules() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModContent.init(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DGConfig.SERVER_SPEC);
        modBus.addListener(DGCapabilities::registerCapabilities);
        NetworkHandler.init(modBus);
        modBus.addListener(DGModules::onConstruct);

        MinecraftForge.EVENT_BUS.register(ServerTickHandler.class);
        MinecraftForge.EVENT_BUS.register(DraconicShieldDomeEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientOnly.init();
        }
    }

    private static void onConstruct(final FMLConstructModEvent event) {
        DEModules.MODULE_PROVIDING_MODS.add(MODID);
        LOGGER.info("Early add {} to DE MODULE_PROVIDING_MODS (construct)", MODID);
    }

    private static class ClientOnly {
        static void init() {
            MinecraftForge.EVENT_BUS.register(WeaponHpCutTooltip.class);
        }
    }
}
