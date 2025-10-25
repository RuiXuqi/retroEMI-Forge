package dev.emi.emi.api.widget;

import com.google.common.collect.Lists;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.input.EmiInput;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.widget.RecipeButtonWidget;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public class RecipeFillButtonWidget extends RecipeButtonWidget {
    private boolean canFill;
    private List<TooltipComponent> tooltip;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ApiStatus.Internal
    public RecipeFillButtonWidget(int x, int y, EmiRecipe recipe) {
        super(x, y, 24, 0, recipe);
        GuiContainer hs = EmiApi.getHandledScreen();
        EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, hs);
        tooltip = com.rewindmc.retroemi.shim.java.List.of(TooltipComponent.of(EmiPort.ordered(EmiPort.translatable("emi.inapplicable"))));
        if (handler == null) {
            canFill = false;
        } else {
            try {
                EmiPlayerInventory inv = handler.getInventory(hs);
                boolean applicable = handler.supportsRecipe(recipe);
                EmiCraftContext context = new EmiCraftContext<>(hs, inv, EmiCraftContext.Type.FILL_BUTTON);
                canFill = applicable && handler.canCraft(recipe, context);
                if (applicable) {
                    tooltip = handler.getTooltip(recipe, context);
                }
            } catch (Exception e) {
                canFill = false;
                tooltip = com.rewindmc.retroemi.shim.java.List.of(TooltipComponent.of(EmiPort.ordered(EmiPort.translatable("emi.error.recipe.initialize"))));
            }
        }
    }

    @Override
    public int getTextureOffset(int mouseX, int mouseY) {
        if (!canFill) {
            return 24;
        }
        return super.getTextureOffset(mouseX, mouseY);
    }

    @Override
    public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
        List<TooltipComponent> list = Lists.newArrayList();
        if (canFill) {
            list.add(TooltipComponent.of(EmiPort.ordered(EmiPort.translatable("tooltip.emi.fill_recipe"))));
        }
        list.addAll(tooltip);
        return list;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (canFill) {
            GuiContainer hs = EmiApi.getHandledScreen();
            if (hs != null && EmiRecipeFiller.performFill(recipe, hs, EmiCraftContext.Type.FILL_BUTTON, EmiCraftContext.Destination.NONE, EmiInput.isShiftDown() ? Integer.MAX_VALUE : 1)) {
                this.playButtonSound();
                return true;
            }
        }
        return false;
    }
}
