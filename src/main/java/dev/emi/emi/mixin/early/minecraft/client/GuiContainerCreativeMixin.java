package dev.emi.emi.mixin.early.minecraft.client;

import dev.emi.emi.mixinsupport.inject_interface.EmiSearchInput;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainerCreative.class)
public class GuiContainerCreativeMixin {
    @Inject(method = "handleMouseInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I",
                    remap = false
            ),
            cancellable = true
    )
    public void handleMouseInput(CallbackInfo ci) {
        if (((EmiSearchInput) this).getEMIMouseInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At(value = "HEAD"), cancellable = true)
    public void blockEMISearchToCreativeSearch(CallbackInfo ci) {
        if (((EmiSearchInput) this).getEMISearchInput()) {
            ci.cancel();
        }
    }
}
