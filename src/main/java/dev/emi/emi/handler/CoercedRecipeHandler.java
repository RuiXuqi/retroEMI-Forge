package dev.emi.emi.handler;

import com.google.common.collect.Lists;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.mixin.early.accessor.SlotCraftingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

import java.util.List;

public class CoercedRecipeHandler<T extends Container> implements StandardRecipeHandler<T> {
    private SlotCrafting output;
    private InventoryCrafting inv;

    public CoercedRecipeHandler(SlotCrafting output) {
        this.output = output;
        this.inv = ((SlotCraftingAccessor) output).getCraftMatrix();
    }

    @Override
    public Slot getOutputSlot(Container handler) {
        return output;
    }

    @Override
    public List<Slot> getInputSources(Container handler) {
        Minecraft client = Minecraft.getMinecraft();
        List<Slot> slots = Lists.newArrayList();
        if (output != null) {
            for (Slot slot : (List<Slot>) handler.inventorySlots) {
                if (slot.canTakeStack(client.player) && slot != output) {
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(Container handler) {
        List<Slot> slots = Lists.newArrayList();
        int width = inv.getWidth();
        int height = inv.getSizeInventory() / width;
        for (int i = 0; i < 9; i++) {
            slots.add(null);
        }
        for (Slot slot : handler.inventorySlots) { //Something about this is broken, not sure what
            if (slot.inventory == inv && slot.getSlotIndex() < width * height && slot.getSlotIndex() >= 0) {
                int index = slot.getSlotIndex();
                index = index * 3 / width;
                slots.set(index, slot);
            }
        }
        return slots;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree()) {
            if (recipe instanceof EmiCraftingRecipe crafting) {
                int width = inv.getWidth();
                int height = inv.getSizeInventory() / width;
                return crafting.canFit(width, height);
            }
            return true;
        }
        return false;
    }
}
