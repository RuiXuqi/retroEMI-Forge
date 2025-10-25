package dev.emi.emi.mixin.early.minecraft.client;

import com.rewindmc.retroemi.REMIMixinHooks;
import dev.emi.emi.mixinsupport.inject_interface.EmiSearchInput;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class GuiContainerMixin extends GuiScreen {
    @Shadow
    public Container inventorySlots;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void addEMIWidgets(CallbackInfo ci) {
        EmiScreenManager.addWidgets(this);
    }

    @Inject(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V",
                    shift = At.Shift.AFTER
            ))
    private void renderEMIBackground(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EmiDrawContext context = EmiDrawContext.instance();
        EmiScreenManager.drawBackground(context, mouseX, mouseY, delta);
    }

    @Inject(method = "drawSlot", at = @At(value = "RETURN"))
    private void drawSlot(Slot slot, CallbackInfo ci) {
        REMIMixinHooks.drawSlot(slot);
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    public void disableHotkeyInEMISearchInput(char c, int k, CallbackInfo ci) {
        if (((EmiSearchInput) this).getEMISearchInput()) {
            ci.cancel();
        }
    }
}
