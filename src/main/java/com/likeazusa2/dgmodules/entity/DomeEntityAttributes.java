package com.likeazusa2.dgmodules.entity;

import com.likeazusa2.dgmodules.ModContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = com.likeazusa2.dgmodules.DGModules.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DomeEntityAttributes {

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(ModContent.DOME_CORE.get(), DraconicShieldDomeCoreEntity.createAttributes().build());
    }
}
