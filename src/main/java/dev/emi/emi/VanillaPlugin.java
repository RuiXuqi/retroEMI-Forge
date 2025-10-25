package dev.emi.emi;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rewindmc.retroemi.RetroEMI;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.config.EffectLocation;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.FluidUnit;
import dev.emi.emi.handler.CookingRecipeHandler;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.handler.InventoryRecipeHandler;
import dev.emi.emi.mixin.early.accessor.ItemToolAccessor;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.recipe.*;
import dev.emi.emi.recipe.forge.EmiShapedOreRecipe;
import dev.emi.emi.recipe.forge.EmiShapelessOreRecipe;
import dev.emi.emi.recipe.special.*;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.stack.serializer.FluidEmiStackSerializer;
import dev.emi.emi.stack.serializer.ItemEmiStackSerializer;
import dev.emi.emi.stack.serializer.ListEmiIngredientSerializer;
import dev.emi.emi.stack.serializer.TagEmiIngredientSerializer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.registry.tag.ItemKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.*;

@EmiEntrypoint
public class VanillaPlugin implements EmiPlugin {
    public static EmiRecipeCategory TAG = new EmiRecipeCategory(EmiPort.id("emi:tag"),
            EmiStack.of(Items.NAME_TAG), simplifiedRenderer(240, 208), EmiRecipeSorting.none());

    public static EmiRecipeCategory INGREDIENT = new EmiRecipeCategory(EmiPort.id("emi:ingredient"),
            EmiStack.of(Items.COMPASS), simplifiedRenderer(240, 208));
    public static EmiRecipeCategory RESOLUTION = new EmiRecipeCategory(EmiPort.id("emi:resolution"),
            EmiStack.of(Items.COMPASS), simplifiedRenderer(240, 208));

    static {
        CRAFTING = new EmiRecipeCategory(EmiPort.id("minecraft:crafting"),
                EmiStack.of(Blocks.CRAFTING_TABLE), simplifiedRenderer(240, 240), EmiRecipeSorting.compareOutputThenInput());
        SMELTING = new EmiRecipeCategory(EmiPort.id("minecraft:smelting"),
                EmiStack.of(Blocks.FURNACE), simplifiedRenderer(224, 240), EmiRecipeSorting.compareOutputThenInput());
        ANVIL_REPAIRING = new EmiRecipeCategory(EmiPort.id("emi:anvil_repairing"),
                EmiStack.of(Blocks.ANVIL), simplifiedRenderer(240, 224), EmiRecipeSorting.none());
        BREWING = new EmiRecipeCategory(EmiPort.id("minecraft:brewing"),
                EmiStack.of(Items.BREWING_STAND), simplifiedRenderer(224, 224), EmiRecipeSorting.none());
        WORLD_INTERACTION = new EmiRecipeCategory(EmiPort.id("emi:world_interaction"),
                EmiStack.of(Blocks.GRASS), simplifiedRenderer(208, 224), EmiRecipeSorting.none());
        EmiRenderable flame = (matrices, x, y, delta) -> {
            EmiTexture.FULL_FLAME.render(matrices, x + 1, y + 1, delta);
        };
        FUEL = new EmiRecipeCategory(EmiPort.id("emi:fuel"), flame, flame, EmiRecipeSorting.compareInputThenOutput());
        INFO = new EmiRecipeCategory(EmiPort.id("emi:info"),
                EmiStack.of(Items.WRITABLE_BOOK), simplifiedRenderer(208, 224), EmiRecipeSorting.none());
    }


    @Override
    public void initialize(EmiInitRegistry registry) {
        registry.addIngredientSerializer(ItemEmiStack.class, new ItemEmiStackSerializer());
        registry.addIngredientSerializer(FluidEmiStack.class, new FluidEmiStackSerializer());
        registry.addIngredientSerializer(TagEmiIngredient.class, new TagEmiIngredientSerializer());
        registry.addIngredientSerializer(ListEmiIngredient.class, new ListEmiIngredientSerializer());

        registry.addRegistryAdapter(EmiRegistryAdapter.simple(ItemKey.class, TagKey.Type.ITEM, (key, nbt, amount) -> EmiStack.of(key.item(), nbt, amount, key.meta())));
        registry.addRegistryAdapter(EmiRegistryAdapter.simple(Fluid.class, TagKey.Type.FLUID, EmiStack::of));
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CRAFTING);
        registry.addCategory(SMELTING);
        registry.addCategory(ANVIL_REPAIRING);
        registry.addCategory(BREWING);
        registry.addCategory(WORLD_INTERACTION);
        registry.addCategory(FUEL);
        registry.addCategory(INFO);
        registry.addCategory(TAG);
        registry.addCategory(INGREDIENT);
        registry.addCategory(RESOLUTION);

