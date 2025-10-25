package dev.emi.emi.stack.serializer;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;


public class ItemEmiStackSerializer implements EmiStackSerializer<ItemEmiStack> {

    @Override
    public String getType() {
        return "item";
    }

    @Override
    public EmiStack create(ResourceLocation id, NBTTagCompound nbt, long amount, int subtype) {
        Item item = EmiPort.getItemRegistry().getValue(id);
        if (item == null) return EmiStack.EMPTY;
        ItemStack stack = new ItemStack(item, 1, subtype);
        stack.setTagCompound(nbt);
        return EmiStack.of(stack, amount);
    }
}
