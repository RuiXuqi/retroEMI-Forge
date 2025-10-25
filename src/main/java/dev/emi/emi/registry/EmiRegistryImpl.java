package dev.emi.emi.registry;

import com.google.common.collect.Lists;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeDecorator;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.data.EmiAlias;
import dev.emi.emi.runtime.EmiHidden;
import dev.emi.emi.runtime.EmiReloadLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.text.Text;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class EmiRegistryImpl implements EmiRegistry {
    private static final Minecraft client = Minecraft.getMinecraft();

    @Override
    public boolean isStackDisabled(EmiIngredient stack) {
        return EmiHidden.isDisabled(stack);
    }

    @Override
    public void addCategory(EmiRecipeCategory category) {
        EmiRecipes.addCategory(category);
    }

    @Override
    public void addWorkstation(EmiRecipeCategory category, EmiIngredient workstation) {
        EmiRecipes.addWorkstation(category, workstation);
    }

    @Override
    public void addRecipe(EmiRecipe recipe) {
        if (recipe.getInputs() == null) {
            EmiReloadLog.warn("Recipe " + recipe.getId() + " provides null inputs and cannot be added");
        } else if (recipe.getOutputs() == null) {
            EmiReloadLog.warn("Recipe " + recipe.getId() + " provides null outputs and cannot be added");
        } else {
            EmiRecipes.addRecipe(recipe);
        }
    }

    @Override
    public void removeRecipes(Predicate<EmiRecipe> predicate) {
        EmiRecipes.invalidators.add(predicate);
    }

    @Override
    public void addDeferredRecipes(Consumer<Consumer<EmiRecipe>> consumer) {
        EmiRecipes.lateRecipes.add(consumer);
    }

    @Override
    public void addEmiStack(EmiStack stack) {
        EmiStackList.stacks.add(stack);
    }

    @Override
    public void addEmiStackAfter(EmiStack stack, Predicate<EmiStack> predicate) {
        ListIterator<EmiStack> listIterator = EmiStackList.stacks.listIterator();
        while (listIterator.hasNext()) {
            EmiStack candidate = listIterator.next();
            if (predicate.test(candidate)) {
                listIterator.add(stack);
                return;
            }
        }
    }

    @Override
    public void removeEmiStacks(Predicate<EmiStack> predicate) {
        EmiStackList.invalidators.add(predicate);
    }

    @Override
    public <T extends EmiIngredient> void addIngredientSerializer(Class<T> clazz, EmiIngredientSerializer<T> serializer) {
        EmiIngredientSerializers.BY_CLASS.put(clazz, serializer);
        EmiIngredientSerializers.BY_TYPE.put(serializer.getType(), serializer);
    }

    @Override
    public <T extends GuiScreen> void addExclusionArea(Class<T> clazz, EmiExclusionArea<T> area) {
        EmiExclusionAreas.fromClass.computeIfAbsent(clazz, c -> Lists.newArrayList()).add(area);
    }

    @Override
    public void addGenericExclusionArea(EmiExclusionArea<GuiScreen> area) {
        EmiExclusionAreas.generic.add(area);
    }

    @Override
    public <T extends GuiScreen> void addDragDropHandler(Class<T> clazz, EmiDragDropHandler<T> handler) {
        EmiDragDropHandlers.fromClass.computeIfAbsent(clazz, c -> Lists.newArrayList()).add(handler);
    }

    @Override
    public void addGenericDragDropHandler(EmiDragDropHandler<GuiScreen> handler) {
        EmiDragDropHandlers.generic.add(handler);
    }

    @Override
    public <T extends GuiScreen> void addStackProvider(Class<T> clazz, EmiStackProvider<T> provider) {
        EmiStackProviders.fromClass.computeIfAbsent(clazz, c -> Lists.newArrayList()).add(provider);
    }

    @Override
    public void addGenericStackProvider(EmiStackProvider<GuiScreen> provider) {
        EmiStackProviders.generic.add(provider);
    }

    @Override
    public <T extends Container> void addRecipeHandler(Class<T> type, dev.emi.emi.api.recipe.handler.EmiRecipeHandler<T> handler) {
        EmiRecipeFiller.handlers.computeIfAbsent(type, (c) -> Lists.newArrayList()).add(handler);
    }

    @Override
    public void setDefaultComparison(Object key, Function<Comparison, Comparison> comparison) {
        EmiComparisonDefaults.comparisons.put(key, comparison.apply(EmiComparisonDefaults.get(key)));
    }

    @Override
    public void addAlias(EmiIngredient stack, Text text) {
        EmiStackList.registryAliases.add(new EmiAlias.Baked(com.rewindmc.retroemi.shim.java.List.of(stack), com.rewindmc.retroemi.shim.java.List.of(text)));
    }

    @Override
    public void addRecipeDecorator(EmiRecipeDecorator decorator) {
        EmiRecipes.decorators.add(decorator);
    }
}
