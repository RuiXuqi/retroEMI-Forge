package net.minecraft.registry.tag;

import com.google.common.collect.BiMap;
import com.rewindmc.retroemi.RetroEMI;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.oredict.OreDictionary;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TagKey<T> {
    private final ResourceLocation tag;
    private final Type type;

    public TagKey(ResourceLocation tag, Type type) {
        this.tag = tag;
        this.type = type;
    }

    public static <T> TagKey<T> of(Class<T> type, ResourceLocation tag) {
        return new TagKey<>(tag, Type.of(type));
    }

    public static <T> TagKey<T> of(Type type, ResourceLocation tag) {
        return new TagKey<>(tag, type);
    }

    public List<T> getAll() {
        return switch (type) {
            case ITEM ->
                    (List<T>) expand(OreDictionary.getOres(convertTag(tag))).stream().map(ItemKey::of).collect(Collectors.toList());
            case BLOCK ->
                    (List<T>) expand(OreDictionary.getOres(convertTag(tag))).stream().filter(stack -> stack.getItem() instanceof ItemBlock)
                            .map(stack -> ((ItemBlock) stack.getItem()).getBlock()).collect(Collectors.toList());
            case FLUID -> {
                //todo this probably breaks everything because backup fluids are not registered
                String oredict = ":" + convertTag(tag);
                List<String> modIds = new ArrayList<>(Loader.instance().getModList().stream().map(ModContainer::getModId).collect(Collectors.toList()));
                modIds.add("minecraft");
                yield (List<T>) modIds.stream().map(s -> s + oredict).map(s -> getMasterFluidReference().get(s)).filter(Objects::nonNull).collect(Collectors.toList());
            }
        };
    }

    //Expand all stacks with wildcard meta to their variants
    private static List<ItemStack> expand(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                NonNullList<ItemStack> itemStacks = NonNullList.create();
                stack.getItem().getSubItems(CreativeTabs.MISC, itemStacks);
                result.addAll(itemStacks);
            } else {
                result.add(stack);
            }
        }
        return result;
    }

    private BiMap<String, Fluid> getMasterFluidReference() {
        try {
            Field field = FluidRegistry.class.getDeclaredField("masterFluidReference");
            field.setAccessible(true);
            return (BiMap<String, Fluid>) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(T entry) {
        return getAll().contains(entry);
    }

    public ResourceLocation getTag() {
        return tag;
    }

    public Type getType() {
        return type;
    }

    public ResourceLocation id() {
        return tag;
    }

    private static String convertTag(ResourceLocation tag) {
        if (tag.getNamespace().equals("forge") || tag.getNamespace().equals("c")) {
            return formatTag(tag.getPath());
        } else {
            return formatTag(tag.getNamespace() + "/" + tag.getPath());
        }
    }

    private static ResourceLocation convertOredict(String oredict) {
        return new ResourceLocation("forge", formatOredict(oredict));
    }

    private static String formatTag(String tag) {
        String result = tag;
        while (result.contains("/")) {
            int nextChar = result.indexOf("/") + 1;
            result = RetroEMI.replaceCharAt(result, nextChar, Character.toUpperCase(result.charAt(nextChar)));
            result = result.replaceFirst("/", "");
        }
        return result;
    }

    private static String formatOredict(String oredict) {
        StringBuilder result = new StringBuilder();
        for (char c : oredict.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append("/" + Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public enum Type {
        ITEM,
        BLOCK,
        FLUID;

        public static Type of(Class<?> type) {
            if (type == ItemKey.class) {
                return ITEM;
            } else if (type == Block.class) {
                return BLOCK;
            } else if (type == Fluid.class) {
                return FLUID;
            }
            throw new IllegalArgumentException("Unsupported tag type: " + type);
        }

        public static Type of(ResourceLocation id) {
            String type = id.toString();
            return switch (type) {
                case "minecraft:item" -> ITEM;
                case "minecraft:block" -> BLOCK;
                case "minecraft:fluid" -> FLUID;
                default -> throw new IllegalArgumentException("Unsupported tag type: " + type);
            };
        }

        public List<TagKey<?>> getAll() {
            return switch (this) {
                case ITEM ->
                        Arrays.stream(OreDictionary.getOreNames()).map(s -> TagKey.of(this, convertOredict(s))).collect(Collectors.toList());
                case BLOCK ->
                        Arrays.stream(OreDictionary.getOreNames()).map(s -> TagKey.of(this, convertOredict(s))).filter(tag -> !tag.getAll().isEmpty()).collect(Collectors.toList());
                case FLUID ->
                        FluidRegistry.getRegisteredFluids().keySet().stream().map(s -> TagKey.of(this, convertOredict(s))).collect(Collectors.toList());
            };
        }

        public ResourceLocation getRegistryName() {
            return new ResourceLocation(name().toLowerCase());
        }
    }

}
