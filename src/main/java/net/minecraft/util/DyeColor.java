package net.minecraft.util;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;

public enum DyeColor {
    WHITE,
    ORANGE,
    MAGENTA,
    LIGHT_BLUE,
    YELLOW,
    LIME,
    PINK,
    GRAY,
    LIGHT_GRAY,
    CYAN,
    PURPLE,
    BLUE,
    BROWN,
    GREEN,
    RED,
    BLACK,
    ;

    public float[] getColorComponents() {
        return EnumDyeColor.byMetadata(ordinal()).getColorComponentValues();
    }

    public int getFireworkColor() {
        return ItemDye.DYE_COLORS[ordinal()];
    }
}
