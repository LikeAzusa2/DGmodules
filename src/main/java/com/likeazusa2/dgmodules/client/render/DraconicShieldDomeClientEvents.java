package com.likeazusa2.dgmodules.client.render;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.ModContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * Client-side registration for renderers + shaders used by the dome shield.
 *
 * NOTE:
 * Shader assets must exist at:
 *   assets/dgmodules/shaders/core/dome_shield.json
 *   assets/dgmodules/shaders/core/dome_shield.vsh
 *   assets/dgmodules/shaders/core/dome_shield.fsh
 */
@EventBusSubscriber(modid = DGModules.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class DraconicShieldDomeClientEvents {

    /** Custom (original) soft-wave shield shader for the dome. */
    public static ShaderInstance DOME_WAVE_SHADER;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(DGModules.MODID, "dome_shield"),
                DefaultVertexFormat.POSITION_COLOR
        ), shader -> DOME_WAVE_SHADER = shader);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.DOME_EMITTER_PROJECTILE.get(), DomeEmitterProjectileRenderer::new);
        event.registerEntityRenderer(ModContent.DOME_CORE.get(), DraconicShieldDomeCoreRenderer::new);
    }
}