        registry.addWorkstation(CRAFTING, EmiStack.of(Blocks.CRAFTING_TABLE));
        registry.addWorkstation(SMELTING, EmiStack.of(Blocks.FURNACE));
        registry.addWorkstation(ANVIL_REPAIRING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(ANVIL_REPAIRING, EmiStack.of(new ItemStack(Blocks.ANVIL, 1, 1)));
        registry.addWorkstation(ANVIL_REPAIRING, EmiStack.of(new ItemStack(Blocks.ANVIL, 1, 2)));
        registry.addWorkstation(BREWING, EmiStack.of(Items.BREWING_STAND));

        registry.addRecipeHandler(ContainerPlayer.class, new InventoryRecipeHandler());
        registry.addRecipeHandler(ContainerWorkbench.class, new CraftingRecipeHandler());
        registry.addRecipeHandler(ContainerFurnace.class, new CookingRecipeHandler<>(SMELTING));

        registry.addExclusionArea(GuiContainerCreative.class, (screen, consumer) -> {
            int left = screen.getGuiLeft();
            int top = screen.getGuiTop();
            int width = screen.getXSize();
            int bottom = top + screen.getYSize();
            consumer.accept(new Bounds(left, top - 28, width, 28));
            consumer.accept(new Bounds(left, bottom, width, 28));
        });

        registry.addGenericExclusionArea((screen, consumer) -> {
            if (EmiConfig.effectLocation != EffectLocation.HIDDEN && screen instanceof InventoryEffectRenderer inv) {
                Minecraft client = Minecraft.getMinecraft();
                Collection<PotionEffect> collection = client.player.getActivePotionEffects();
                if (!collection.isEmpty()) {
                    int k = 33;
                    if (collection.size() > 5) {
                        k = 132 / (collection.size() - 1);
                    }
                    int right = inv.getGuiLeft() + inv.getXSize() + 2;
                    int rightWidth = inv.width - right;
                    if (rightWidth >= 32) {
                        int top = inv.getGuiTop();
                        int height = (collection.size() - 1) * k + 32;
                        int left, width;
                        if (EmiConfig.effectLocation == EffectLocation.TOP) {
                            int size = collection.size();
                            top = inv.getGuiTop() - 34;
                            if (screen instanceof GuiContainerCreative) {
                                top -= 28;
                                if (EmiAgnos.isForge()) {
                                    top -= 22;
                                }
                            }
                            int xOff = 34;
                            if (size == 1) {
                                xOff = 122;
                            } else if (size > 5) {
                                xOff = (inv.getXSize() - 32) / (size - 1);
                            }
                            width = Math.max(122, (size - 1) * xOff + 32);
                            left = inv.getGuiLeft() + (inv.getXSize() - width) / 2;
                            height = 32;
                        } else {
                            left = switch (EmiConfig.effectLocation) {
                                case LEFT_COMPRESSED -> inv.getGuiLeft() - 2 - 32;
                                case LEFT -> inv.getGuiLeft() - 2 - 120;
                                default -> right;
                            };
                            width = switch (EmiConfig.effectLocation) {
                                case LEFT, RIGHT -> 120;
                                case LEFT_COMPRESSED, RIGHT_COMPRESSED -> 32;
                                default -> 32;
                            };
                        }
                        consumer.accept(new Bounds(left, top, width, height));
                    }
                }
            }
        });

        Comparison potionComparison = Comparison.of((a, b) -> RetroEMI.getEffects(a).equals(RetroEMI.getEffects(b)));

        registry.setDefaultComparison(Items.POTIONITEM, potionComparison);
        registry.setDefaultComparison(Items.ENCHANTED_BOOK, EmiPort.compareStrict());

        Set<Item> hiddenItems = Stream.concat(
                TagKey.of(ItemKey.class, EmiTags.HIDDEN_FROM_RECIPE_VIEWERS).getAll().stream().map(ItemKey::item),
                EmiPort.getDisabledItems()
        ).collect(Collectors.toSet());

        List<Item> dyeableItems = RetroEMI.getAllItems().stream().filter(i -> i instanceof ItemArmor armor && armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER).collect(Collectors.toList());

        for (Item i : EmiRepairItemRecipe.TOOLS) {
            if (!hiddenItems.contains(i)) {
                addRecipeSafe(registry, () -> new EmiRepairItemRecipe(i, synthetic("crafting/repairing", EmiUtil.subId(i))));
            }
        }

        for (IRecipe recipe : EmiPort.getRecipeRegistry().getValuesCollection()) {
            if (recipe instanceof RecipesMapExtending map) {
                EmiStack paper = EmiStack.of(Items.PAPER);
                addRecipeSafe(registry, () -> new EmiCraftingRecipe(com.rewindmc.retroemi.shim.java.List.of(
                        paper, paper, paper, paper,
                        EmiStack.of(Items.MAP),
                        paper, paper, paper, paper
                ),
                        EmiStack.of(Items.MAP),
                        new ResourceLocation("minecraft", "map_extending"), false), recipe);
            } else if (recipe instanceof ShapedRecipes shaped) {
                if (shaped.getRecipeWidth() <= 3 && shaped.getRecipeHeight() <= 3) {
                    addRecipeSafe(registry, () -> new EmiShapedRecipe(shaped), recipe);
                }
            } else if (recipe instanceof ShapelessRecipes shapeless && shapeless.getIngredients().size() <= 9) {
                addRecipeSafe(registry, () -> new EmiShapelessRecipe(shapeless), shapeless);
            } else if (recipe instanceof ShapedOreRecipe shaped) {
                int width = shaped.getRecipeWidth();
                if (width <= 3 && shaped.getRecipeHeight() <= 3) {
                    addRecipeSafe(registry, () -> new EmiShapedOreRecipe(shaped));
                }
            } else if (recipe instanceof ShapelessOreRecipe shapeless && recipe.getIngredients().size() <= 9) {
                addRecipeSafe(registry, () -> new EmiShapelessOreRecipe(shapeless));
            } else if (recipe instanceof RecipesArmorDyes) {
                for (Item i : dyeableItems) {
                    if (!hiddenItems.contains(i)) {
                        addRecipeSafe(registry, () -> new EmiArmorDyeRecipe(i, synthetic("crafting/dying", EmiUtil.subId(i))), recipe);
                    }
                }
            } else if (recipe instanceof RecipeFireworks) {
                // All firework recipes are one recipe in 1.7
                addRecipeSafe(registry, () -> new EmiFireworkStarRecipe(new ResourceLocation("minecraft", "firework_star")), recipe);
                addRecipeSafe(registry, () -> new EmiFireworkStarFadeRecipe(new ResourceLocation("minecraft", "firework_star_fade")), recipe);
                addRecipeSafe(registry, () -> new EmiFireworkRocketRecipe(new ResourceLocation("minecraft", "firework_rocket")), recipe);
            } else if (recipe instanceof RecipesMapCloning map) {
                addRecipeSafe(registry, () -> new EmiMapCloningRecipe(new ResourceLocation("minecraft", "map_cloning")), recipe);
            } else {
                // No way to introspect arbitrary recipes in 1.7. :(
            }
        }

        for (var recipe : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            ItemStack in = recipe.getKey();
            ItemStack out = recipe.getValue();
            String id = in.getTranslationKey() + "." + in.getItemDamage() + "/" + out.getTranslationKey() + "." + in.getItemDamage();
            float xp = FurnaceRecipes.instance().getSmeltingExperience(out);
            addRecipeSafe(registry, () -> new EmiCookingRecipe(new ResourceLocation("smelting", "furnace/" + id), in, out, xp, SMELTING, 1, false));
        }

        safely("repair", () -> addRepair(registry, hiddenItems));
        safely("brewing", () -> EmiAgnos.addBrewingRecipes(registry));
        safely("world interaction", () -> addWorldInteraction(registry, hiddenItems, dyeableItems));
        safely("fuel", () -> addFuel(registry, hiddenItems));

        for (TagKey<?> key : EmiTags.TAGS) {
            if (new TagEmiIngredient(key, 1).getEmiStacks().size() > 1) {
                addRecipeSafe(registry, () -> new EmiTagRecipe(key));
            }
        }
    }

