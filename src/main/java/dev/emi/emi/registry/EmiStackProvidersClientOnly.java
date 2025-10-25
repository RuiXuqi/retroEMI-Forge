package dev.emi.emi.registry;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.mixin.early.accessor.SlotCraftingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

@SideOnly(Side.CLIENT)
public class EmiStackProvidersClientOnly {
    @Nullable
    public static EmiStackInteraction getEmiStackInteraction(Slot s, ItemStack stack) {
        if (s instanceof SlotCrafting craf) {
            // Emi be making assumptions
            try {
                InventoryCrafting inv = ((SlotCraftingAccessor) craf).getCraftMatrix();
                Minecraft client = Minecraft.getMinecraft();
                for (var r : EmiPort.getRecipeRegistry().getValuesCollection()) {
                    if (r.matches(inv, client.world)) {
                        ResourceLocation id = EmiPort.getId(r);
                        EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
                        if (recipe != null) {
                            return new EmiStackInteraction(EmiStack.of(stack), recipe, false);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
