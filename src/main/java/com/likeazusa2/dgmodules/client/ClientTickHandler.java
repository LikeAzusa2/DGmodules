package com.likeazusa2.dgmodules.client;

import com.brandon3055.draconicevolution.handlers.DESounds;
import com.likeazusa2.dgmodules.client.sound.ChaosLaserBeamSound;
import com.likeazusa2.dgmodules.client.sound.ChaosLaserChargingSound;
import com.likeazusa2.dgmodules.network.C2SChaosLaser;
import com.likeazusa2.dgmodules.network.C2SFlightTunerInput;
import com.likeazusa2.dgmodules.network.C2SPhaseShieldToggle;
import com.likeazusa2.dgmodules.network.NetworkHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "dgmodules", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientTickHandler {
    private static long cooldownEndClient = 0;
    private static boolean serverFiring = false;
    private static byte serverPhase = -1;
    private static boolean lastPhysicalDown = false;
    private static boolean lastPhaseShieldKey = false;
    private static boolean suppressUntilRelease = false;
    private static boolean lastJump = false;
    private static boolean lastSneak = false;
    private static float lastZza = 0F;
    private static float lastXxa = 0F;
    private static int flightInputCooldown = 0;
    private static ChaosLaserChargingSound chargingSound;
    private static ChaosLaserBeamSound beamSound;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            hardStopAll(false);
            ClientPhaseShieldSound.stop();
            ClientPhaseShieldSound.stopAllEntitySounds();
            lastPhysicalDown = false;
            suppressUntilRelease = false;
            return;
        }

        if (mc.screen != null || mc.isPaused()) {
            if (lastPhysicalDown) {
                NetworkHandler.sendToServer(new C2SChaosLaser(false));
            }
            hardStopAll(false);
            ClientPhaseShieldSound.stop();
            ClientPhaseShieldSound.stopAllEntitySounds();
            lastPhysicalDown = false;
            suppressUntilRelease = false;
            return;
        }

        long now = mc.level.getGameTime();
        LocalPlayer lp = mc.player;
        if (lp != null) {
            boolean jump = lp.input != null && lp.input.jumping;
            boolean sneak = lp.input != null && lp.input.shiftKeyDown;
            float zza = lp.zza;
            float xxa = lp.xxa;

            flightInputCooldown++;
            boolean changed = (jump != lastJump) || (sneak != lastSneak) || (zza != lastZza) || (xxa != lastXxa);
            if (changed || flightInputCooldown >= 3) {
                flightInputCooldown = 0;
                lastJump = jump;
                lastSneak = sneak;
                lastZza = zza;
                lastXxa = xxa;
                NetworkHandler.sendToServer(new C2SFlightTunerInput(jump, sneak, zza, xxa));
            }
        }

        boolean inCooldown = now < cooldownEndClient;
        boolean physicalDown = pollKeyDown(mc);

        boolean phaseDown = pollKeyDown(mc, ClientKeybinds.PHASE_SHIELD_KEY);
        if (phaseDown && !lastPhaseShieldKey) {
            NetworkHandler.sendToServer(new C2SPhaseShieldToggle());
        }
        lastPhaseShieldKey = phaseDown;

        if (suppressUntilRelease) {
            if (!physicalDown) {
                suppressUntilRelease = false;
            }
            physicalDown = false;
        }

        if (inCooldown) {
            physicalDown = false;
            if (!serverFiring) {
                stopChargingSound();
                stopBeamSound();
            }
        }

        if (physicalDown && !lastPhysicalDown) {
            NetworkHandler.sendToServer(new C2SChaosLaser(true));
        } else if (!physicalDown && lastPhysicalDown) {
            NetworkHandler.sendToServer(new C2SChaosLaser(false));
            stopChargingSound();
            stopBeamSound();
        }

        lastPhysicalDown = physicalDown;

        if (chargingSound != null && chargingSound.isDone()) {
            chargingSound = null;
        }
        if (beamSound != null && beamSound.isDone()) {
            beamSound = null;
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.screen != null || mc.isPaused()) {
            return;
        }
    }

    private static boolean pollKeyDown(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        InputConstants.Key key = ClientKeybinds.CHAOS_LASER_KEY.getKey();
        if (key == null || key.getType() == null) {
            return false;
        }

        return switch (key.getType()) {
            case KEYSYM, SCANCODE -> GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    private static boolean pollKeyDown(Minecraft mc, net.minecraft.client.KeyMapping mapping) {
        long window = mc.getWindow().getWindow();
        InputConstants.Key key = mapping.getKey();
        if (key == null || key.getType() == null) {
            return false;
        }

        return switch (key.getType()) {
            case KEYSYM, SCANCODE -> GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    public static void applyServerState(boolean firing, byte phase, long cooldownEndTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (cooldownEndTick > cooldownEndClient) {
            cooldownEndClient = cooldownEndTick;
        }

        serverFiring = firing;
        serverPhase = phase;

        if (!firing) {
            stopChargingSound();
            stopBeamSound();
            if (lastPhysicalDown) {
                suppressUntilRelease = true;
            }
            return;
        }

        LocalPlayer player = mc.player;
        if (phase == 0) {
            startChargingSound(mc, player);
            stopBeamSound();
        } else if (phase == 1) {
            stopChargingSound();
            startBeamSound(mc, DESounds.BEAM.get());
        } else if (phase == 2) {
            stopChargingSound();
            startBeamSound(mc, DESounds.BEAM.get());
            if (beamSound != null && !beamSound.isDone()) {
                beamSound.setPhaseExecute();
            }
        }
    }

    private static void hardStopAll(boolean sendStopPacketIfNeeded) {
        if (sendStopPacketIfNeeded) {
            NetworkHandler.sendToServer(new C2SChaosLaser(false));
        }
        stopChargingSound();
        stopBeamSound();
    }

    private static void startChargingSound(Minecraft mc, LocalPlayer player) {
        if (chargingSound != null && !chargingSound.isDone()) {
            return;
        }
        chargingSound = new ChaosLaserChargingSound(player, 60);
        mc.getSoundManager().play(chargingSound);
    }

    private static void startBeamSound(Minecraft mc, SoundEvent event) {
        if (beamSound != null && !beamSound.isDone()) {
            return;
        }
        beamSound = new ChaosLaserBeamSound(event);
        mc.getSoundManager().play(beamSound);
    }

    private static void stopChargingSound() {
        if (chargingSound != null) {
            chargingSound.requestStop();
            chargingSound = null;
        }
    }

    private static void stopBeamSound() {
        if (beamSound != null) {
            beamSound.requestStop();
            beamSound = null;
        }
    }
}
