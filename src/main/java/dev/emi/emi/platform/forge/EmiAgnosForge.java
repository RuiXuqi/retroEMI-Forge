package dev.emi.emi.platform.forge;

import com.google.common.collect.Lists;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.EmiPluginContainer;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.registry.tag.ItemKey;
import net.minecraft.text.Text;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.InjectedModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ITypeDiscoverer;
import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.text.WordUtils;
import org.objectweb.asm.Type;

import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class EmiAgnosForge extends EmiAgnos {
    static {
        EmiAgnos.delegate = new EmiAgnosForge();
    }

    @Override
    protected boolean isForgeAgnos() {
        return true;
    }

    @Override
    protected String getModNameAgnos(String namespace) {
        if (namespace.equals("c")) {
            return "Common";
        }
        Optional<? extends ModContainer> container = Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace));
        if (container.isPresent()) {
            return container.get().getName();
        }
        container = Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace.replace('_', '-')));
        if (container.isPresent()) {
            return container.get().getName();
        }
        return WordUtils.capitalizeFully(namespace.replace('_', ' '));
    }

    @Override
    protected Path getConfigDirectoryAgnos() {
        return Loader.instance().getConfigDir().toPath();
    }

    @Override
    protected boolean isDevelopmentEnvironmentAgnos() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    @Override
    protected boolean isModLoadedAgnos(String id) {
        return Loader.isModLoaded(id);
    }

    @Override
    protected List<String> getAllModNamesAgnos(String id) {
        List<String> modNames = new ArrayList<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            if (container.getModId().equals(id)) {
                modNames.add(container.getMetadata().name);
            }
        }
        return modNames;
    }

    @Override
    protected List<String> getAllModAuthorsAgnos(String id) {
        List<String> authors = new ArrayList<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            if (container.getModId().equals(id)) {
                for (String author : container.getMetadata().authorList) {
                    if (!authors.contains(author)) {
                        authors.add(author);
                    }
                }
            }
        }
        return authors;
    }

    @Override
    protected List<String> getModsWithPluginsAgnos() {
//        List<String> mods = Lists.newArrayList();
//        Type entrypointType = Type.getType(EmiEntrypoint.class);
//        for (ModFileScanData data : ModList.get().getAllScanData()) {
//            for (ModFileScanData.AnnotationData annot : data.getAnnotations()) {
//                try {
//                    if (entrypointType.equals(annot.annotationType())) {
//                        mods.add(data.getIModInfoData().get(0).getMods().get(0).getModId());
//                    }
//                } catch (Throwable t) {
//                    EmiLog.error("Exception constructing entrypoint:", t);
//                }
//            }
//        }
//        return mods;
        return com.rewindmc.retroemi.shim.java.List.of();
    }

    @Override
    protected List<EmiPluginContainer> getPluginsAgnos() {
        List<EmiPluginContainer> containers = Lists.newArrayList();
        Type entrypointType = Type.getType(EmiEntrypoint.class);
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            try {
                if (mod instanceof DummyModContainer || (mod instanceof InjectedModContainer container && container.wrappedContainer instanceof DummyModContainer)) {
                    continue;
                }
                JarFile jar = new JarFile(mod.getSource());
                for (JarEntry entry : Collections.list(jar.entries())) {
                    if (ITypeDiscoverer.classFile.matcher(entry.getName()).matches()) {
                        ASMModParser parser = new ASMModParser(jar.getInputStream(entry));
                        for (ModAnnotation annot : parser.getAnnotations().stream().filter(annot -> annot.getASMType().equals(entrypointType)).collect(Collectors.toList())) {
                            Class<?> clazz = Class.forName(annot.getMember());
                            if (EmiPlugin.class.isAssignableFrom(clazz)) {
                                Class<? extends EmiPlugin> pluginClass = clazz.asSubclass(EmiPlugin.class);
                                EmiPlugin plugin = pluginClass.getConstructor().newInstance();
                                String id = mod.getModId();
                                containers.add(new EmiPluginContainer(plugin, id));
                            } else {
                                EmiLog.error("EmiEntrypoint " + annot.getMember() + " does not implement EmiPlugin");
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                EmiLog.error("Exception constructing entrypoint:", t);
            }
        }
        return containers;
    }

    @Override
    protected void addBrewingRecipesAgnos(EmiRegistry registry) {
        //TODO
/*		var tebs = new TileEntityBrewingStand();
        var ingredience = EmiPort.getItemRegistry().getValuesCollection().stream()
                .filter(Objects::nonNull)
                .filter(item -> PotionHelper.isReagent(new ItemStack(item)))
                .collect(Collectors.toList());

        Map<InputPair, Prototype> recipes = new HashMap<>();
        IntList potions = new IntArrayList();
        potions.add(0); // water bottle
        IntSet seenPotions = new IntLinkedOpenHashSet();

        while (!potions.isEmpty()) {
            int[] iter = potions.toIntArray();
            potions.clear();
            for (int potion : iter) {
                seenPotions.add(potion);
                for (Object obj : ingredience) {
                    Item ing = (Item) obj;
                    int result = tebs.func_145936_c(potion, new ItemStack(ing));
                    List<PotionEffect> inputEffects = Items.POTIONITEM.getEffects(potion);
                    List<PotionEffect> resultEffects = Items.POTIONITEM.getEffects(result);
                    if (((potion <= 0 || inputEffects != resultEffects) &&
                            (inputEffects == null || !inputEffects.equals(resultEffects) && resultEffects != null)) ||
                            (!ItemPotion.isSplash(potion) && ItemPotion.isSplash(result))) {
                        if (potion != result && !seenPotions.contains(result)) {
                            potions.add(result);
                            recipes.put(new InputPair(new Prototype(ing), new Prototype(Items.POTIONITEM, potion)), new Prototype(Items.POTIONITEM, result));
                        }
                    }
                }
            }
        }

        for (Map.Entry<InputPair, Prototype> en : recipes.entrySet()) {
            InputPair i = en.getKey();
            registry.addRecipe(new EmiBrewingRecipe(EmiStack.of(i.potion()), EmiStack.of(i.ingredient()), EmiStack.of(en.getValue()),
                    new ResourceLocation("brewing", "/" + i.potion().toStack().getItemDamage() + "/" + i.ingredient().toStack().getTranslationKey() + "/" +
                        en.getValue().toStack().getItemDamage())));
        }
        // Vanilla potion entries have different meta from brewable potions (!)
        // Remove all those uncraftable potions from the index

        registry.removeEmiStacks(
                es -> es.getItemStack() != null && es.getItemStack().getItem() == Items.POTIONITEM && !seenPotions.contains(es.getItemStack().getItemDamage()));

        // We just did an exhaustive search and determined every legitimately obtainable potion
        // So let's just cram those into the index where they're supposed to go.
        // Sort them into something resembling a logical order, though!
        List<EmiStack> sorted = recipes.values().stream().filter(p -> p.meta() != 0).distinct().sorted((b, a) -> {
            int i = Boolean.compare(ItemPotion.isSplash(a.meta()), ItemPotion.isSplash(b.meta()));
            if (i != 0) {
                return i;
            }
            List<PotionEffect> effA = ((List<PotionEffect>) ((ItemPotion) a.getItem()).getEffects(a.toStack()));
            List<PotionEffect> effB = ((List<PotionEffect>) ((ItemPotion) b.getItem()).getEffects(b.toStack()));
            return listCompare(effA, effB, Comparator.comparingInt(PotionEffect::getPotionID).thenComparingInt(PotionEffect::getAmplifier).thenComparingInt(PotionEffect::getDuration));
        }).map(EmiStack::of).collect(Collectors.toList());
        EmiStack prev = EmiStack.of(new Prototype(Items.POTIONITEM, 0));
        for (EmiStack potion : sorted) {
            registry.addEmiStackAfter(potion, prev);
        }*/
    }

    private static <T> int listCompare(List<T> a, List<T> b, Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }

        int length = Math.min(a.size(), b.size());
        for (int i = 0; i < length; i++) {
            T oa = a.get(i);
            T ob = b.get(i);
            if (oa != ob) {
                // Null-value comparison is deferred to the comparator
                int v = cmp.compare(oa, ob);
                if (v != 0) {
                    return v;
                }
            }
        }

        return a.size() - b.size();
    }

//	@SuppressWarnings("RedundantCast")
//	@Override
//	protected List<TooltipComponent> getItemTooltipAgnos(ItemStack stack) {
//		if (MinecraftServerEMI.getIsServer()) {
//			String var5 = stack.getDisplayName();
//
//			if (stack.hasDisplayName())
//			{
//				var5 = EnumChatFormatting.ITALIC + var5 + EnumChatFormatting.RESET;
//			}
//			return Collections.singletonList(TooltipComponent.of(Text.literal(var5)));
//		}
//		else {
//			// I SWEAR TO GOD DON'T YOU FUCKING TOUCH THIS CAST
//			EntityPlayer player = (EntityPlayer) (Object) Minecraft.getMinecraft().thePlayer;
//			while (player == null) {
//				// THE CLASSLOADER IS A LIE
//				player = (EntityPlayer) (Object) Minecraft.getMinecraft().thePlayer;
//				try {
//					Thread.sleep(5);
//				}
//				catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}
//			}
//			List<String> tip = stack.getTooltip(player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips, (Slot) null);
//			for (int i = 0; i < tip.size(); i++) {
//				tip.set(i, "§" + (i == 0 ? Integer.toHexString(stack.getRarity().rarityColor) : "7") + tip.get(i));
//			}
//			return tip.stream().map(Text::literal).map(TooltipComponent::of).collect(Collectors.toList());
//		}
//	}

    @Override
    protected List<TooltipComponent> getItemTooltipAgnos(ItemStack stack) {
        List<String> tip = stack.getTooltip(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        for (int i = 0; i < tip.size(); i++) {
            tip.set(i, (i == 0 ? stack.getItem().getForgeRarity(stack).getColor() : TextFormatting.GRAY) + tip.get(i));
        }
        return tip.stream()
                .map(Text::literal).map(TooltipComponent::of)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    protected Text getFluidNameAgnos(Fluid fluid, NBTTagCompound nbt) {
        return Text.literal(new FluidStack(fluid, 1000, nbt).getLocalizedName());
    }

    @Override
    protected List<Text> getFluidTooltipAgnos(Fluid fluid, NBTTagCompound nbt) {
        return Collections.singletonList(getFluidNameAgnos(fluid, nbt));
    }

    @Override
    protected boolean isFloatyFluidAgnos(FluidEmiStack stack) {
        FluidStack fs = new FluidStack(stack.getKeyOfType(Fluid.class), 1000, stack.getNbt());
        return fs.getFluid().getDensity() <= 0;
    }

    @Override
    protected void renderFluidAgnos(FluidEmiStack stack, MatrixStack matrices, int x, int y, float delta, int xOff, int yOff, int width, int height) {
        FluidStack fs = new FluidStack(stack.getKeyOfType(Fluid.class), 1000, stack.getNbt());
        Minecraft mc = Minecraft.getMinecraft();
        ResourceLocation fluidStill = fs.getFluid().getStill();
        TextureMap textureMapBlocks = mc.getTextureMapBlocks();
        TextureAtlasSprite fluidStillSprite = null;
        if (fluidStill != null) {
            fluidStillSprite = textureMapBlocks.getTextureExtry(fluidStill.toString());
        }
        if (fluidStillSprite == null) {
            fluidStillSprite = textureMapBlocks.getMissingSprite();
        }
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        mc.currentScreen.drawTexturedModalRect(x, y, fluidStillSprite, 16, 16);
    }

    @Override
    protected EmiStack createFluidStackAgnos(Object object) {
        if (object instanceof FluidStack f) {
            return EmiStack.of(f.getFluid(), f.tag, f.amount);
        }
        return EmiStack.EMPTY;
    }

    @Override
    protected boolean canBatchAgnos(ItemStack stack) {
        return false;
    }

    @Override
    protected Map<ItemKey, Integer> getFuelMapAgnos() {
        Map<ItemKey, Integer> fuelMap = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof ItemPotion) {
                NonNullList<ItemStack> stacks = NonNullList.create();
                item.getSubItems(CreativeTabs.MISC, stacks);
                for (ItemStack stack : stacks) {
                    int time = TileEntityFurnace.getItemBurnTime(stack);
                    if (time > 0) {
                        fuelMap.put(ItemKey.of(stack), time);
                    }
                }
            }
        }
        return fuelMap;
    }

    @Override
    protected boolean isEnchantableAgnos(ItemStack stack, Enchantment enchantment) {
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.setEnchantments(Collections.singletonMap(enchantment, enchantment.getMaxLevel()), enchantedBook);
        return stack.getItem().isBookEnchantable(stack, enchantedBook);
    }
}
