package com.rewindmc.retroemi;

import com.rewindmc.retroemi.integ.MiscPlugin;
import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.platform.EmiAgnos;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.stream.Stream;

public class REMIPlugin implements EmiMultiPlugin, Runnable {

    private final Map<String, Class<? extends EmiPlugin>> children = com.rewindmc.retroemi.shim.java.Map.of(
            null, VanillaPlugin.class,
            "", MiscPlugin.class
//            "Forestry", ForestryPlugin.class,
//            "IC2", IC2Plugin.class,
//            "AdvancedMachines", AdvancedMachinesPlugin.class,
//            "Railcraft", RailcraftPlugin.class
    );

    @Override
    public void run() {
    }

    @Override
    public Stream<EmiPlugin> getChildPlugins() {
        return children.entrySet().stream()
                .filter(en -> en.getKey() == null || en.getKey().isEmpty() || EmiAgnos.isModLoaded(en.getKey()))
                .map(en -> {
                    try {
                        return (EmiPlugin) en.getValue().getConstructor().newInstance();
                    } catch (Throwable t) {
                        throw new AssertionError(t);
                    }
                });
    }

    public static EmiRecipeCategory category(String id, ItemStack icon) {
        return new EmiRecipeCategory(new ResourceLocation(id), EmiStack.of(icon),
                new EmiTexture(new ResourceLocation("emi", "textures/simple_icons/" + id + ".png"), 0, 0, 16, 16, 16, 16, 16, 16));
    }

/*    public static <T extends IRecipe> Iterable<T> recipesOfType(Class<T> clazz) {
        return CraftingManager.getInstance().getRecipeList().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)::iterator;
    }*/

}

