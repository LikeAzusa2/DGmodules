package com.likeazusa2.dgmodules.client.render;

import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.ModContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
@Mod.EventBusSubscriber(modid = DGModules.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DraconicShieldDomeClientEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Custom (original) soft-wave shield shader for the dome. */
    public static ShaderInstance DOME_WAVE_SHADER;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(DGModules.MODID, "dome_shield"),
                    DefaultVertexFormat.NEW_ENTITY
            ), shader -> DOME_WAVE_SHADER = shader);
        } catch (IOException exception) {
            DOME_WAVE_SHADER = null;
            LOGGER.warn("Failed to load dgmodules dome shader, falling back to default DE shield shader", exception);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.DOME_EMITTER_PROJECTILE.get(), DomeEmitterProjectileRenderer::new);
        event.registerEntityRenderer(ModContent.DOME_CORE.get(), DraconicShieldDomeCoreRenderer::new);
    }
}
