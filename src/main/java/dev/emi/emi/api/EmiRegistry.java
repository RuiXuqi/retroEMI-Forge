package dev.emi.emi.api;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeDecorator;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.text.Text;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface EmiRegistry {

    /**
     * @return Whether the provided stack is disabled.
     * Generally, this is not important to check before registering recipes, EMI will handle recipe hiding.
     * There are certain cases where checking this and not including stacks in recipes is desired behavior, however.
     */
    @ApiStatus.Experimental
    boolean isStackDisabled(EmiIngredient stack);

    /**
     * Adds a recipe category.
     * Recipes are organized based on recipe category.
     */
    void addCategory(EmiRecipeCategory category);

    /**
     * Adds a workstation to a recipe category.
     */
    void addWorkstation(EmiRecipeCategory category, EmiIngredient workstation);

    /**
     * Adds a recipe to EMI that can be viewed and associated with its components.
     */
    void addRecipe(EmiRecipe recipe);

    /**
     * Adds a predicate to run on all current and future recipes to prevent certain ones from being added.
     */
    void removeRecipes(Predicate<EmiRecipe> predicate);

    /**
     * Adds a predicate to run on all current and future recipes to prevent certain ones with the given identifier from being added.
     */
    default void removeRecipes(ResourceLocation id) {
        removeRecipes(r -> id.equals(r.getId()));
    }

    /**
     * Add recipes that are reliant on a majority of EMI metadata is populated.
     * The passed consumer will be run after all EMI plugins have executed.
     */
    void addDeferredRecipes(Consumer<Consumer<EmiRecipe>> consumer);

    /**
     * Adds an EmiStack to the sidebar.
     */
    void addEmiStack(EmiStack stack);

    /**
     * Adds an EmiStack to the sidebar immediately following another.
     * If the predicate never succeeds, the provided EmiStack will not be added.
     */
    void addEmiStackAfter(EmiStack stack, Predicate<EmiStack> predicate);

    /**
     * Adds an EmiStack to the sidebar immediately following another.
     * If the predicate never succeeds, the provided EmiStack will not be added.
     */
    default void addEmiStackAfter(EmiStack stack, EmiStack other) {
        addEmiStackAfter(stack, s -> s.equals(other));
    }

    /**
     * Adds a predicate to run on all current and future EmiStacks to prevent certain ones from being added to the sidebar.
     */
    void removeEmiStacks(Predicate<EmiStack> predicate);

    /**
     * Adds a predicate to run on all current and future EmiStacks to prevent matching ones from being added to the sidebar.
     */
    default void removeEmiStacks(EmiStack stack) {
        removeEmiStacks(s -> s.equals(stack));
    }

    /**
     * @deprecated Use {@link EmiPlugin#initialize(EmiInitRegistry)} and {@link EmiInitRegistry#addIngredientSerializer(Class, EmiIngredientSerializer)}
     */
    @Deprecated
    <T extends EmiIngredient> void addIngredientSerializer(Class<T> clazz, EmiIngredientSerializer<T> serializer);

    /**
     * Adds an EmiExclusionArea to screens of a given class.
     * Exclusion areas can provide rectangles where EMI will not place EmiStacks.
     */
    <T extends GuiScreen> void addExclusionArea(Class<T> clazz, EmiExclusionArea<T> area);

    /**
     * Adds an EmiExclusionArea to every screen.
     * Exclusion areas can provide rectangles where EMI will not place EmiStacks.
     */
    void addGenericExclusionArea(EmiExclusionArea<GuiScreen> area);

    /**
     * Adds an EmiDragDropHandler to screens of a given class.
     * Drag drop handlers can consume events related to sidebar stacks being dragged and dropped.
     */
    <T extends GuiScreen> void addDragDropHandler(Class<T> clazz, EmiDragDropHandler<T> handler);

    /**
     * Adds an EmiDragDropHandler to every screen.
     * Drag drop handlers can consume events related to sidebar stacks being dragged and dropped.
     */
    void addGenericDragDropHandler(EmiDragDropHandler<GuiScreen> handler);

    /**
     * Adds an EmiStackProvider to screens of a given class.
     * Stack providers can inform EMI of EmiIngredients that are located on the screen.
     */
    <T extends GuiScreen> void addStackProvider(Class<T> clazz, EmiStackProvider<T> provider);

    /**
     * Adds an EmiStackProvider to every screen.
     * Stack providers can inform EMI of EmiIngredients that are located on the screen.
     */
    void addGenericStackProvider(EmiStackProvider<GuiScreen> provider);

    /**
     * Adds a default compraison method for a stack key.
     *
     * @param key        A stack key such as an item or fluid.
     * @param comparison A function to mutate the current comprison method.
     */
    void setDefaultComparison(Object key, Function<Comparison, Comparison> comparison);

    /**
     * Adds a default compraison method for a stack using its key.
     *
     * @param key        A stack key such as an item or fluid.
     * @param comparison The desired comparison method.
     */
    default void setDefaultComparison(Object key, Comparison comparison) {
        setDefaultComparison(key, old -> comparison);
    }

    /**
     * Adds a default compraison method for a stack using its key.
     *
     * @param stack      A stack to derive a key from.
     * @param comparison A function to mutate the current comprison method.
     */
    default void setDefaultComparison(EmiStack stack, Function<Comparison, Comparison> comparison) {
        setDefaultComparison(stack.getKey(), comparison);
    }

    /**
     * Adds a default compraison method for a stack using its key.
     *
     * @param stack      A stack to derive a key from.
     * @param comparison The desired comparison method.
     */
    default void setDefaultComparison(EmiStack stack, Comparison comparison) {
        setDefaultComparison(stack.getKey(), old -> comparison);
    }

    /**
     * Adds a search alias for a given stack.
     * Aliases are treated the same as the stack's name when searching.
     * Aliases should be text the player would look up trying to find the given stack, but wouldn't match the stack's name.
     *
     * @param stack A stack that can be searched with the provided alias.
     * @param text  The alias for the given stack.
     */
    @ApiStatus.Experimental
    void addAlias(EmiIngredient stack, Text text);

    /**
     * Adds a recipe handler to a specified type of screen handler.
     * Recipe handlers are responsible for filling recipes automatically.
     */
    <T extends Container> void addRecipeHandler(Class<T> type, EmiRecipeHandler<T> handler);

    /**
     * Adds a recipe decorator for all recipe categories.
     * Recipe decorators can display additional widgets in recipes to indicate information from external mods.
     */
    @ApiStatus.Experimental
    void addRecipeDecorator(EmiRecipeDecorator decorator);

    /**
     * Adds a recipe decorator for a specific recipe category.
     * Recipe decorators can display additional widgets in recipes to indicate information from external mods.
     */
    @ApiStatus.Experimental
    default void addRecipeDecorator(EmiRecipeCategory category, EmiRecipeDecorator decorator) {
        addRecipeDecorator((recipe, widgets) -> {
            if (recipe.getCategory() == category) {
                decorator.decorateRecipe(recipe, widgets);
            }
        });
    }
}
