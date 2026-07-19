package com.likeazusa2.dgmodules.client.jei;

import com.brandon3055.draconicevolution.init.DEContent;
import com.likeazusa2.dgmodules.DGModules;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ChaosFragmentExplosionJeiCategory
        implements IRecipeCategory<ChaosFragmentExplosionJeiRecipe> {

    public static final RecipeType<ChaosFragmentExplosionJeiRecipe> TYPE = RecipeType.create(
            DGModules.MODID,
            "chaos_fragment_explosion",
            ChaosFragmentExplosionJeiRecipe.class
    );

    private final IDrawable background;
    private final IDrawable icon;

    public ChaosFragmentExplosionJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 72);
        this.icon = guiHelper.createDrawableItemStack(
                new ItemStack(DEContent.CHAOS_FRAG_SMALL.get())
        );
    }

    @Override
    public RecipeType<ChaosFragmentExplosionJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.dgmodules.chaos_fragment_explosion.title");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 72;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          ChaosFragmentExplosionJeiRecipe recipe,
                          IFocusGroup focuses) {
        builder.addInputSlot(8, 20)
                .addItemStack(new ItemStack(DEContent.DRAGON_HEART.get()));
        builder.addInputSlot(44, 20)
                .addItemStack(new ItemStack(Items.NETHER_STAR));
        builder.addInputSlot(80, 20)
                .addItemStack(new ItemStack(Items.DIAMOND));
        builder.addOutputSlot(132, 20)
                .addItemStack(new ItemStack(DEContent.CHAOS_FRAG_SMALL.get(), 2));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras,
                                   ChaosFragmentExplosionJeiRecipe recipe,
                                   IFocusGroup focuses) {
        extras.addRecipeArrow(106, 20);
        extras.addText(
                Component.translatable("jei.dgmodules.chaos_fragment_explosion.instruction"),
                8,
                53,
                144,
                16
        ).alignHorizontalCenter();
    }
}
