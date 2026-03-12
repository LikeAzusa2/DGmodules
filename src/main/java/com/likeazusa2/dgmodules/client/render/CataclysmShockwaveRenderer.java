package com.likeazusa2.dgmodules.client.render;

import com.likeazusa2.dgmodules.DGModules;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = DGModules.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CataclysmShockwaveRenderer {
    private static final RenderType SHOCKWAVE_TYPE = RenderType.create(
            "dgmodules:cataclysm_shockwave",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.PARTICLES_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final List<ShockwaveInstance> ACTIVE = new ArrayList<>();

    private CataclysmShockwaveRenderer() {}

    public static void spawn(double x, double y, double z, float radius, int durationTicks, long startGameTime) {
        ACTIVE.add(new ShockwaveInstance(new Vec3(x, y, z), Math.max(0.5F, radius), Math.max(1, durationTicks), startGameTime));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE.isEmpty()) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long gameTime = mc.level.getGameTime();
        ACTIVE.removeIf(instance -> instance.isExpired(gameTime, partialTick));
        if (ACTIVE.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(SHOCKWAVE_TYPE);
        for (ShockwaveInstance instance : ACTIVE) {
            renderShockwave(instance, gameTime, partialTick, consumer, matrix);
        }

        poseStack.popPose();
        bufferSource.endBatch(SHOCKWAVE_TYPE);
    }

    private static void renderShockwave(ShockwaveInstance instance, long gameTime, float partialTick, VertexConsumer consumer, Matrix4f matrix) {
        float age = (gameTime - instance.startGameTime) + partialTick;
        float y = (float) instance.center.y + 0.06F;
        float swirl = age * 0.2F;
        float phaseTime = age / instance.durationTicks;

        if (phaseTime <= 1.0F) {
            float progress = Mth.clamp(phaseTime, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - progress) * (1.0F - progress);

            float outerRadius = Math.max(0.35F, instance.radius * eased);
            float coreThickness = Math.max(0.16F, instance.radius * (0.15F - progress * 0.08F));
            float haloThickness = coreThickness * 2.5F;
            float alpha = 0.92F * (1.0F - progress);
            float spikeAlpha = 0.52F * (1.0F - progress);

            drawRing(matrix, consumer, instance.center, y, outerRadius, haloThickness, 255, 48, 24, alpha * 0.40F, swirl, 40);
            drawRing(matrix, consumer, instance.center, y + 0.02F, outerRadius, coreThickness, 255, 92, 72, alpha, -swirl * 1.35F, 48);
            drawRing(matrix, consumer, instance.center, y + 0.04F, Math.max(0.2F, outerRadius * 0.72F), coreThickness * 0.45F, 255, 156, 132, spikeAlpha, swirl * 1.8F, 24);
            return;
        }

        float collapseProgress = Mth.clamp(phaseTime - 1.0F, 0.0F, 1.0F);
        float collapseEase = collapseProgress * collapseProgress * (3.0F - 2.0F * collapseProgress);
        float collapseRadius = Math.max(0.12F, instance.radius * (1.0F - collapseEase));
        float shellThickness = Math.max(0.12F, instance.radius * (0.10F - collapseProgress * 0.06F));
        float innerThickness = Math.max(0.08F, shellThickness * 0.58F);
        float alpha = 0.78F * (1.0F - collapseProgress);
        float emberAlpha = 0.55F * (1.0F - collapseProgress);
        float pullSwirl = swirl * -(1.7F + collapseProgress * 0.8F);

        drawRing(matrix, consumer, instance.center, y + 0.01F, collapseRadius, shellThickness * 2.3F, 150, 0, 0, alpha * 0.26F, pullSwirl, 36);
        drawRing(matrix, consumer, instance.center, y + 0.035F, collapseRadius, shellThickness, 255, 36, 24, alpha, pullSwirl * 1.4F, 44);
        drawRing(matrix, consumer, instance.center, y + 0.06F, Math.max(0.08F, collapseRadius * 0.42F), innerThickness, 255, 120, 100, emberAlpha, -pullSwirl * 1.8F, 20);
    }

    private static void drawRing(Matrix4f matrix, VertexConsumer consumer, Vec3 center, float y, float radius, float thickness, int red, int green, int blue, float alpha, float angleOffset, int segments) {
        float inner = Math.max(0.02F, radius - thickness * 0.5F);
        float outer = radius + thickness * 0.5F;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (a <= 0 || outer <= 0.03F) return;

        for (int i = 0; i < segments; i++) {
            float t0 = (i / (float) segments) * Mth.TWO_PI + angleOffset;
            float t1 = ((i + 1) / (float) segments) * Mth.TWO_PI + angleOffset;

            float wobble0 = 1.0F + 0.035F * Mth.sin(t0 * 6.0F);
            float wobble1 = 1.0F + 0.035F * Mth.sin(t1 * 6.0F);

            float ix0 = (float) center.x + Mth.cos(t0) * inner * wobble0;
            float iz0 = (float) center.z + Mth.sin(t0) * inner * wobble0;
            float ox0 = (float) center.x + Mth.cos(t0) * outer * wobble0;
            float oz0 = (float) center.z + Mth.sin(t0) * outer * wobble0;
            float ix1 = (float) center.x + Mth.cos(t1) * inner * wobble1;
            float iz1 = (float) center.z + Mth.sin(t1) * inner * wobble1;
            float ox1 = (float) center.x + Mth.cos(t1) * outer * wobble1;
            float oz1 = (float) center.z + Mth.sin(t1) * outer * wobble1;

            consumer.addVertex(matrix, ix0, y, iz0).setColor(red, green, blue, a);
            consumer.addVertex(matrix, ox0, y, oz0).setColor(red, green, blue, 0);
            consumer.addVertex(matrix, ox1, y, oz1).setColor(red, green, blue, 0);
            consumer.addVertex(matrix, ix1, y, iz1).setColor(red, green, blue, a);
        }
    }

    private record ShockwaveInstance(Vec3 center, float radius, int durationTicks, long startGameTime) {
        private boolean isExpired(long gameTime, float partialTick) {
            return (gameTime - startGameTime) + partialTick > durationTicks * 2.0F + 1.0F;
        }
    }
}
