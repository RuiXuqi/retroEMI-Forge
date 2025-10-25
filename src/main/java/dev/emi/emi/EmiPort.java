package dev.emi.emi;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.data.EmiTagExclusionsLoader;
import dev.emi.emi.data.RecipeDefaultLoader;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SyntheticIdentifier;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Multiversion quarantine, to avoid excessive git pain
 */
public final class EmiPort {

    public static MutableText literal(String s) {
        return Text.literal(s);
    }

    public static MutableText literal(String s, Formatting formatting) {
        return Text.literal(s).formatted(formatting);
    }

    public static MutableText literal(String s, Formatting... formatting) {
        return Text.literal(s).formatted(formatting);
    }

    public static MutableText literal(String s, Style style) {
        return Text.literal(s).setStyle(style);
    }

    public static MutableText translatable(String s) {
        return Text.translatable(s);
    }

    public static MutableText translatable(String s, Formatting formatting) {
        return Text.translatable(s).formatted(formatting);
    }

    public static MutableText translatable(String s, Object... objects) {
        return Text.translatable(s, objects);
    }

    public static MutableText append(MutableText text, Text appended) {
        return text.append(appended);
    }

    public static OrderedText ordered(Text text) {
        return text.asOrderedText();
    }

//	public static Collection<ResourceLocation> findResources(IResourceManager manager, String prefix, Predicate<String> pred) {
//		return manager.findResources(prefix, i -> pred.test(i.toString())).keySet();
//	}

    public static InputStream getInputStream(IResource resource) {
        try {
            return resource.getInputStream();
        } catch (Exception e) {
            return null;
        }
    }

//	public static BannerPatternsComponent addRandomBanner(BannerPatternsComponent patterns, Random random) {
//		var bannerRegistry = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BANNER_PATTERN);
//		return new BannerPatternsComponent.Builder().addAll(patterns).add(bannerRegistry.getEntry(random.nextInt(bannerRegistry.size())).get(),
//			DyeColor.values()[random.nextInt(DyeColor.values().length)]).build();
//	}
//
/*	public static boolean canTallFlowerDuplicate(BlockTallGrass tallFlowerBlock) {
        try {
            return tallFlowerBlock.canBlockStay(null, 0, 0, 0);
        } catch(Exception e) {
            return false;
        }
    }*/
//
//	public static void setShader(VertexBuffer buf, Matrix4f mat) {
//		buf.bind();
//		buf.draw(mat, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
//	}
//
//	public static List<BakedQuad> getQuads(BakedModel model) {
//		return model.getQuads(null, null, RANDOM);
//	}
//
//	public static void draw(Tessellator bufferBuilder) {
//		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
//	}

    public static int getGuiScale(Minecraft client) {
        return new ScaledResolution(client).getScaleFactor();
    }

    public static void setPositionTexShader() {
        glEnable(GL_TEXTURE_2D);
    }

    public static void setPositionColorTexShader() {
        glEnable(GL_TEXTURE_2D);
    }

    public static IForgeRegistry<IRecipe> getRecipeRegistry() {
        return ForgeRegistries.RECIPES;
    }

    public static IForgeRegistry<Item> getItemRegistry() {
        return ForgeRegistries.ITEMS;
    }

    public static IForgeRegistry<Block> getBlockRegistry() {
        return ForgeRegistries.BLOCKS;
    }

    @Contract(pure = true)
    public static @NotNull Map<String, Fluid> getFluidRegistry() {
        return FluidRegistry.getRegisteredFluids();
    }

    public static IForgeRegistry<Enchantment> getEnchantmentRegistry() {
        return ForgeRegistries.ENCHANTMENTS;
    }

    public static ButtonWidget newButton(int x, int y, int w, int h, Text name, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(name, action).position(x, y).size(w, h).build();
    }

    public static ItemStack getOutput(IRecipe recipe) {
        return recipe.getRecipeOutput();
    }

    public static void focus(TextFieldWidget widget, boolean focused) {
        widget.setFocused(focused);
    }

    public static Stream<Item> getDisabledItems() {
        return EmiPort.getItemRegistry().getValuesCollection().stream().filter(item -> item.getCreativeTab() == null);
    }

    public static ResourceLocation getId(IRecipe recipe) {
        return SyntheticIdentifier.generateId(recipe);
    }

    public static @Nullable IRecipe getRecipe(ResourceLocation id) {
        return getRecipeRegistry().getValue(id);
    }

    public static void registerReloadListeners(IReloadableResourceManager manager) {
        manager.registerReloadListener(new RecipeDefaultLoader());
//		manager.registerReloadListener(new EmiRemoveFromIndex());
        manager.registerReloadListener(new EmiTagExclusionsLoader());
    }

    public static Comparison compareStrict() {
        return Comparison.compareComponents();
    }

//	public static ItemStack setPotion(ItemStack stack, Potion potion) {
//		stack.apply(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT, getPotionRegistry().getEntry(potion), PotionContentsComponent::with);
//		return stack;
//	}

    public static NBTTagCompound emptyExtraData() {
        return null;
    }

    public static ResourceLocation id(String id) {
        if (id.contains(":")) {
            String[] parts = id.split(":");
            String mod = parts[0];//Avoid mods being forced to lowercase and not being able to get them
            String name = parts[1];
            return new ResourceLocation(mod, name);
        }
        return new ResourceLocation(id);
    }

    public static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static void applyModelViewMatrix() {
        RenderSystem.applyModelViewMatrix();
    }
}
