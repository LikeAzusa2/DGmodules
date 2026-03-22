package com.likeazusa2.dgmodules.entity;

import com.likeazusa2.dgmodules.ModContent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.likeazusa2.dgmodules.DGModules.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DomeEntityAttributes {

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(ModContent.DOME_CORE.get(), DraconicShieldDomeCoreEntity.createAttributes().build());
    }
}
