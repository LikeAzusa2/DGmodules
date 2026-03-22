package com.likeazusa2.dgmodules.modules;

import com.brandon3055.draconicevolution.api.config.BooleanProperty;
import com.brandon3055.draconicevolution.api.config.ConfigProperty;
import com.brandon3055.draconicevolution.api.config.DecimalProperty;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.data.NoData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

import static com.brandon3055.draconicevolution.api.config.ConfigProperty.DecimalFormatter.PERCENT_0;

public class FlightTunerModuleEntity extends ModuleEntity<NoData> {
    private static final String KEY_SPEED_MUL = "speed_mul";
    private static final String KEY_NO_INERTIA = "no_inertia";

    private Optional<DecimalProperty> speedMul = Optional.empty();
    private Optional<BooleanProperty> noInertia = Optional.empty();

    public FlightTunerModuleEntity(Module<NoData> module) {
        super(module);
        speedMul = Optional.of((DecimalProperty) addProperty(copySpeedProperty(1.0)));
        noInertia = Optional.of((BooleanProperty) addProperty(copyNoInertiaProperty(false)));
        savePropertiesToItem = true;
    }

    public FlightTunerModuleEntity(Module<NoData> module, int gridX, int gridY) {
        this(module);
        setPos(gridX, gridY);
    }

    public ModuleEntity<?> copy() {
        FlightTunerModuleEntity entity = new FlightTunerModuleEntity(getModule(), getGridX(), getGridY());
        entity.getOrCreateSpeedMul().setValue(getOrCreateSpeedMul().getValue());
        entity.getOrCreateNoInertia().setValue(getOrCreateNoInertia().getValue());
        return entity;
    }

    private void attachListeners() {
        speedMul.ifPresent(property -> property.setChangeListener(() -> {}));
        noInertia.ifPresent(property -> property.setChangeListener(() -> {}));
    }

    public void getEntityProperties(List<ConfigProperty> properties) {
        properties.add(getOrCreateSpeedMul());
        properties.add(getOrCreateNoInertia());
    }

    private DecimalProperty getOrCreateSpeedMul() {
        return speedMul.orElseGet(() -> {
            DecimalProperty property = copySpeedProperty(1.0);
            property.setChangeListener(() -> {});
            speedMul = Optional.of(property);
            return property;
        });
    }

    private BooleanProperty getOrCreateNoInertia() {
        return noInertia.orElseGet(() -> {
            BooleanProperty property = copyNoInertiaProperty(false);
            property.setChangeListener(() -> {});
            noInertia = Optional.of(property);
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
        nbt.putDouble(KEY_SPEED_MUL, getOrCreateSpeedMul().getValue());
        nbt.putBoolean(KEY_NO_INERTIA, getOrCreateNoInertia().getValue());
        return nbt;
    }

    @Override
    protected void readExtraData(CompoundTag nbt) {
        if (nbt.contains(KEY_SPEED_MUL)) {
            getOrCreateSpeedMul().setValue(nbt.getDouble(KEY_SPEED_MUL));
        }
        if (nbt.contains(KEY_NO_INERTIA)) {
            getOrCreateNoInertia().setValue(nbt.getBoolean(KEY_NO_INERTIA));
        }
    }

    private DecimalProperty copySpeedProperty(double value) {
        return new DecimalProperty(
                KEY_SPEED_MUL,
                Component.translatable("dgmodules.module.flight_tuner.speed"),
                value
        ).range(1.0, 8.0).setFormatter(PERCENT_0);
    }

    private BooleanProperty copyNoInertiaProperty(boolean value) {
        return new BooleanProperty(
                KEY_NO_INERTIA,
                Component.translatable("dgmodules.module.flight_tuner.no_inertia"),
                value
        );
    }

    public double getSpeedMul() {
        return getOrCreateSpeedMul().getValue();
    }

    public boolean isNoInertia() {
        return getOrCreateNoInertia().getValue();
    }
}
