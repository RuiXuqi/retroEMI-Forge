package dev.emi.emi.screen.tooltip;

import com.google.common.collect.Lists;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.bom.ChanceMaterialCost;
import dev.emi.emi.bom.FlatMaterialCost;
import dev.emi.emi.bom.MaterialTree;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecipeCostTooltipComponent implements EmiTooltipComponent {
    private static final Text COST = EmiPort.translatable("emi.cost_per");
    private final List<Node> nodes = Lists.newArrayList();
    public final MaterialTree tree;
    private int maxWidth = 0;

    public RecipeCostTooltipComponent(EmiRecipe recipe) {
        tree = new MaterialTree(recipe);
        tree.batches = tree.cost.getIdealBatch(tree.goal, 1, 1);
        tree.calculateCost();
        addNodes();
    }

    public boolean shouldDisplay() {
        return !nodes.isEmpty();
    }

    public void addNodes() {
        double batches = tree.batches;
        List<FlatMaterialCost> costs = Stream.concat(
                tree.cost.costs.values().stream(),
                tree.cost.chanceCosts.values().stream()
        ).sorted((a, b) -> Integer.compare(
                EmiStackList.getIndex(a.ingredient.getEmiStacks().get(0)),
                EmiStackList.getIndex(b.ingredient.getEmiStacks().get(0))
        )).collect(Collectors.toList());
        for (FlatMaterialCost cost : costs) {
            if (cost instanceof ChanceMaterialCost cmc) {
                nodes.add(new Node(cost.ingredient, cost.amount / batches * cmc.chance, true));
            } else {
                nodes.add(new Node(cost.ingredient, cost.amount / batches, false));
            }
        }
        positionNodes();
    }

    public void positionNodes() {
        int wrapWidth = getWrapWidth();
        int padding = 8;
        int x = 0;
        int y = 10;
        maxWidth = 0;
        for (Node node : nodes) {
            int width = 16 + EmiRenderHelper.getAmountOverflow(node.text);
            if (x + width > wrapWidth) {
                x = 0;
                y += 18;
            }
            maxWidth = Math.max(maxWidth, x + width);
            node.x = x;
            node.y = y;
            x += width + padding;
        }
    }

    public int getWrapWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        if (!nodes.isEmpty()) {
            return nodes.get(nodes.size() - 1).y + 18;
        }
        return 10;
    }

    @Override
    public int getWidth(FontRenderer fontRenderer) {
        return Math.max(fontRenderer.getStringWidth(COST.asString()), maxWidth);
    }

    @Override
    public void drawTooltip(EmiDrawContext context, TooltipRenderData render) {
        for (Node node : nodes) {
            context.drawStack(node.stack, node.x, node.y);
            EmiRenderHelper.renderAmount(context, node.x, node.y, node.text);
        }
    }

    @Override
    public void drawTooltipText(TextRenderData text) {
        text.draw(COST, 0, 0, Formatting.GRAY.getColorValue(), true);
    }

    private static class Node {
        public final EmiIngredient stack;
        public final Text text;
        public int x, y;

        public Node(EmiIngredient stack, double amount, boolean chanced) {
            this.stack = stack;
            if (chanced) {
                text = EmiPort.append(EmiPort.literal("≈"), EmiRenderHelper.getAmountText(stack, amount)).formatted(Formatting.GOLD);
            } else {
                text = EmiRenderHelper.getAmountText(stack, amount);
            }
        }
    }
}
