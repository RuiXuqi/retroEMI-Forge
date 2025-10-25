package dev.emi.emi.data;

import com.github.bsideup.jabel.Desugar;
import dev.emi.emi.api.stack.EmiIngredient;

import java.util.List;
import java.util.function.Predicate;

@Desugar
public record IndexStackData(boolean disable, List<Added> added, List<EmiIngredient> removed, List<Filter> filters) {

    @Desugar
    public static record Added(EmiIngredient added, EmiIngredient after) {
    }

    @Desugar
    public static record Filter(Predicate<String> filter) {
    }
}
