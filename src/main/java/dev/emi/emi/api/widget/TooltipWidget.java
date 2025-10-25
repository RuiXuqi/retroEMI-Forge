package dev.emi.emi.api.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

import java.util.List;
import java.util.function.BiFunction;

public class TooltipWidget extends Widget {
    private final Bounds bounds;
    private final BiFunction<Integer, Integer, List<TooltipComponent>> tooltipSupplier;

    public TooltipWidget(BiFunction<Integer, Integer, List<TooltipComponent>> tooltipSupplier, int x, int y, int width, int height) {
        this.bounds = new Bounds(x, y, width, height);
        this.tooltipSupplier = tooltipSupplier;
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
        return tooltipSupplier.apply(mouseX, mouseY);
    }

    @Override
    public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
    }
}