    private static void addRepair(EmiRegistry registry, Set<Item> hiddenItems) {
        List<Enchantment> targetedEnchantments = Lists.newArrayList();
        List<Enchantment> universalEnchantments = Lists.newArrayList();
        for (Enchantment enchantment : EmiPort.getEnchantmentRegistry().getValuesCollection()) {
            try {
                if (enchantment.canApply(new ItemStack(Blocks.AIR))) {
                    universalEnchantments.add(enchantment);
                    continue;
                }
            } catch (Throwable t) {
            }
            targetedEnchantments.add(enchantment);
            for (int i = 1; i <= enchantment.getMaxLevel(); i++) {
                registry.addEmiStack(EmiStack.of(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(enchantment, i))));
            }
        }
        for (Item i : RetroEMI.getAllItems()) {
            if (hiddenItems.contains(i)) {
                continue;
            }
            try {
                if (i.getMaxDamage(new ItemStack(i)) > 0) {
                    if (i instanceof ItemArmor ai) {
                        ai.getArmorMaterial().getRepairItemStack();
                        ResourceLocation id = synthetic("anvil/repairing/material", EmiUtil.subId(i) + "/" + EmiUtil.subId(ai.getArmorMaterial().getRepairItemStack()));
                        addRecipeSafe(registry, () -> new EmiAnvilRecipe(EmiStack.of(i), EmiStack.of(ai.getArmorMaterial().getRepairItemStack()), id));
                    } else if (i instanceof ItemTool ti) {
                        ResourceLocation id = synthetic("anvil/repairing/material", EmiUtil.subId(i) + "/" + EmiUtil.subId(((ItemToolAccessor) ti).getToolMaterial().getRepairItemStack().getItem()));
                        addRecipeSafe(registry, () -> new EmiAnvilRecipe(EmiStack.of(i), EmiStack.of(((ItemToolAccessor) ti).getToolMaterial().getRepairItemStack()), id));
                    }
                }
                if (i.isDamageable()) {
                    addRecipeSafe(registry, () -> new EmiAnvilRepairItemRecipe(i, synthetic("anvil/repairing/tool", EmiUtil.subId(i))));
                }
            } catch (Throwable t) {
                EmiLog.error("Exception thrown registering repair recipes", t);
            }
            try {
                ItemStack defaultStack = new ItemStack(i);
                int acceptableEnchantments = 0;
                Consumer<Enchantment> consumer = e -> {
                    int max = e.getMaxLevel();
                    addRecipeSafe(registry, () -> new EmiAnvilEnchantRecipe(i, e, max,
                            synthetic("anvil/enchanting", EmiUtil.subId(i) + "/" + e.getName() + "/" + max)));
                };
                for (Enchantment e : targetedEnchantments) {
                    if (e.canApply(defaultStack)
                            && EmiAgnos.isEnchantable(defaultStack, e)) {
                        consumer.accept(e);
                        acceptableEnchantments++;
                    }
                }
                if (acceptableEnchantments > 0) {
                    for (Enchantment e : universalEnchantments) {
                        if (e.canApply(defaultStack)) {
                            consumer.accept(e);
                            acceptableEnchantments++;
                        }
                    }
                }
            } catch (Throwable t) {
                EmiReloadLog.warn("Exception thrown registering enchantment recipes", t);
            }
        }
        NonNullList<ItemStack> stacks = NonNullList.create();
        Blocks.DOUBLE_PLANT.getSubBlocks(CreativeTabs.MISC, stacks);
        for (ItemStack stack : stacks) {
            if (stack.getItemDamage() != 2 && stack.getItemDamage() != 3) {
                addRecipeSafe(registry, () -> basicWorld(EmiStack.of(stack).setRemainder(EmiStack.of(stack)), EmiStack.of(Items.DYE, 1, 15), EmiStack.of(stack),
                        synthetic("world/flower_duping", EmiUtil.subId(EmiPort.id(stack.getItem() + "." + stack.getItemDamage()))), false));
            }
        }
    }

    private static void addWorldInteraction(EmiRegistry registry, Set<Item> hiddenItems, List<Item> dyeableItems) {
//		EmiIngredient hoes = damagedTool(getPreferredTag(com.rewindmc.retroemi.shim.java.List.of(
//				"minecraft:hoes", "c:hoes", "c:tools/hoes", "fabric:hoes", "forge:tools/hoes"
//			), EmiStack.of(Items.iron_hoe)), 1);
        for (Item item : RetroEMI.getAllItems()) {
            if (item instanceof ItemHoe hoe) {
                EmiIngredient hoes = damagedTool(EmiStack.of(hoe), 1);
                EmiIngredient dirt = EmiIngredient.of(com.rewindmc.retroemi.shim.java.List.of(EmiStack.of(Blocks.DIRT), EmiStack.of(Blocks.GRASS)));
                ResourceLocation id = synthetic("world/tilling", EmiUtil.subId(EmiPort.id(hoe.getTranslationKey() + "." + Blocks.DIRT)));
                addRecipeSafe(registry, () -> basicWorld(dirt, hoes, EmiStack.of(Blocks.FARMLAND), id));
            }
        }

        for (Item i : dyeableItems) {
            if (hiddenItems.contains(i)) {
                continue;
            }
            EmiStack cauldron = EmiStack.of(Items.CAULDRON);
            EmiStack waterThird = EmiStack.of(FluidRegistry.WATER, FluidUnit.BOTTLE);
            int uniq = EmiUtil.RANDOM.nextInt();
            addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
                    .id(synthetic("world/cauldron_washing", EmiUtil.subId(i)))
                    .leftInput(EmiStack.EMPTY, s -> new GeneratedSlotWidget(r -> {
                        ItemStack stack = new ItemStack(i);
                        ((ItemArmor) i).setColor(stack, r.nextInt(0xFFFFFF + 1));
                        return EmiStack.of(stack);
                    }, uniq, s.getBounds().x(), s.getBounds().y()))
                    .rightInput(cauldron, true)
                    .rightInput(waterThird, false)
                    .output(EmiStack.of(i))
                    .supportsRecipeTree(false)
                    .build());
        }

        EmiStack water = EmiStack.of(FluidRegistry.WATER, FluidUnit.BUCKET);
        EmiStack lava = EmiStack.of(FluidRegistry.LAVA, FluidUnit.BUCKET);
        EmiStack waterCatalyst = water.copy().setRemainder(water);
        EmiStack lavaCatalyst = lava.copy().setRemainder(lava);

        addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
                .id(synthetic("world/fluid_spring", "minecraft/water"))
                .leftInput(waterCatalyst)
                .rightInput(waterCatalyst, false)
                .output(EmiStack.of(FluidRegistry.WATER, FluidUnit.BUCKET))
                .build());
        addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
                .id(synthetic("world/fluid_interaction", "minecraft/cobblestone"))
                .leftInput(waterCatalyst)
                .rightInput(lavaCatalyst, false)
                .output(EmiStack.of(Blocks.COBBLESTONE))
                .build());
        addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
                .id(synthetic("world/fluid_interaction", "minecraft/stone"))
                .leftInput(waterCatalyst)
                .rightInput(lavaCatalyst, false)
                .output(EmiStack.of(Blocks.STONE))
                .build());
        addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
                .id(synthetic("world/fluid_interaction", "minecraft/obsidian"))
                .leftInput(lava)
                .rightInput(waterCatalyst, false)
                .output(EmiStack.of(Blocks.OBSIDIAN))
                .build());

