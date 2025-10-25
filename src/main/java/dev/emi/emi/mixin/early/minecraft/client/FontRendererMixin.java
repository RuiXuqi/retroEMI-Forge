package dev.emi.emi.mixin.early.minecraft.client;

import com.rewindmc.retroemi.REMIMixinHooks;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FontRenderer.class, priority = 2000)
public abstract class FontRendererMixin {

    @ModifyVariable(
            method = "renderStringAtPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            ordinal = 0 // i
    )
    private int customFontColor(int original, String text, boolean shadow) {
        return REMIMixinHooks.applyCustomFormatCodes((FontRenderer) (Object) this, text, shadow, original);
    }
}
