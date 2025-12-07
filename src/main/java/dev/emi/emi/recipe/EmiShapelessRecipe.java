package dev.emi.emi.recipe;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.crafting.ShapelessRecipes;

import java.util.stream.Collectors;

public class EmiShapelessRecipe extends EmiCraftingRecipe {

    public EmiShapelessRecipe(ShapelessRecipes recipe) {
        super(recipe.getIngredients().stream().map(i -> EmiStack.of(i.getMatchingStacks()[0])).collect(Collectors.toList()),
                EmiStack.of(EmiPort.getOutput(recipe)), EmiPort.getId(recipe));
        EmiShapedRecipe.setRemainders(input, recipe);
    }

    @Override
    public boolean canFit(int width, int height) {
        return input.size() <= width * height;
    }
}
