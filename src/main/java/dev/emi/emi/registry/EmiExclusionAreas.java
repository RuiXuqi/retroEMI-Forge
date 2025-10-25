package dev.emi.emi.registry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EmiExclusionAreas {
    public static Map<Class<?>, List<EmiExclusionArea<?>>> fromClass = Maps.newHashMap();
    public static List<EmiExclusionArea<?>> generic = Lists.newArrayList();

    public static void clear() {
        fromClass.clear();
        generic.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Bounds> getExclusion(EmiScreenBase base) {
        GuiScreen screen = base.screen();
        List<Bounds> list = Lists.newArrayList();
        list.add(base.bounds());
        // EMI buttons
        list.add(new Bounds(0, screen.height - 22, base.bounds().left(), 22));
        // Search bar
        if (EmiScreenManager.search.isVisible()) {
            list.add(new Bounds(EmiScreenManager.search.x - 1, EmiScreenManager.search.y - 1, EmiScreenManager.search.getWidth() + 2, EmiScreenManager.search.getHeight() + 2));
        }
        try {
            if (fromClass.containsKey(screen.getClass())) {
                for (EmiExclusionArea exclusion : fromClass.get(screen.getClass())) {
                    exclusion.addExclusionArea(screen, addBounds(list));
                }
            }
            for (EmiExclusionArea exclusion : generic) {
                exclusion.addExclusionArea(screen, addBounds(list));
            }
        } catch (Exception e) {
            EmiLog.error("Exception thrown when adding exclusion areas", e);
        }
        return list;
    }

    private static Consumer<Bounds> addBounds(List<Bounds> list) {
        return rect -> {
            // Impossible sizing, or integer overflow
            if (rect.empty() || rect.right() <= rect.x() || rect.bottom() <= rect.y()) {
                return;
            }
            list.add(rect);
        };
    }
}
