package net.minecraft.text;

import net.minecraft.util.Formatting;

public interface MutableText extends Text {

    MutableText setStyle(Style s);

    MutableText formatted(Formatting fmt);

    MutableText formatted(Formatting... fmt);

    MutableText append(Text text);

}