/*        for (var entry : FluidContainerRegistry.getRegisteredFluidContainerData()) {
            Fluid fluid = entry.fluid.getFluid();
            if (entry.emptyContainer.getUnlocalizedName() == Items.BUCKET.getTranslationKey()) {
                ItemStack bucket = entry.filledContainer;
                addRecipeSafe(registry, () -> basicWorld(EmiStack.of(Items.BUCKET), EmiStack.of(fluid, FluidUnit.BUCKET), EmiStack.of(bucket),
                        synthetic("emi", "bucket_filling/" + EmiUtil.subId(fluid)), false));
            }
        }*/

        addRecipeSafe(registry, () -> basicWorld(EmiStack.of(Items.GLASS_BOTTLE), water,
                EmiStack.of(Items.POTIONITEM),
                synthetic("world/unique", "minecraft/water_bottle")));
    }

    private static EmiIngredient damagedTool(EmiIngredient tool, int damage) {
        for (EmiStack stack : tool.getEmiStacks()) {
            ItemStack is = stack.getItemStack().copy();
            is.setItemDamage(damage);
            stack.setRemainder(EmiStack.of(is));
        }
        return tool;
    }

    private static EmiIngredient getPreferredTag(List<String> candidates, EmiIngredient fallback) {
        for (String id : candidates) {
            EmiIngredient potential = EmiIngredient.of(TagKey.of(ItemKey.class, EmiPort.id(id)));
            if (!potential.isEmpty()) {
                return potential;
            }
        }
        return fallback;
    }

    private static void addFuel(EmiRegistry registry, Set<Item> hiddenItems) {
        Map<ItemKey, Integer> fuelMap = EmiAgnos.getFuelMap();
        compressRecipesToTags(fuelMap.keySet().stream().collect(Collectors.toSet()), (a, b) -> {
            return Integer.compare(fuelMap.get(a), fuelMap.get(b));
        }, tag -> {
            EmiIngredient stack = EmiIngredient.of(tag);
            Item item = stack.getEmiStacks().get(0).getItemStack().getItem();
            int time = fuelMap.get(item);
            registry.addRecipe(new EmiFuelRecipe(stack, time, synthetic("fuel/tag", EmiUtil.subId(tag.id()))));
        }, item -> {
            if (!hiddenItems.contains(item.item())) {
                int time = fuelMap.get(item);
                registry.addRecipe(new EmiFuelRecipe(EmiStack.of(item.toStack()), time, synthetic("fuel/item", EmiUtil.subId(EmiPort.id(item.toStack().getTranslationKey() + "@" + Item.getIdFromItem(item.item()) + "." + item.toStack().getItemDamage())))));
            }
        });
    }

    private static void compressRecipesToTags(Set<ItemKey> stacks, Comparator<ItemKey> comparator, Consumer<TagKey<ItemKey>> tagConsumer, Consumer<ItemKey> itemConsumer) {
        Set<ItemKey> handled = Sets.newHashSet();
        outer:
        for (TagKey<ItemKey> key : (List<TagKey<ItemKey>>) (List<?>) EmiTags.getTags(TagKey.Type.ITEM)) {
            List<ItemKey> items = key.getAll();
            if (items.size() < 2) {
                continue;
            }
            ItemKey base = items.get(0);
            if (!stacks.contains(base)) {
                continue;
            }
            for (int i = 1; i < items.size(); i++) {
                ItemKey item = items.get(i);
                if (!stacks.contains(item) || comparator.compare(base, item) != 0) {
                    continue outer;
                }
            }
            if (handled.containsAll(items)) {
                continue;
            }
            handled.addAll(items);
            tagConsumer.accept(key);
        }
        for (ItemKey item : stacks) {
            if (handled.contains(item)) {
                continue;
            }
            itemConsumer.accept(item);
        }
    }

    private static ResourceLocation synthetic(String type, String name) {
        return EmiPort.id(type, "/" + name);
    }

    private static void safely(String name, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            EmiReloadLog.warn("Exception thrown when reloading " + name + " step in vanilla EMI plugin", t);
        }
    }

    private static void addRecipeSafe(EmiRegistry registry, Supplier<EmiRecipe> supplier) {
        try {
            registry.addRecipe(supplier.get());
        } catch (Throwable e) {
            EmiReloadLog.warn("Exception thrown when parsing EMI recipe (no ID available)", e);
        }
    }

    private static void addRecipeSafe(EmiRegistry registry, Supplier<EmiRecipe> supplier, IRecipe recipe) {
        try {
            registry.addRecipe(supplier.get());
        } catch (Throwable e) {
            EmiReloadLog.warn("Exception thrown when parsing vanilla recipe " + EmiPort.getId(recipe), e);
        }
    }

    private static EmiRenderable simplifiedRenderer(int u, int v) {
        return (raw, x, y, delta) -> {
            EmiDrawContext context = EmiDrawContext.wrap(raw);
            context.drawTexture(EmiRenderHelper.WIDGETS, x, y, u, v, 16, 16);
        };
    }

    private static void addConcreteRecipe(EmiRegistry registry, Block powder, EmiStack water, Block result) {
        addRecipeSafe(registry, () -> basicWorld(EmiStack.of(powder), water, EmiStack.of(result),
                synthetic("world/concrete", EmiUtil.subId(result))));
    }

    private static EmiRecipe basicWorld(EmiIngredient left, EmiIngredient right, EmiStack output, ResourceLocation id) {
        return basicWorld(left, right, output, id, true);
    }

    private static EmiRecipe basicWorld(EmiIngredient left, EmiIngredient right, EmiStack output, ResourceLocation id, boolean catalyst) {
        return EmiWorldInteractionRecipe.builder()
                .id(id)
                .leftInput(left)
                .rightInput(right, catalyst)
                .output(output)
                .build();
    }
}
