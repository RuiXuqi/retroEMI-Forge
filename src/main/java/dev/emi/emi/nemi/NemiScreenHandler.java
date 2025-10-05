package dev.emi.emi.nemi;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiAdapter;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.client.gui.inventory.GuiContainer;

public class NemiScreenHandler extends INEIGuiAdapter {
    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {

        if (EmiConfig.enabled)
            currentVisibility.showItemSection =
                currentVisibility.enableDeleteMode =
                    currentVisibility.showSearchSection = false;

        return currentVisibility;
    }
}
