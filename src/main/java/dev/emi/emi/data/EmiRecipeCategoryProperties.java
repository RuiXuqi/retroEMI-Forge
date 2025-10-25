package dev.emi.emi.data;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;

import java.util.Comparator;
import java.util.function.Supplier;

public class EmiRecipeCategoryProperties {
    public int order;
    public Supplier<EmiRenderable> icon, simplified;
    public Comparator<EmiRecipe> sort;

    public static int getOrder(EmiRecipeCategory category) {
        EmiRecipeCategoryProperties props = EmiData.categoryPriorities.get(category.getId().toString());
        if (props != null) {
            return props.order;
        }
        return 0;
    }

    public static Comparator<EmiRecipe> getSort(EmiRecipeCategory category) {
        EmiRecipeCategoryProperties props = EmiData.categoryPriorities.get(category.getId().toString());
        if (props != null && props.sort != null) {
            return props.sort;
        }
        return category.getSort();
    }

    public static EmiRenderable getIcon(EmiRecipeCategory category) {
        EmiRecipeCategoryProperties props = EmiData.categoryPriorities.get(category.getId().toString());
        if (props != null && props.icon != null) {
            return props.icon.get();
        }
        return category.icon;
    }

    public static EmiRenderable getSimplifiedIcon(EmiRecipeCategory category) {
        EmiRecipeCategoryProperties props = EmiData.categoryPriorities.get(category.getId().toString());
        if (props != null && props.simplified != null) {
            return props.simplified.get();
        }
        return category.simplified;
    }
}
