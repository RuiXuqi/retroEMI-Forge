package dev.emi.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.data.EmiRecipeCategoryProperties;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class EmiUtil {
    public static final Random RANDOM = new Random();

    public static String subId(ResourceLocation id) {
        return id.getNamespace() + "/" + id.getPath();
    }

    public static String subId(Block block) {
        return subId(EmiPort.id(block.getRegistryName().toString()));
    }

    public static String subId(Item item) {
        return subId(EmiPort.id(item.getRegistryName().toString()));
    }

    public static String subId(Fluid fluid) {
        return subId(EmiPort.id(fluid.getName()));
    }

    public static String subId(ItemStack stack) {
        return subId(EmiPort.id(stack.getDisplayName()));
    }

//	public static <T> Stream<RegistryEntry<T>> values(TagKey<T> key) {
//		MinecraftClient client = MinecraftClient.getInstance();
//		Registry<T> registry = client.world.getRegistryManager().get(key.registry());
//		Optional<Named<T>> opt = registry.getEntryList(key);
//		if (opt.isEmpty()) {
//			return Stream.of();
//		} else {
//			if (registry == EmiPort.getFluidRegistry()) {
//				return opt.get().stream().filter(o -> {
//					Fluid f = (Fluid) o.value();
//					return f.isStill(f.getDefaultState());
//				});
//			}
//			return opt.get().stream();
//		}
//	}

    public static boolean showAdvancedTooltips() {
        Minecraft client = Minecraft.getMinecraft();
        return client.gameSettings.advancedItemTooltips;
    }

    public static String translateId(String prefix, ResourceLocation id) {
        return prefix + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static String getModName(String namespace) {
        return EmiAgnos.getModName(namespace);
    }

    public static List<String> getStackTrace(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer, true));
        return Arrays.asList(writer.getBuffer().toString().split("\n"));
    }

    public static InventoryCrafting getCraftingInventory() {
        return new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }

            @Override
            public void detectAndSendChanges() {
            }
        }, 3, 3);
    }

    public static InventoryCrafting getSoulforgeInventory() {
        return new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }


            @Override
            public void detectAndSendChanges() {
            }
        }, 4, 4);
    }

    public static int getOutputCount(EmiRecipe recipe, EmiIngredient stack) {
        int count = 0;
        for (EmiStack o : recipe.getOutputs()) {
            if (stack.getEmiStacks().contains(o)) {
                count += o.getAmount();
            }
        }
        return count;
    }

    public static EmiRecipe getPreferredRecipe(EmiIngredient ingredient, EmiPlayerInventory inventory, boolean requireCraftable) {
        if (ingredient.getEmiStacks().size() == 1 && !ingredient.isEmpty()) {
            EmiStack stack = ingredient.getEmiStacks().get(0);
            return getPreferredRecipe(EmiApi.getRecipeManager().getRecipesByOutput(stack), inventory, requireCraftable);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static EmiRecipe getPreferredRecipe(List<EmiRecipe> recipes, EmiPlayerInventory inventory, boolean requireCraftable) {
        EmiRecipe preferred = null;
        int preferredWeight = -1;
        GuiContainer hs = EmiApi.getHandledScreen();
        EmiCraftContext context = new EmiCraftContext<>(hs, inventory, EmiCraftContext.Type.CRAFTABLE);
        for (EmiRecipe recipe : recipes) {
            int weight = 0;
            EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, hs);
            if (handler != null && handler.canCraft(recipe, context)) {
                weight += 16;
            } else if (requireCraftable) {
                continue;
            } else if (inventory.canCraft(recipe)) {
                weight += 8;
            }
            if (BoM.isRecipeEnabled(recipe)) {
                weight += 4;
            }
            if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
                weight += 2;
            }
            if (weight > preferredWeight) {
                preferredWeight = weight;
                preferred = recipe;
            } else if (weight == preferredWeight) {
                if (EmiRecipeCategoryProperties.getOrder(recipe.getCategory()) < EmiRecipeCategoryProperties.getOrder(preferred.getCategory())) {
                    preferredWeight = weight;
                    preferred = recipe;
                }
            }
        }
        return preferred;
    }

    public static EmiRecipe getRecipeResolution(EmiIngredient ingredient, EmiPlayerInventory inventory) {
        if (ingredient.getEmiStacks().size() == 1 && !ingredient.isEmpty()) {
            EmiStack stack = ingredient.getEmiStacks().get(0);
            return getPreferredRecipe(EmiApi.getRecipeManager().getRecipesByOutput(stack).stream().filter(r -> {
                return r.supportsRecipeTree() && r.getOutputs().stream().anyMatch(i -> i.isEqual(stack));
            }).collect(Collectors.toList()), inventory, false);
        }
        return null;
    }
}
