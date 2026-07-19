package com.likeazusa2.dgmodules.item;

import com.likeazusa2.dgmodules.logic.ChaosCrystalBreakerLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class ChaosCrystalBreakerItem extends Item {

    public ChaosCrystalBreakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var target = ChaosCrystalBreakerLogic.findTarget(level, player, context.getClickedPos());

        if (target == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && !ChaosCrystalBreakerLogic.installAndConsume(
                level, player, context.getItemInHand(), target, context.getClickedPos())) {
            return InteractionResult.PASS;
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Fallback for clients that do not report the guardian crystal entity as
     * the direct hit target.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        var target = ChaosCrystalBreakerLogic.findTarget(level, player, null);
        if (target == null) {
            // Still let the server receive the use packet. The server has the
            // authoritative entity list and can resolve the target itself.
            return level.isClientSide()
                    ? InteractionResultHolder.sidedSuccess(stack, true)
                    : InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide()
                && !ChaosCrystalBreakerLogic.installAndConsume(level, player, stack, target, null)) {
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dgmodules.chaos_crystal_breaker.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.dgmodules.chaos_crystal_breaker.countdown",
                        Math.max(1, (ChaosCrystalBreakerLogic.configuredCountdownTicks() + 19) / 20)
                )
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.dgmodules.chaos_crystal_breaker.radius",
                        String.format(java.util.Locale.ROOT, "%.1f", ChaosCrystalBreakerLogic.configuredBlastRadius())
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.dgmodules.chaos_crystal_breaker.use")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
