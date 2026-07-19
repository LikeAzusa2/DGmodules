package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.config.BooleanProperty;
import com.brandon3055.draconicevolution.api.config.ConfigProperty;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import com.brandon3055.draconicevolution.init.DEModules;
import com.brandon3055.draconicevolution.init.ItemData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class PhaseShieldModuleEntity extends ModuleEntity<NoData> {

    // 紧急启动：玩家濒死时自动开启相位护盾（默认开）
    private Optional<BooleanProperty> emergencyEnabled = Optional.empty();

    public static final Codec<PhaseShieldModuleEntity> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DEModules.codec().fieldOf("module").forGetter(e -> (Module<?>) e.getModule()),
            Codec.INT.fieldOf("gridx").forGetter(ModuleEntity::getGridX),
            Codec.INT.fieldOf("gridy").forGetter(ModuleEntity::getGridY),
            BooleanProperty.CODEC.optionalFieldOf("phase_shield_emergency").forGetter(e -> e.emergencyEnabled.map(BooleanProperty::copy))
    ).apply(inst, (m, x, y, ee) -> {
        PhaseShieldModuleEntity e = new PhaseShieldModuleEntity((Module<NoData>) m, x, y);
        e.emergencyEnabled = ee;
        e.attachListener();
        return e;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhaseShieldModuleEntity> STREAM_CODEC =
            StreamCodec.composite(
                    DEModules.streamCodec(), e -> (Module<?>) e.getModule(),
                    ByteBufCodecs.INT, ModuleEntity::getGridX,
                    ByteBufCodecs.INT, ModuleEntity::getGridY,
                    ByteBufCodecs.optional(BooleanProperty.STREAM_CODEC), e -> e.emergencyEnabled.map(BooleanProperty::copy),
                    (m, x, y, ee) -> {
                        PhaseShieldModuleEntity e = new PhaseShieldModuleEntity((Module<NoData>) m, x, y);
                        e.emergencyEnabled = ee;
                        e.attachListener();
                        return e;
                    }
            );

    public PhaseShieldModuleEntity(Module<NoData> module) {
        super(module);
    }

    public PhaseShieldModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module, gridX, gridY);
    }

    @Override
    public ModuleEntity<?> copy() {
        PhaseShieldModuleEntity e = new PhaseShieldModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
        e.emergencyEnabled = this.emergencyEnabled.map(BooleanProperty::copy);
        e.attachListener();
        return e;
    }

    private void attachListener() {
        emergencyEnabled.ifPresent(p -> p.setChangeListener(stack -> {
            boolean val = p.getValue();
            stack.set(ItemData.BOOL_ITEM_PROP_3.get(), p.copy());
            markDirty();
            syncToPlayerPersistentData(stack, val);
        }));
    }

    @Override
    public void getEntityProperties(List<ConfigProperty> properties) {
        properties.add(getOrCreateEmergencyEnabled());
    }

    private BooleanProperty getOrCreateEmergencyEnabled() {
        return emergencyEnabled.orElseGet(() -> {
            BooleanProperty p = new BooleanProperty(
                    "phase_shield_emergency",
                    Component.translatable("item_prop.draconicevolution.phase_shield_emergency"),
                    true
            );

            p.setChangeListener(stack -> {
                boolean val = p.getValue();
                stack.set(ItemData.BOOL_ITEM_PROP_3.get(), p.copy());
                markDirty();
                syncToPlayerPersistentData(stack, val);
            });

            emergencyEnabled = Optional.of(p);
            return p;
        });
    }

    /** 直接写所有在线玩家的 persistentData，避开 DE ItemStack 引用不一致问题。 */
    private static void syncToPlayerPersistentData(ItemStack hostStack, boolean value) {
        try {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            for (var player : server.getPlayerList().getPlayers()) {
                player.getPersistentData().putBoolean("dg_phase_shield_emergency_enabled", value);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void saveEntityToStack(ItemStack stack, ModuleContext context) {
        stack.set(ItemData.BOOL_ITEM_PROP_3.get(), getOrCreateEmergencyEnabled().copy());
    }

    @Override
    public void loadEntityFromStack(ItemStack stack, ModuleContext context) {
        BooleanProperty ee = stack.get(ItemData.BOOL_ITEM_PROP_3.get());
        if (ee != null) emergencyEnabled = Optional.of(ee.copy());
        attachListener();
    }

    public boolean isEmergencyEnabled() {
        return getOrCreateEmergencyEnabled().getValue();
    }

    public static boolean hostHasPhaseShield(ModuleHost host) {
        for (var ent : host.getModuleEntities()) {
            if (ent instanceof PhaseShieldModuleEntity) return true;
        }
        return false;
    }
}
