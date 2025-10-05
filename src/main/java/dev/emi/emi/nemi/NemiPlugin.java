package dev.emi.emi.nemi;

import codechicken.nei.*;
import codechicken.nei.api.API;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.GuiOpenEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EmiEntrypoint
public class NemiPlugin implements EmiPlugin {
    private static final Minecraft client = Minecraft.getMinecraft();
    public static void onLoad() {
        API.registerNEIGuiHandler(new NemiScreenHandler());
    }

    @SideOnly(Side.CLIENT)
    public static void cycleNemi() {
        EmiConfig.enabled = !EmiConfig.enabled;
        if (client.currentScreen instanceof RecipeScreen rs) {
            rs.close();
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, consumer) -> {
            final LayoutStyleMinecraft layout = (LayoutStyleMinecraft) LayoutManager.getLayoutStyle();
            final int rows = (int) Math.ceil((double) layout.buttonCount / layout.numButtons);
            final int diff = rows * 19 + 2;
            consumer.accept(new Bounds(0, 0, layout.numButtons * 19, diff));
        });
    }

    private static final Map<ItemStack, EmiStack> stackCache = new HashMap<>();

    public static EmiStack convertNEIStackToEMI(ItemStack neiStack) {
        return stackCache.computeIfAbsent(neiStack, stack ->
            EmiStack.of(stack.getItem(), stack.getItemDamage())
        );
    }

    public static ItemStack convertEMIStackToNEI(EmiStack emiStack) {
        return emiStack.getItemStack().copy();
    }

//    // 2. 书签/收藏转换
//    @Override
//    public void register(EmiRegistry registry) {
//        // 读取NEI书签
//        for (PositionedStack stack : NEIClientConfig.bookmarks) {
//            EmiStack emiStack = convertNEIStackToEMI(stack.item);
//            registry.addFavorite(emiStack);
//        }
//
//        // 双向同步监听
//        registry.addFavoriteChangeListener(stack -> {
//            ItemStack neiStack = convertEMIStackToNEI(stack);
//            NEIClientUtils.bookmarkItem(neiStack);
//        });
//    }

    // 3. 配方转换适配器
    private static class NEIRecipeHandler extends BasicRecipeHandler {
        @Override
        public List<EmiRecipe> getEMIRecipes(EmiRegistry registry) {
            List<EmiRecipe> converted = new ArrayList<>();

            for (IRecipeHandler handler : RecipeHandlers.getRecipeList()) {
                for (IRecipe recipe : handler.getRecipes()) {
                    converted.add(convertNEIRecipe(recipe));
                }
            }
            return converted;
        }

        private EmiRecipe convertNEIRecipe(IRecipe neiRecipe) {
            // 配方输入输出转换逻辑
            List<EmiIngredient> inputs = new ArrayList<>();
            for (PositionedStack stack : neiRecipe.getIngredients()) {
                inputs.add(convertNEIStackToEMI(stack.item).asIngredient());
            }

            EmiStack output = convertNEIStackToEMI(neiRecipe.getResult().item);

            return new EmiCraftingRecipe(inputs, output, "nei");
        }
    }

    // 4. 界面切换系统
    public static class GuiSwitcher {
        private static boolean showEMI = true;
        private static Object currentRecipe;

        @SubscribeEvent
        public void onGuiOpen(GuiOpenEvent event) {
            if (event.gui instanceof GuiRecipe || event.gui instanceof EmiScreen) {
                // 保存当前查看的配方
                currentRecipe = getCurrentRecipe();
            }
        }

        // 热键切换（示例：R键）
        @SubscribeEvent
        public void onKeyPress(InputEvent.KeyInputEvent event) {
            if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
                showEMI = !showEMI;
                restoreRecipeView();
            }
        }

        private void restoreRecipeView() {
            if (currentRecipe != null) {
                if (showEMI && currentRecipe instanceof IRecipe) {
                    // 打开EMI配方界面
                    EmiScreen.openRecipe((IRecipe) currentRecipe);
                } else if (!showEMI && currentRecipe instanceof EmiRecipe) {
                    // 打开NEI配方界面
                    GuiRecipe.openRecipeGui("crafting",
                        convertToNEIRecipe((EmiRecipe) currentRecipe));
                }
            }
        }
    }

    // NEI配方界面包装器
    public static class HybridRecipeGui extends GuiContainer {
        @Override
        public void drawScreen(int mouseX, int mouseY, float delta) {
            if (showEMI) {
                drawEMIInterface();
            } else {
                super.drawScreen(mouseX, mouseY, delta); // 原始NEI渲染
            }
        }

        private void drawEMIInterface() {
            // 渲染EMI界面逻辑
        }
    }
}
