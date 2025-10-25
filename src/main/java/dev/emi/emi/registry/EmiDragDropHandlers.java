package dev.emi.emi.registry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;
import java.util.Map;

public class EmiDragDropHandlers {
    public static Map<Class<?>, List<EmiDragDropHandler<?>>> fromClass = Maps.newHashMap();
    public static List<EmiDragDropHandler<?>> generic = Lists.newArrayList();

    public static void clear() {
        fromClass.clear();
        generic.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(GuiScreen screen, EmiIngredient stack, DrawContext draw, int mouseX, int mouseY, float delta) {
        if (fromClass.containsKey(screen.getClass())) {
            for (EmiDragDropHandler handler : fromClass.get(screen.getClass())) {
                handler.render(screen, stack, draw, mouseX, mouseY, delta);
            }
        }
        for (EmiDragDropHandler handler : generic) {
            handler.render(screen, stack, draw, mouseX, mouseY, delta);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean dropStack(GuiScreen screen, EmiIngredient stack, int x, int y) {
        if (fromClass.containsKey(screen.getClass())) {
            for (EmiDragDropHandler handler : fromClass.get(screen.getClass())) {
                if (handler.dropStack(screen, stack, x, y)) {
                    return true;
                }
            }
        }
        for (EmiDragDropHandler handler : generic) {
            if (handler.dropStack(screen, stack, x, y)) {
                return true;
            }
        }
        return false;
    }
}
