package dev.emi.emi.search;

import com.google.common.collect.Sets;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Set;

public class TooltipQuery extends Query {
    private final Set<EmiStack> valid = Sets.newIdentityHashSet();
    private final String name;

    public TooltipQuery(String name) {
        EmiSearch.tooltips.findAll(name.toLowerCase()).forEach(s -> valid.add(s.stack));
        this.name = name.toLowerCase();
    }

    @Override
    public boolean matches(EmiStack stack) {
        return valid.contains(stack);
    }

    @Override
    public boolean matchesUnbaked(EmiStack stack) {
        for (Text text : getText(stack)) {
            if (text.getString().toLowerCase().contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static List<Text> getText(EmiStack stack) {
        List<Text> lines = stack.getTooltipText();
        if (lines.isEmpty()) {
            return lines;
        } else {
            return lines.subList(1, lines.size());
        }
    }
}
