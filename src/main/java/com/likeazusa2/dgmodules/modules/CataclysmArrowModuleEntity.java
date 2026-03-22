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

public class CataclysmArrowModuleEntity extends ModuleEntity<NoData> {
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HIGH_FREQUENCY_PIERCE = "high_frequency_pierce";
    private static final String KEY_SUMMON_FIREBALL = "summon_fireball";

    private Optional<BooleanProperty> enabled = Optional.empty();
    private Optional<BooleanProperty> highFrequencyPierce = Optional.empty();
    private Optional<BooleanProperty> summonFireball = Optional.empty();

    public CataclysmArrowModuleEntity(Module<NoData> module) {
        super(module);
        enabled = Optional.of((BooleanProperty) addProperty(createEnabled(true)));
        highFrequencyPierce = Optional.of((BooleanProperty) addProperty(createHighFrequencyPierce(true)));
        summonFireball = Optional.of((BooleanProperty) addProperty(createSummonFireball(true)));
        savePropertiesToItem = true;
    }

    public CataclysmArrowModuleEntity(Module<NoData> module, int gridX, int gridY) {
        this(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        CataclysmArrowModuleEntity entity = new CataclysmArrowModuleEntity((Module<NoData>) this.module, getGridX(), getGridY());
        entity.getOrCreateEnabled().setValue(getOrCreateEnabled().getValue());
        entity.getOrCreateHighFrequencyPierce().setValue(getOrCreateHighFrequencyPierce().getValue());
        entity.getOrCreateSummonFireball().setValue(getOrCreateSummonFireball().getValue());
        return entity;
    }

    private void attachListeners() {
        enabled.ifPresent(property -> property.setChangeListener(() -> {}));
        highFrequencyPierce.ifPresent(property -> property.setChangeListener(() -> {}));
        summonFireball.ifPresent(property -> property.setChangeListener(() -> {}));
    }

    public void getEntityProperties(List<ConfigProperty> properties) {
        properties.add(getOrCreateEnabled());
        properties.add(getOrCreateHighFrequencyPierce());
        properties.add(getOrCreateSummonFireball());
    }

    private BooleanProperty createEnabled(boolean value) {
        return new BooleanProperty(KEY_ENABLED, Component.translatable("dgmodules.module.cataclysm_arrow.enabled"), value);
    }

    private BooleanProperty createHighFrequencyPierce(boolean value) {
        return new BooleanProperty(KEY_HIGH_FREQUENCY_PIERCE, Component.translatable("dgmodules.module.cataclysm_arrow.high_frequency_pierce"), value);
    }

    private BooleanProperty createSummonFireball(boolean value) {
        return new BooleanProperty(KEY_SUMMON_FIREBALL, Component.translatable("dgmodules.module.cataclysm_arrow.summon_fireball"), value);
    }

    private BooleanProperty getOrCreateEnabled() {
        return enabled.orElseGet(() -> {
            BooleanProperty property = createEnabled(true);
            property.setChangeListener(() -> {});
            enabled = Optional.of(property);
            return property;
        });
    }

    private BooleanProperty getOrCreateHighFrequencyPierce() {
        return highFrequencyPierce.orElseGet(() -> {
            BooleanProperty property = createHighFrequencyPierce(true);
            property.setChangeListener(() -> {});
            highFrequencyPierce = Optional.of(property);
            return property;
        });
    }

    private BooleanProperty getOrCreateSummonFireball() {
        return summonFireball.orElseGet(() -> {
            BooleanProperty property = createSummonFireball(true);
            property.setChangeListener(() -> {});
            summonFireball = Optional.of(property);
            return property;
        });
    }

    public boolean isEnabled() {
        return getOrCreateEnabled().getValue();
    }

    public boolean isHighFrequencyPierceEnabled() {
        return getOrCreateHighFrequencyPierce().getValue();
    }

    public boolean shouldSummonFireball() {
        return getOrCreateSummonFireball().getValue();
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
        nbt.putBoolean(KEY_HIGH_FREQUENCY_PIERCE, getOrCreateHighFrequencyPierce().getValue());
        nbt.putBoolean(KEY_SUMMON_FIREBALL, getOrCreateSummonFireball().getValue());
        return nbt;
    }

    @Override
    protected void readExtraData(CompoundTag nbt) {
        if (nbt.contains(KEY_ENABLED)) {
            getOrCreateEnabled().setValue(nbt.getBoolean(KEY_ENABLED));
        }
        if (nbt.contains(KEY_HIGH_FREQUENCY_PIERCE)) {
            getOrCreateHighFrequencyPierce().setValue(nbt.getBoolean(KEY_HIGH_FREQUENCY_PIERCE));
        }
        if (nbt.contains(KEY_SUMMON_FIREBALL)) {
            getOrCreateSummonFireball().setValue(nbt.getBoolean(KEY_SUMMON_FIREBALL));
        }
    }

    @Override
    public void tick(ModuleContext ctx) {
        // Event-driven.
    }
}
