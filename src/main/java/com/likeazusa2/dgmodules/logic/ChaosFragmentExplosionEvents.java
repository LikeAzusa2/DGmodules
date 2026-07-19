package com.likeazusa2.dgmodules.logic;

import com.brandon3055.draconicevolution.init.DEContent;
import com.likeazusa2.dgmodules.DGModules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Converts one thrown set of high-tier ingredients when all three item
 * entities are caught by the same explosion.
 */
@EventBusSubscriber(modid = DGModules.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ChaosFragmentExplosionEvents {

    private ChaosFragmentExplosionEvents() {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        ItemEntity dragonHeart = findIngredient(event, DEContent.DRAGON_HEART.get());
        ItemEntity netherStar = findIngredient(event, Items.NETHER_STAR);
        ItemEntity diamond = findIngredient(event, Items.DIAMOND);
        if (dragonHeart == null || netherStar == null || diamond == null) return;

        Vec3 outputPos = averagePosition(dragonHeart, netherStar, diamond);
        consumeOne(dragonHeart);
        consumeOne(netherStar);
        consumeOne(diamond);

        // Keep the consumed entities from receiving a second explosion pass.
        event.getAffectedEntities().remove(dragonHeart);
        event.getAffectedEntities().remove(netherStar);
        event.getAffectedEntities().remove(diamond);

        ItemEntity result = new ItemEntity(
                level,
                outputPos.x,
                outputPos.y,
                outputPos.z,
                new ItemStack(DEContent.CHAOS_FRAG_SMALL.get(), 2)
        );
        result.setDefaultPickUpDelay();
        result.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(result);
    }

    private static ItemEntity findIngredient(ExplosionEvent.Detonate event, Item item) {
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof ItemEntity itemEntity
                    && itemEntity.isAlive()
                    && itemEntity.getItem().is(item)) {
                return itemEntity;
            }
        }
        return null;
    }

    private static void consumeOne(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        }
    }

    private static Vec3 averagePosition(ItemEntity first, ItemEntity second, ItemEntity third) {
        return first.position()
                .add(second.position())
                .add(third.position())
                .scale(1.0D / 3.0D);
    }
}
