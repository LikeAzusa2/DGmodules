package com.likeazusa2.dgmodules.client.render;

import com.brandon3055.draconicevolution.client.DEShaders;
import com.likeazusa2.dgmodules.entity.DraconicShieldDomeCoreEntity;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

import java.util.Objects;

public class DraconicShieldDomeCoreRenderer extends EntityRenderer<DraconicShieldDomeCoreEntity> {

    private static final ResourceLocation SHIELD_DUMMY_TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final int MIN_LAT_STEPS = 28;
    private static final int MAX_LAT_STEPS = 56;
    private static final int MIN_LON_STEPS = 56;
    private static final int MAX_LON_STEPS = 112;
    private static final RenderStateShard.TransparencyStateShard DOME_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "dgmodules_dome_transparency",
            () -> {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            },
            com.mojang.blaze3d.systems.RenderSystem::disableBlend
    );
    private static final RenderStateShard.DepthTestStateShard LEQUAL_DEPTH = new RenderStateShard.DepthTestStateShard("dgmodules_lequal_depth", 515);
    private static final RenderStateShard.CullStateShard NO_CULL = new RenderStateShard.CullStateShard(false);
    private static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
    private static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);
    private static final RenderStateShard.WriteMaskStateShard COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);
    private static final RenderStateShard.OutputStateShard TRANSLUCENT_TARGET = new RenderStateShard.OutputStateShard(
            "dgmodules_translucent_target",
            () -> Minecraft.getInstance().levelRenderer.entityTarget().bindWrite(false),
            () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false)
    );
    private static final RenderType DOME_SHIELD_TYPE = RenderType.create(
            "dgmodules:draconic_shield_dome",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.TRIANGLES,
            256,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> {
                        if (DraconicShieldDomeClientEvents.DOME_WAVE_SHADER != null) {
                            return DraconicShieldDomeClientEvents.DOME_WAVE_SHADER;
                        }
                        return Objects.requireNonNull(DEShaders.CHESTPIECE_SHIELD_SHADER).getShaderInstance();
                    }))
                    .setTextureState(new RenderStateShard.TextureStateShard(SHIELD_DUMMY_TEXTURE, false, false))
                    .setTransparencyState(DOME_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH)
                    .setCullState(NO_CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)
    );

    public DraconicShieldDomeCoreRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(DraconicShieldDomeCoreEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        double r = entity.getDomeRadius() + 1.0D;
        AABB visualBox = new AABB(
                entity.getX() - r, entity.getY() - 1.0D, entity.getZ() - r,
                entity.getX() + r, entity.getY() + r, entity.getZ() + r
        );
        return frustum.isVisible(visualBox);
    }

    @Override
    public void render(DraconicShieldDomeCoreEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float radius = entity.getDomeRadius();
        float time = entity.tickCount + partialTick;

        poseStack.pushPose();
        renderShieldDome(entity, poseStack, buffer, packedLight, radius, time);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderShieldDome(DraconicShieldDomeCoreEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float radius, float time) {
        float flash = Mth.clamp(entity.getHitFlashTicks() / 6.0F, 0.0F, 1.0F);
        float fade = 1.0F;
        try {
            fade = entity.getFadeFactor();
        } catch (Throwable ignored) {
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f normalMat = pose.normal();
        VertexConsumer vc = buffer.getBuffer(DOME_SHIELD_TYPE);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(entity.position());
        boolean insideView = cameraPos.lengthSqr() < (radius * radius);

        float pulse = 0.74F + 0.26F * (0.5F + 0.5F * Mth.sin(time * 0.28F));
        float outerR = Mth.lerp(flash, 0.42F, 1.0F);
        float outerG = Mth.lerp(flash, 0.02F, 0.11F);
        float outerB = Mth.lerp(flash, 0.02F, 0.10F);
        float outerA = Mth.lerp(flash, 0.94F, 0.82F) * fade;
        int latSteps = Mth.clamp(Mth.ceil(radius * 4.0F), MIN_LAT_STEPS, MAX_LAT_STEPS);
        int lonSteps = Mth.clamp(latSteps * 2, MIN_LON_STEPS, MAX_LON_STEPS);
        safeSetUniforms(time * 1.55F, outerR, outerG, outerB, outerA, (0.28F + pulse * 0.06F) * fade, 1.16F + pulse * 0.14F, fade, cameraPos, insideView);
        renderHemisphereMesh(vc, mat, normalMat, packedLight, radius, time, latSteps, lonSteps);

        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(DOME_SHIELD_TYPE);
        }
    }

    private static void safeSetUniforms(float time, float r, float g, float b, float a, float baseAlpha, float strength, float activation, Vec3 camPos, boolean insideView) {
        ShaderInstance shader = DraconicShieldDomeClientEvents.DOME_WAVE_SHADER;
        if (shader == null) return;

        try {
            Uniform uTime = shader.getUniform("u_Time");
            if (uTime != null) uTime.set(time / 12.0F);

            Uniform uStrength = shader.getUniform("u_Strength");
            if (uStrength != null) uStrength.set(strength);

            Uniform uAlpha = shader.getUniform("u_Alpha");
            if (uAlpha != null) uAlpha.set(baseAlpha);

            Uniform uAct = shader.getUniform("Activation");
            if (uAct != null) uAct.set(activation);

            Uniform uBase = shader.getUniform("u_BaseColor");
            if (uBase != null) uBase.set(r, g, b, a);

            Uniform uCamPos = shader.getUniform("u_CamPos");
            if (uCamPos != null) uCamPos.set((float) camPos.x, (float) camPos.y, (float) camPos.z);

            Uniform uInside = shader.getUniform("u_Inside");
            if (uInside != null) uInside.set(insideView ? 1.0F : 0.0F);
        } catch (NullPointerException ignored) {
        }
    }

    private void renderHemisphereMesh(VertexConsumer vc, Matrix4f mat, Matrix3f normalMat, int packedLight, float radius, float time, int latSteps, int lonSteps) {
        for (int i = 0; i < latSteps; i++) {
            float theta1 = Mth.HALF_PI * (i / (float) latSteps);
            float theta2 = Mth.HALF_PI * ((i + 1) / (float) latSteps);

            for (int j = 0; j < lonSteps; j++) {
                float phi1 = Mth.TWO_PI * (j / (float) lonSteps);
                float phi2 = Mth.TWO_PI * ((j + 1) / (float) lonSteps);

                Vec3 p1 = sphere(radius, theta1, phi1);
                Vec3 p2 = sphere(radius, theta2, phi1);
                Vec3 p3 = sphere(radius, theta2, phi2);
                Vec3 p4 = sphere(radius, theta1, phi2);

                putVertex(vc, mat, normalMat, packedLight, p1, time);
                putVertex(vc, mat, normalMat, packedLight, p2, time);
                putVertex(vc, mat, normalMat, packedLight, p3, time);

                putVertex(vc, mat, normalMat, packedLight, p1, time);
                putVertex(vc, mat, normalMat, packedLight, p3, time);
                putVertex(vc, mat, normalMat, packedLight, p4, time);
            }
        }
    }

    private static Vec3 sphere(float r, float theta, float phi) {
        float x = r * Mth.sin(theta) * Mth.cos(phi);
        float y = r * Mth.cos(theta);
        float z = r * Mth.sin(theta) * Mth.sin(phi);
        return new Vec3(x, y, z);
    }

    private void putVertex(VertexConsumer vc, Matrix4f mat, Matrix3f normalMat, int packedLight, Vec3 p, float time) {
        float u = (float) (p.x * 0.08F + 0.5F + (time * 0.002F));
        float v = (float) (p.z * 0.08F + 0.5F + (time * 0.0015F));
        Vec3 n = p.normalize();

        vc.vertex(mat, (float) p.x, (float) p.y, (float) p.z)
                .color(96, 255, 255, 210)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMat, (float) n.x, (float) n.y, (float) n.z)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DraconicShieldDomeCoreEntity entity) {
        return SHIELD_DUMMY_TEXTURE;
    }
}
