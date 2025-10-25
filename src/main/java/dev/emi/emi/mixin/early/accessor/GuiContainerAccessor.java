package dev.emi.emi.mixin.early.accessor;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {
    // TODO:True???
    @Accessor("hoveredSlot")
    Slot getTheSlot();
}
