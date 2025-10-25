package dev.emi.emi.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SizedButtonWidget extends ButtonWidget {
    private final BooleanSupplier isActive;
    private final IntSupplier vOffset;
    protected ResourceLocation texture = EmiRenderHelper.BUTTONS;
    protected Supplier<List<Text>> text;
    protected int u, v;

    public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, PressAction action) {
        this(x, y, width, height, u, v, isActive, action, () -> 0);
    }

    public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, PressAction action,
                             List<Text> text) {
        this(x, y, width, height, u, v, isActive, action, () -> 0, () -> text);
    }

    public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, PressAction action,
                             IntSupplier vOffset) {
        this(x, y, width, height, u, v, isActive, action, vOffset, null);
    }

    public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, PressAction action,
                             IntSupplier vOffset, Supplier<List<Text>> text) {
        super(x, y, width, height, EmiPort.literal(""), action, s -> s.get());
        this.u = u;
        this.v = v;
        this.isActive = isActive;
        this.vOffset = vOffset;
        this.text = text;
    }

    protected int getU(int mouseX, int mouseY) {
        return this.u;
    }

    protected int getV(int mouseX, int mouseY) {
        int v = this.v + vOffset.getAsInt();
        this.active = this.isActive.getAsBoolean();
        if (!this.active) {
            v += this.height * 2;
        } else if (this.isMouseOver(mouseX, mouseY)) {
            v += this.height;
        }
        return v;
    }

    @Override
    public void renderWidget(DrawContext raw, int mouseX, int mouseY, float delta) {
        EmiDrawContext context = EmiDrawContext.wrap(raw);
        if (!this.isMouseOver(mouseX, mouseY)) {
            context.resetColor();
        }
        RenderSystem.enableDepthTest();
        context.drawTexture(texture, this.x, this.y, getU(mouseX, mouseY), getV(mouseX, mouseY), this.width, this.height);
        if (this.isMouseOver(mouseX, mouseY) && text != null && this.active) {
            context.push();
            RenderSystem.disableDepthTest();
            Minecraft client = Minecraft.getMinecraft();
            EmiRenderHelper.drawTooltip(client.currentScreen, context, text.get().stream().map(EmiPort::ordered).map(TooltipComponent::of).collect(Collectors.toList()), mouseX, mouseY);
            context.pop();
        }
    }
}
