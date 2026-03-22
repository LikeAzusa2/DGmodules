package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.config.BooleanProperty;
import com.brandon3055.draconicevolution.api.config.ConfigProperty;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class FrameBreakerModuleEntity extends ModuleEntity<NoData> {
    private static final String KEY_ENABLED = "frame_breaker_enabled";
    private static final String KEY_AFFECT_PLAYERS = "affect_players";

    private Optional<BooleanProperty> enabled = Optional.empty();
    private Optional<BooleanProperty> affectPlayers = Optional.empty();

    public FrameBreakerModuleEntity(Module<NoData> module) {
        super(module);
        enabled = Optional.of((BooleanProperty) addProperty(createEnabled(true)));
        affectPlayers = Optional.of((BooleanProperty) addProperty(createAffectPlayers(false)));
        savePropertiesToItem = true;
    }

    public FrameBreakerModuleEntity(Module<NoData> module, int gridX, int gridY) {
        this(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        FrameBreakerModuleEntity entity = new FrameBreakerModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
        entity.getOrCreateEnabled().setValue(getOrCreateEnabled().getValue());
        entity.getOrCreateAffectPlayers().setValue(getOrCreateAffectPlayers().getValue());
        return entity;
    }

    private BooleanProperty createEnabled(boolean value) {
        return new BooleanProperty(KEY_ENABLED, Component.translatable("item_prop.dgmodules.frame_breaker_enabled"), value);
    }

    private BooleanProperty createAffectPlayers(boolean value) {
        return new BooleanProperty(KEY_AFFECT_PLAYERS, Component.translatable("item_prop.dgmodules.affect_players"), value);
    }

    private void attachListeners() {
        enabled.ifPresent(property -> property.setChangeListener(() -> {}));
        affectPlayers.ifPresent(property -> property.setChangeListener(() -> {}));
    }

    public void getEntityProperties(List<ConfigProperty> properties) {
        properties.add(getOrCreateEnabled());
        properties.add(getOrCreateAffectPlayers());
    }

    private BooleanProperty getOrCreateEnabled() {
        return enabled.orElseGet(() -> {
            BooleanProperty property = createEnabled(true);
            property.setChangeListener(() -> {});
            enabled = Optional.of(property);
            return property;
        });
    }

    private BooleanProperty getOrCreateAffectPlayers() {
        return affectPlayers.orElseGet(() -> {
            BooleanProperty property = createAffectPlayers(false);
            property.setChangeListener(() -> {});
            affectPlayers = Optional.of(property);
            return property;
        });
    }

    @Override
    public void writeToItemStack(ItemStack stack, ModuleContext context) {
        super.writeToItemStack(stack, context);
        writeExtraData(stack.getOrCreateTag());
    }

    @Override
    public void readFromItemStack(ItemStack stack, ModuleContext context) {
        super.readFromItemStack(stack, context);
    }

    @Override
    protected CompoundTag writeExtraData(CompoundTag nbt) {
        nbt.putBoolean(KEY_ENABLED, getOrCreateEnabled().getValue());
        nbt.putBoolean(KEY_AFFECT_PLAYERS, getOrCreateAffectPlayers().getValue());
        return nbt;
    }

    @Override
    protected void readExtraData(CompoundTag nbt) {
        if (nbt.contains(KEY_ENABLED)) {
            getOrCreateEnabled().setValue(nbt.getBoolean(KEY_ENABLED));
        }
        if (nbt.contains(KEY_AFFECT_PLAYERS)) {
            getOrCreateAffectPlayers().setValue(nbt.getBoolean(KEY_AFFECT_PLAYERS));
        }
    }

    public boolean isEnabled() {
        return getOrCreateEnabled().getValue();
    }

    public boolean isAffectPlayers() {
        return getOrCreateAffectPlayers().getValue();
    }
}
