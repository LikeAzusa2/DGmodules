package com.likeazusa2.dgmodules.client.render;

import com.brandon3055.draconicevolution.entity.GuardianCrystalEntity;
import com.likeazusa2.dgmodules.DGModules;
import com.likeazusa2.dgmodules.logic.ChaosCrystalBreakerLogic;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders the breaker detonation as a custom singularity diffusion effect.
 * It deliberately does not use Minecraft's particle engine.
 */
@EventBusSubscriber(modid = DGModules.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ChaosCrystalSingularityRenderer {

    private static final RenderType SINGULARITY_TYPE = RenderType.create(
            "dgmodules:chaos_crystal_singularity",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(true)
    );

    private static final List<Singularity> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Countdown> ACTIVE_COUNTDOWNS = new HashMap<>();

    private ChaosCrystalSingularityRenderer() {
    }

    public static void handle(double x, double y, double z, float radius, int effectTicks,
                              long startGameTime, UUID crystalId,
                              int defenseDurationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ChaosCrystalBreakerLogic.activateClient(
                    minecraft.level,
                    crystalId,
                    defenseDurationTicks
            );
        }
        spawn(x, y, z, radius, effectTicks, startGameTime);
    }

    public static void spawn(double x, double y, double z, float radius, int effectTicks, long startGameTime) {
        ACTIVE.add(new Singularity(
                new Vec3(x, y, z),
                Math.max(0.5F, radius),
                Math.max(1, effectTicks),
                startGameTime
        ));
        if (ACTIVE.size() > 16) {
            ACTIVE.remove(0);
        }
    }

    public static void handleCountdown(UUID crystalId, long detonationGameTime) {
        if (crystalId == null) return;
        ACTIVE_COUNTDOWNS.put(crystalId, new Countdown(detonationGameTime));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || (ACTIVE.isEmpty() && ACTIVE_COUNTDOWNS.isEmpty())) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long gameTime = minecraft.level.getGameTime();
        ACTIVE.removeIf(effect -> effect.isExpired(gameTime, partialTick));
        pruneCountdowns(minecraft.level, gameTime);
        if (ACTIVE.isEmpty() && ACTIVE_COUNTDOWNS.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        if (!ACTIVE.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = buffer.getBuffer(SINGULARITY_TYPE);
            for (Singularity effect : ACTIVE) {
                renderSingularity(effect, gameTime, partialTick, matrix, consumer);
            }

            poseStack.popPose();
            buffer.endBatch(SINGULARITY_TYPE);
        }

        renderCountdowns(minecraft, poseStack, cameraPos, buffer, gameTime, partialTick);
        buffer.endBatch();
    }

    private static void pruneCountdowns(ClientLevel level, long gameTime) {
        Iterator<Map.Entry<UUID, Countdown>> iterator = ACTIVE_COUNTDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Countdown> entry = iterator.next();
            if (entry.getValue().detonationGameTime() <= gameTime) {
                iterator.remove();
                continue;
            }

            Entity entity = findEntity(level, entry.getKey());
            if (entity != null && (!entity.isAlive() || !(entity instanceof GuardianCrystalEntity))) {
                iterator.remove();
            }
        }
    }

    private static void renderCountdowns(Minecraft minecraft, PoseStack poseStack, Vec3 cameraPos,
                                         MultiBufferSource.BufferSource buffer, long gameTime,
                                         float partialTick) {
        if (ACTIVE_COUNTDOWNS.isEmpty()) return;

        Font font = minecraft.font;
        for (Map.Entry<UUID, Countdown> entry : ACTIVE_COUNTDOWNS.entrySet()) {
            long remainingTicks = entry.getValue().detonationGameTime() - gameTime;
            Entity entity = findEntity(minecraft.level, entry.getKey());
            if (!(entity instanceof GuardianCrystalEntity crystal) || !crystal.isAlive()) continue;

            int seconds = Math.max(1, Mth.ceil(remainingTicks / 20.0F));
            String text = Integer.toString(seconds);
            Vec3 entityPosition = crystal.getPosition(partialTick);
            double textY = entityPosition.y + crystal.getBbHeight() + 0.75D;

            poseStack.pushPose();
            poseStack.translate(
                    entityPosition.x - cameraPos.x,
                    textY - cameraPos.y,
                    entityPosition.z - cameraPos.z
            );
            poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(0.04F, -0.04F, 0.04F);

            float textWidth = font.width(text);
            font.drawInBatch(
                    text,
                    -textWidth / 2.0F,
                    0.0F,
                    0xFFFF5CFF,
                    true,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0xA0001028,
                    LightTexture.FULL_BRIGHT
            );
            poseStack.popPose();
        }
    }

    private static Entity findEntity(ClientLevel level, UUID entityId) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entityId.equals(entity.getUUID())) return entity;
        }
        return null;
    }

    private static void renderSingularity(Singularity effect, long gameTime, float partialTick,
                                           Matrix4f matrix, VertexConsumer consumer) {
        float age = (gameTime - effect.startGameTime()) + partialTick;
        float progress = Mth.clamp(age / effect.effectTicks(), 0.0F, 1.0F);
        float spread = Mth.clamp(progress / 0.68F, 0.0F, 1.0F);
        float collapse = Mth.clamp((progress - 0.68F) / 0.32F, 0.0F, 1.0F);
        float easedSpread = 1.0F - (1.0F - spread) * (1.0F - spread);
        float rotation = age * 0.075F;

        float outerRadius = effect.radius() * (0.12F + easedSpread * 0.88F);
        outerRadius *= 1.0F - collapse * 0.32F;
        float waveAlpha = 0.78F * (1.0F - collapse);
        float shellAlpha = 0.26F * (1.0F - collapse * 0.85F);
        float coreRadius = effect.radius() * (0.12F + collapse * 0.05F);

        drawRing(matrix, consumer, effect.center(), outerRadius, Math.max(0.12F, effect.radius() * 0.07F),
                Plane.XZ, 155, 38, 255, waveAlpha, rotation, 72);
        drawRing(matrix, consumer, effect.center(), outerRadius * 0.86F, Math.max(0.08F, effect.radius() * 0.035F),
                Plane.XY, 255, 38, 190, waveAlpha * 0.68F, -rotation * 1.35F, 56);
        drawRing(matrix, consumer, effect.center(), outerRadius * 0.72F, Math.max(0.08F, effect.radius() * 0.04F),
                Plane.YZ, 64, 105, 255, waveAlpha * 0.60F, rotation * 1.7F, 56);

        drawSphereShell(matrix, consumer, effect.center(), Math.max(0.16F, outerRadius * 0.92F),
                12, 36, 86, 12, 150, shellAlpha, rotation * 0.8F);
        drawSphereShell(matrix, consumer, effect.center(), coreRadius,
                8, 24, 22, 0, 30, 0.72F * (1.0F - progress * 0.45F), -rotation * 1.5F);

        // A second, thin shell gives the expanding wave a sharp singularity
        // edge without relying on a texture or the particle renderer.
        drawSphereShell(matrix, consumer, effect.center(), Math.max(0.12F, outerRadius * 0.52F),
                8, 24, 210, 24, 120, waveAlpha * 0.55F, -rotation * 1.9F);
    }

    private static void drawSphereShell(Matrix4f matrix, VertexConsumer consumer, Vec3 center,
                                        float radius, int latitudeSteps, int longitudeSteps,
                                        int red, int green, int blue, float alpha, float rotation) {
        if (radius <= 0.01F || alpha <= 0.0F) return;

        int opaqueAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        for (int lat = 0; lat < latitudeSteps; lat++) {
            float theta0 = Mth.PI * (lat / (float) latitudeSteps);
            float theta1 = Mth.PI * ((lat + 1) / (float) latitudeSteps);

            for (int lon = 0; lon < longitudeSteps; lon++) {
                float phi0 = Mth.TWO_PI * (lon / (float) longitudeSteps) + rotation;
                float phi1 = Mth.TWO_PI * ((lon + 1) / (float) longitudeSteps) + rotation;

                Vec3 p0 = spherePoint(center, radius, theta0, phi0);
                Vec3 p1 = spherePoint(center, radius, theta1, phi0);
                Vec3 p2 = spherePoint(center, radius, theta1, phi1);
                Vec3 p3 = spherePoint(center, radius, theta0, phi1);

                consumer.addVertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z)
                        .setColor(red, green, blue, opaqueAlpha);
                consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                        .setColor(red, green, blue, opaqueAlpha / 2);
                consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                        .setColor(red, green, blue, opaqueAlpha / 2);
                consumer.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                        .setColor(red, green, blue, opaqueAlpha);
            }
        }
    }

    private static Vec3 spherePoint(Vec3 center, float radius, float theta, float phi) {
        float sinTheta = Mth.sin(theta);
        return new Vec3(
                center.x + radius * sinTheta * Mth.cos(phi),
                center.y + radius * Mth.cos(theta),
                center.z + radius * sinTheta * Mth.sin(phi)
        );
    }

    private static void drawRing(Matrix4f matrix, VertexConsumer consumer, Vec3 center,
                                 float radius, float thickness, Plane plane,
                                 int red, int green, int blue, float alpha,
                                 float rotation, int segments) {
        int opaqueAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (opaqueAlpha <= 0 || radius <= 0.02F) return;

        float inner = Math.max(0.02F, radius - thickness * 0.5F);
        float outer = radius + thickness * 0.5F;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * (i / (float) segments) + rotation;
            float a1 = Mth.TWO_PI * ((i + 1) / (float) segments) + rotation;
            float wobble0 = 1.0F + Mth.sin(a0 * 5.0F + rotation * 2.0F) * 0.035F;
            float wobble1 = 1.0F + Mth.sin(a1 * 5.0F + rotation * 2.0F) * 0.035F;

            Vec3 i0 = planePoint(center, inner * wobble0, a0, plane);
            Vec3 o0 = planePoint(center, outer * wobble0, a0, plane);
            Vec3 o1 = planePoint(center, outer * wobble1, a1, plane);
            Vec3 i1 = planePoint(center, inner * wobble1, a1, plane);

            consumer.addVertex(matrix, (float) i0.x, (float) i0.y, (float) i0.z)
                    .setColor(red, green, blue, opaqueAlpha);
            consumer.addVertex(matrix, (float) o0.x, (float) o0.y, (float) o0.z)
                    .setColor(red, green, blue, opaqueAlpha / 4);
            consumer.addVertex(matrix, (float) o1.x, (float) o1.y, (float) o1.z)
                    .setColor(red, green, blue, opaqueAlpha / 4);
            consumer.addVertex(matrix, (float) i1.x, (float) i1.y, (float) i1.z)
                    .setColor(red, green, blue, opaqueAlpha);
        }
    }

    private static Vec3 planePoint(Vec3 center, float radius, float angle, Plane plane) {
        float c = Mth.cos(angle) * radius;
        float s = Mth.sin(angle) * radius;
        return switch (plane) {
            case XZ -> new Vec3(center.x + c, center.y, center.z + s);
            case XY -> new Vec3(center.x + c, center.y + s, center.z);
            case YZ -> new Vec3(center.x, center.y + c, center.z + s);
        };
    }

    private enum Plane {
        XZ,
        XY,
        YZ
    }

    private record Singularity(Vec3 center, float radius, int effectTicks, long startGameTime) {
        private boolean isExpired(long gameTime, float partialTick) {
            return (gameTime - startGameTime) + partialTick > effectTicks + 1.0F;
        }
    }

    private record Countdown(long detonationGameTime) {
    }
}
