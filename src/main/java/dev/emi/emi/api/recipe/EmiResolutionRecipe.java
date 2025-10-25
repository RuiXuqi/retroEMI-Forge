package dev.emi.emi.api.recipe;

import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmiResolutionRecipe implements EmiRecipe {
    public final EmiIngredient ingredient;
    public final EmiStack stack;

    public EmiResolutionRecipe(EmiIngredient ingredient, EmiStack stack) {
        this.ingredient = ingredient;
        this.stack = stack;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaPlugin.RESOLUTION;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return null;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return com.rewindmc.retroemi.shim.java.List.of(stack);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return com.rewindmc.retroemi.shim.java.List.of(stack);
    }

    @Override
    public int getDisplayWidth() {
        return 68;
    }

    @Override
    public int getDisplayHeight() {
        return 18;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 22, 1);
        widgets.addSlot(stack, 0, 0);
        widgets.addSlot(ingredient, 50, 0);
    }
}
