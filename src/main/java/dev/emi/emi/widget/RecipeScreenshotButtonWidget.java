package dev.emi.emi.widget;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiScreenshotRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class RecipeScreenshotButtonWidget extends RecipeButtonWidget {
    public RecipeScreenshotButtonWidget(int x, int y, EmiRecipe recipe) {
        super(x, y, 60, 0, recipe);
    }

    @Override
    public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
        return com.rewindmc.retroemi.shim.java.List.of(TooltipComponent.of(EmiPort.ordered(EmiPort.translatable("tooltip.emi.recipe_screenshot"))));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        this.playButtonSound();

        ResourceLocation id = recipe.getId();
        String path;
        if (id == null) {
            path = "unknown-recipe";
        } else {
            // Note that saveScreenshot treats `/`s as indicating subdirectories.
            // We don't want to keep `/` in paths because we want all recipe images in consistent directory locations.
            path = id.getNamespace() + "/" + id.getNamespace().replace("/", "_");
        }

        int width = recipe.getDisplayWidth() + 8;
        int height = recipe.getDisplayHeight() + 8;
        Minecraft client = Minecraft.getMinecraft();
        DrawContext context = DrawContext.INSTANCE;
        EmiScreenshotRecorder.saveScreenshot("emi/recipes/" + path, width, height,
                () -> EmiRenderHelper.renderRecipe(recipe, EmiDrawContext.wrap(context), 0, 0, false, -1));

        return true;
    }
}
