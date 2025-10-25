package dev.emi.emi.api.stack;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.registry.EmiTags;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.registry.tag.TagKey;

import java.util.List;
import java.util.stream.Collectors;

public interface EmiIngredient extends EmiRenderable {
    public static final int RENDER_ICON = 1;
    public static final int RENDER_AMOUNT = 2;
    public static final int RENDER_INGREDIENT = 4;
    public static final int RENDER_REMAINDER = 8;

    /**
     * @return The {@link EmiStack}s represented by this ingredient.
     * List is never empty. For an empty ingredient, us {@link EmiStack#EMPTY}
     */
    List<EmiStack> getEmiStacks();

    default boolean isEmpty() {
        for (EmiStack stack : getEmiStacks()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    EmiIngredient copy();

    long getAmount();

    EmiIngredient setAmount(long amount);

    float getChance();

    EmiIngredient setChance(float chance);

    @Override
    default void render(DrawContext draw, int x, int y, float delta) {
        render(draw, x, y, delta, -1);
    }

    void render(DrawContext draw, int x, int y, float delta, int flags);

    List<TooltipComponent> getTooltip();

    public static boolean areEqual(EmiIngredient a, EmiIngredient b) {
        List<EmiStack> as = a.getEmiStacks();
        List<EmiStack> bs = b.getEmiStacks();
        if (as.size() != bs.size()) {
            return false;
        }
        for (int i = 0; i < as.size(); i++) {
            if (!as.get(i).isEqual(bs.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static <T> EmiIngredient of(TagKey<T> key) {
        return of(key, 1);
    }

    public static <T> EmiIngredient of(TagKey<T> key, long amount) {
        return EmiIngredient.of(EmiTags.getRawValues(key), amount);
    }

    public static EmiIngredient of(List<? extends EmiIngredient> list) {
        return of(list, 1);
    }

    public static EmiIngredient of(List<? extends EmiIngredient> list, long amount) {
        if (list.size() == 0) {
            return EmiStack.EMPTY;
        } else if ((list.size() == 1) && amount <= 64) {
            return list.get(0);
        } else if (list.size() == 1) {
            return new ListEmiIngredient(list, amount - 64); // Surely there's a better way to do this
        } else {
            long internalAmount = list.get(0).getAmount();
            for (EmiIngredient i : list) {
                if (i.getAmount() != internalAmount) {
                    internalAmount = 1;
                }
            }
            if (internalAmount > 1) {
                amount = internalAmount;
                list = list.stream().map(st -> st.copy().setAmount(1)).collect(Collectors.toList());
            }
            Class<?> tagType = null;
            for (EmiIngredient i : list) {
                for (EmiStack s : i.getEmiStacks()) {
                    if (!s.isEmpty()) {
//                        if (tagType == null) {
//                            tagType = EmiTags.ADAPTERS_BY_CLASS.getKey(s.getKey().getClass());
//                        }
                        if (tagType == null || !tagType.isAssignableFrom(s.getKey().getClass())) {
                            return new ListEmiIngredient(list, amount);
                        }
                    }
                }
            }
            return EmiTags.getIngredient(tagType, list.stream().flatMap(i -> i.getEmiStacks().stream()).collect(Collectors.toList()), amount);
        }
    }
}
