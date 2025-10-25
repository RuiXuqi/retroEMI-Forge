package dev.emi.emi.api.recipe;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface EmiRecipe {

    /**
     * @return The recipe category this recipe should be displayed under.
     * This is used for grouping in the recipe screen, as well as category display in the recipe tree.
     */
    EmiRecipeCategory getCategory();

    /**
     * IDs should be standard formatting (minecraft:lime_dye_from_smelting) only if they uniquely represent a data driven json recipe.
     * If a mod wants to represent a vanilla recipe with different processing, it cannot reuse the vanilla ID.
     * For example, a custom machine's recipe crafting an iron pickaxe cannot use "minecraft:iron_pickaxe".
     * If a recipe does not have a normal unique ID, it should use a synthetic ID.
     * Synthetic IDs are formatted "namespace:/path" with a "/" at the start of the path.
     * Commonly, synthetic IDs will be formatted "mymod:/my_process/unique_name".
     *
     * @return The unique ID of the recipe, or null. If null, the recipe cannot be serialized.
     */
    @Nullable ResourceLocation getId();

    /**
     * @return A list of ingredients required for the recipe.
     * Inputs will consider this recipe a use when exploring recipes.
     */
    List<EmiIngredient> getInputs();

    /**
     * @return A list of ingredients associated with the creation of the recipe.
     * Catalysts are considered the same as workstations in the recipe, not broken down as a requirement.
     * However, catalysts will consider this recipe a use when exploring recipes.
     */
    default List<EmiIngredient> getCatalysts() {
        return Collections.emptyList();
    }

    /**
     * @return A list of stacks that are created after a craft.
     * Outputs will consider this recipe a source when exploring recipes.
     */
    List<EmiStack> getOutputs();

    /**
     * @return The width taken up by the recipe's widgets
     * EMI will grow to accomodate requested width.
     * To fit within the default width, recipes should request a width of 134.
     * If a recipe does not support the recipe tree or recipe filling, EMI
     * will not need to add buttons, and it will have space for a width of 160.
     */
    int getDisplayWidth();

    /**
     * @return The maximum height taken up by the recipe's widgets.
     * Vertical screen space is capped, however, and EMI may opt to provide less vertical space.
     * @see {@link WidgetHolder#getHeight()} when adding widgets for the EMI adjusted height.
     */
    int getDisplayHeight();

    /**
     * Called to add widgets that display the recipe.
     * Can be used in several places, including the main recipe screen, and tooltips.
     * It is worth noting that EMI cannot grow vertically, so recipes with large heights
     * may be provided less space than requested if they span more than the entire vertical
     * space available in the recipe scren.
     * In the case of very large heights, recipes should respect {@link WidgetHolder#getHeight()}.
     */
    void addWidgets(WidgetHolder widgets);

    /**
     * @return Whether the recipe supports the recipe tree.
     * Recipes that do not represent a set of inputs producing a set of outputs should exclude themselves.
     * Example for unsupportable recipes are pattern based recipes, like arbitrary dying.
     */
    default boolean supportsRecipeTree() {
        return !getInputs().isEmpty() && !getOutputs().isEmpty();
    }

    /**
     * @return Whether the recipe should be hidden from the craftable menu.
     * This is desirable behavior for recipes that are reimplementations of vanilla recipes in other workstations.
     */
    default boolean hideCraftable() {
        return false;
    }

    /**
     * @return The vanilla {@link IRecipe} this recipe represents, if any.
     * By default, uses the result of {@link EmiRecipe#getId()} to look up in the RecipeManager.
     */
    default @Nullable IRecipe getBackingRecipe() {
        return EmiPort.getRecipe(getId());
    }
}
