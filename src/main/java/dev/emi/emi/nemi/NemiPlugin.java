package dev.emi.emi.nemi;

import codechicken.nei.*;
import codechicken.nei.api.API;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@EmiEntrypoint
public class NemiPlugin implements EmiPlugin {
    private static final Minecraft client = Minecraft.getMinecraft();
    private static final Map<ItemStack, EmiStack> stackCache = new HashMap<>();

    public static void onLoad() {
        API.registerNEIGuiHandler(new NemiScreenHandler());
    }

    @SideOnly(Side.CLIENT)
    public static void cycleNemi() {
        EmiConfig.enabled = !EmiConfig.enabled;
        if (client.currentScreen instanceof RecipeScreen rs) {
            rs.close();
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, consumer) -> {
            final LayoutStyleMinecraft layout = (LayoutStyleMinecraft) LayoutManager.getLayoutStyle();
            final int rows = (int) Math.ceil((double) layout.buttonCount / layout.numButtons);
            final int diff = rows * 19 + 2;
            consumer.accept(new Bounds(0, 0, layout.numButtons * 19, diff));
        });
    }

    public static EmiStack stackEmi2Nei(ItemStack neiStack) {
        return stackCache.computeIfAbsent(neiStack, stack ->
            EmiStack.of(stack.getItem(), stack.getItemDamage())
        );
    }

    public static ItemStack stackNei2Emi(EmiStack emiStack) {
        return emiStack.getItemStack().copy();
    }
}
