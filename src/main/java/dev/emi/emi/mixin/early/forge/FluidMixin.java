package dev.emi.emi.mixin.early.forge;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackConvertible;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.Fluid;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Fluid.class)
public class FluidMixin implements EmiStackConvertible {
    @Override
    public EmiStack emi() {
        return EmiStack.of((Item) (Object) this);
    }

    @Override
    public EmiStack emi(long amount) {
        return EmiStack.of((Item) (Object) this, amount);
    }
}
