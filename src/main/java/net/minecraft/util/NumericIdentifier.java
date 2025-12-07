package net.minecraft.util;

import org.jetbrains.annotations.NotNull;

public class NumericIdentifier extends ResourceLocation {

    private final int id;

    public NumericIdentifier(int id) {
        super("id", Integer.toString(id));
        this.id = id;
    }

    @Override
    @Deprecated
    public @NotNull String getNamespace() {
        return super.getNamespace();
    }

    @Override
    @Deprecated
    public @NotNull String getPath() {
        return super.getPath();
    }

    public int getId() {
        return id;
    }

}
