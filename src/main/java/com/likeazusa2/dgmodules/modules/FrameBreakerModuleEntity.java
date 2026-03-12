package com.likeazusa2.dgmodules.modules;

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

public class FrameBreakerModuleEntity extends ModuleEntity<NoData> {

    private static final String KEY_ENABLED = "frame_breaker_enabled";
    private static final String KEY_AFFECT_PLAYERS = "affect_players";

    private Optional<BooleanProperty> enabled = Optional.empty();
    private Optional<BooleanProperty> affectPlayers = Optional.empty();

    public static final Codec<FrameBreakerModuleEntity> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DEModules.codec().fieldOf("module").forGetter(e -> (Module<?>) e.getModule()),
            Codec.INT.fieldOf("gridx").forGetter(ModuleEntity::getGridX),
            Codec.INT.fieldOf("gridy").forGetter(ModuleEntity::getGridY),
            BooleanProperty.CODEC.optionalFieldOf("enabled").forGetter(e -> e.enabled.map(BooleanProperty::copy)),
            BooleanProperty.CODEC.optionalFieldOf("affect_players").forGetter(e -> e.affectPlayers.map(BooleanProperty::copy))
    ).apply(inst, (m, x, y, en, ap) -> {
        FrameBreakerModuleEntity e = new FrameBreakerModuleEntity((Module<NoData>) m, x, y);
        e.enabled = en.map(v -> e.createEnabled(v.getValue()));
        e.affectPlayers = ap.map(v -> e.createAffectPlayers(v.getValue()));
        e.attachListeners();
        return e;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, FrameBreakerModuleEntity> STREAM_CODEC =
            StreamCodec.composite(
                    DEModules.streamCodec(), e -> (Module<?>) e.getModule(),
                    ByteBufCodecs.INT, ModuleEntity::getGridX,
                    ByteBufCodecs.INT, ModuleEntity::getGridY,
                    ByteBufCodecs.optional(BooleanProperty.STREAM_CODEC), e -> e.enabled.map(BooleanProperty::copy),
                    ByteBufCodecs.optional(BooleanProperty.STREAM_CODEC), e -> e.affectPlayers.map(BooleanProperty::copy),
                    (m, x, y, en, ap) -> {
                        FrameBreakerModuleEntity e = new FrameBreakerModuleEntity((Module<NoData>) m, x, y);
                        e.enabled = en.map(v -> e.createEnabled(v.getValue()));
                        e.affectPlayers = ap.map(v -> e.createAffectPlayers(v.getValue()));
                        e.attachListeners();
                        return e;
                    }
            );

    public FrameBreakerModuleEntity(Module<NoData> module) {
        super(module);
    }

    public FrameBreakerModuleEntity(Module<NoData> module, int gridX, int gridY) {
        super(module, gridX, gridY);
    }

    @Override
    public ModuleEntity<?> copy() {
        FrameBreakerModuleEntity e = new FrameBreakerModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
        e.enabled = this.enabled.map(BooleanProperty::copy).map(v -> e.createEnabled(v.getValue()));
        e.affectPlayers = this.affectPlayers.map(BooleanProperty::copy).map(v -> e.createAffectPlayers(v.getValue()));
        e.attachListeners();
        return e;
    }

    private BooleanProperty createEnabled(boolean value) {
        return new BooleanProperty(
                KEY_ENABLED,
                Component.translatable("item_prop.dgmodules.frame_breaker_enabled"),
                value
        );
    }

    private BooleanProperty createAffectPlayers(boolean value) {
        return new BooleanProperty(
                KEY_AFFECT_PLAYERS,
                Component.translatable("item_prop.dgmodules.affect_players"),
                value
        );
    }

    private void attachListeners() {
        enabled.ifPresent(p -> p.setChangeListener(stack -> {
            stack.set(ItemData.BOOL_ITEM_PROP_1.get(), p.copy());
            markDirty();
        }));
        affectPlayers.ifPresent(p -> p.setChangeListener(stack -> {
            stack.set(ItemData.BOOL_ITEM_PROP_2.get(), p.copy());
            markDirty();
        }));
    }

    @Override
    public void getEntityProperties(List<ConfigProperty> properties) {
        properties.add(getOrCreateEnabled());
        properties.add(getOrCreateAffectPlayers());
    }

    private BooleanProperty getOrCreateEnabled() {
        return enabled.orElseGet(() -> {
            BooleanProperty p = createEnabled(true);
            p.setChangeListener(stack -> {
                stack.set(ItemData.BOOL_ITEM_PROP_1.get(), p.copy());
                markDirty();
            });
            enabled = Optional.of(p);
            return p;
        });
    }

    private BooleanProperty getOrCreateAffectPlayers() {
        return affectPlayers.orElseGet(() -> {
            BooleanProperty p = createAffectPlayers(false);
            p.setChangeListener(stack -> {
                stack.set(ItemData.BOOL_ITEM_PROP_2.get(), p.copy());
                markDirty();
            });
            affectPlayers = Optional.of(p);
            return p;
        });
    }

    @Override
    public void saveEntityToStack(ItemStack stack, ModuleContext context) {
        stack.set(ItemData.BOOL_ITEM_PROP_1.get(), getOrCreateEnabled().copy());
        stack.set(ItemData.BOOL_ITEM_PROP_2.get(), getOrCreateAffectPlayers().copy());
    }

    @Override
    public void loadEntityFromStack(ItemStack stack, ModuleContext context) {
        BooleanProperty en = stack.get(ItemData.BOOL_ITEM_PROP_1.get());
        BooleanProperty ap = stack.get(ItemData.BOOL_ITEM_PROP_2.get());

        enabled = Optional.empty();
        affectPlayers = Optional.empty();

        if (en != null) enabled = Optional.of(createEnabled(en.getValue()));
        if (ap != null) affectPlayers = Optional.of(createAffectPlayers(ap.getValue()));

        getOrCreateEnabled();
        getOrCreateAffectPlayers();
        attachListeners();
    }

    public boolean isEnabled() {
        return getOrCreateEnabled().getValue();
    }

    public boolean isAffectPlayers() {
        return getOrCreateAffectPlayers().getValue();
    }
}
