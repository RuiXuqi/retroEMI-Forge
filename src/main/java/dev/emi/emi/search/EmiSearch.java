package dev.emi.emi.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rewindmc.retroemi.RetroEMI;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.data.EmiAlias;
import dev.emi.emi.data.EmiData;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.search.SuffixArray;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.text.Text;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmiSearch {
    public static final Pattern TOKENS = Pattern.compile(
            "-?[@#$]?" // Any query can be negated or prefixed with type
                    + "(" // Query contents
                    + "\\/(\\\\.|[^\\\\\\/])+\\/" // Any regex contents, for example `/some thing/`
                    + "|"
                    + "\\\"(\\.|[^\\\"])+\\\"" // Any quoted contents, for example, `"some thing"`
                    + "|"
                    + "[^\\s|]+" // Any raw contents, split on space
                    + "|"
                    + "\\|" // Literal OR symbol
                    + "|"
                    + "\\&" // Literal AND symbol (currently ignored since queries AND by deafult, but parsed)
                    + ")");
    private static volatile SearchWorker currentWorker = null;
    public static volatile Thread searchThread = null;
    public static volatile List<? extends EmiIngredient> stacks = EmiStackList.stacks;
    public static volatile CompiledQuery compiledQuery;
    public static Set<EmiStack> bakedStacks;
    public static SuffixArray<SearchStack> names, tooltips, mods;
    public static SuffixArray<EmiStack> aliases;

    public static void bake() {
        SuffixArray<SearchStack> names = new SuffixArray<>();
        SuffixArray<SearchStack> tooltips = new SuffixArray<>();
        SuffixArray<SearchStack> mods = new SuffixArray<>();
        SuffixArray<EmiStack> aliases = new SuffixArray<>();
        Set<EmiStack> bakedStacks = Sets.newIdentityHashSet();
        boolean old = EmiConfig.appendItemModId;
        EmiConfig.appendItemModId = false;
        for (EmiStack stack : EmiStackList.stacks) {
            try {
                SearchStack searchStack = new SearchStack(stack);
                bakedStacks.add(stack);
                Text name = NameQuery.getText(stack);
                if (name != null) {
                    names.add(searchStack, name.getString().toLowerCase());
                }
                List<Text> tooltip = stack.getTooltipText();
                if (tooltip != null) {
                    for (int i = 1; i < tooltip.size(); i++) {
                        Text text = tooltip.get(i);
                        if (text != null) {
                            tooltips.add(searchStack, text.getString().toLowerCase());
                        }
                    }
                }
                ResourceLocation id = stack.getId();
                if (id != null) {
                    mods.add(searchStack, EmiUtil.getModName(id.getNamespace()).toLowerCase());
                    mods.add(searchStack, id.getNamespace().toLowerCase());
                    names.add(searchStack, id.getPath().toLowerCase());
                }
                if (stack instanceof ItemEmiStack && stack.getItemStack().getItem() == Items.ENCHANTED_BOOK) {
                    NBTTagList enchantments = stack.getNbt() != null ?
                            stack.getNbt().getTagList("StoredEnchantments", 10) : null;

                    if (enchantments != null) {
                        for (int i = 0; i < enchantments.tagCount(); i++) {
                            NBTTagCompound enchantmentTag = enchantments.getCompoundTagAt(i);
                            int enchantmentId = enchantmentTag.getShort("id");
                            Enchantment enchantment = Enchantment.getEnchantmentByID(enchantmentId);

                            if (enchantment != null) {
                                String enchantmentName = enchantment.getName();
                                String modId = "minecraft";

                                if (enchantmentName.startsWith("enchantment.")) {
                                    modId = enchantmentName.split("\\.")[1];
                                }

                                if (!modId.equals("minecraft")) {
                                    mods.add(searchStack, modId.toLowerCase());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                EmiLog.error("EMI caught an exception while baking search for " + stack, e);
            }
        }
        for (Supplier<EmiAlias> supplier : EmiData.aliases) {
            EmiAlias alias = supplier.get();
            for (String key : alias.keys()) {
                if (!I18n.hasKey(key)) {
                    EmiReloadLog.warn("Untranslated alias " + key);
                }
                String text = RetroEMI.translate(key).toLowerCase();
                for (EmiIngredient ing : alias.stacks()) {
                    for (EmiStack stack : ing.getEmiStacks()) {
                        aliases.add(stack.copy().comparison(EmiPort.compareStrict()), text);
                    }
                }
            }
        }
        for (EmiAlias.Baked alias : EmiStackList.registryAliases) {
            for (Text text : alias.text()) {
                for (EmiIngredient ing : alias.stacks()) {
                    for (EmiStack stack : ing.getEmiStacks()) {
                        aliases.add(stack.copy().comparison(EmiPort.compareStrict()), text.getString().toLowerCase());
                    }
                }
            }
        }
        EmiConfig.appendItemModId = old;
        names.build();
        tooltips.build();
        mods.build();
        aliases.build();
        EmiSearch.names = names;
        EmiSearch.tooltips = tooltips;
        EmiSearch.mods = mods;
        EmiSearch.aliases = aliases;
        EmiSearch.bakedStacks = bakedStacks;
    }

    public static void update() {
        search(EmiScreenManager.search.getText());
    }

    public static void search(String query) {
        synchronized (EmiSearch.class) {
            SearchWorker worker = new SearchWorker(query, EmiScreenManager.getSearchSource());
            currentWorker = worker;

            searchThread = new Thread(worker);
            searchThread.setDaemon(true);
            searchThread.start();
        }
    }

    public static void apply(SearchWorker worker, List<? extends EmiIngredient> stacks) {
        synchronized (EmiSearch.class) {
            if (worker == currentWorker) {
                EmiSearch.stacks = stacks;
                currentWorker = null;
                searchThread = null;
            }
        }
    }

    public static class CompiledQuery {
        public final Query fullQuery;

        public CompiledQuery(String query) {
            List<Query> full = Lists.newArrayList();
            List<Query> queries = Lists.newArrayList();
            Matcher matcher = TOKENS.matcher(query);
            while (matcher.find()) {
                String q = matcher.group();
                boolean negated = q.startsWith("-");
                if (negated) {
                    q = q.substring(1);
                }
                if (q.isEmpty()) {
                    continue;
                }
                if (q.equals("&")) {
                    // Default behavior
                    continue;
                } else if (q.equals("|")) {
                    if (!queries.isEmpty()) {
                        full.add(new LogicalAndQuery(queries));
                        queries = Lists.newArrayList();
                    }
                    continue;
                }
                QueryType type = QueryType.fromString(q);
                Function<String, Query> constructor = type.queryConstructor;
                Function<String, Query> regexConstructor = type.regexQueryConstructor;
                if (type == QueryType.DEFAULT) {
                    List<Function<String, Query>> constructors = Lists.newArrayList();
                    List<Function<String, Query>> regexConstructors = Lists.newArrayList();
                    constructors.add(constructor);
                    regexConstructors.add(regexConstructor);

                    if (EmiConfig.searchTooltipByDefault) {
                        constructors.add(QueryType.TOOLTIP.queryConstructor);
                        regexConstructors.add(QueryType.TOOLTIP.regexQueryConstructor);
                    }
                    if (EmiConfig.searchModNameByDefault) {
                        constructors.add(QueryType.MOD.queryConstructor);
                        regexConstructors.add(QueryType.MOD.regexQueryConstructor);
                    }
                    if (EmiConfig.searchTagsByDefault) {
                        constructors.add(QueryType.TAG.queryConstructor);
                        regexConstructors.add(QueryType.TAG.regexQueryConstructor);
                    }
                    // TODO add config
                    constructors.add(AliasQuery::new);
                    if (constructors.size() > 1) {
                        constructor = name -> new LogicalOrQuery(constructors.stream().map(c -> c.apply(name)).collect(Collectors.toList()));
                        regexConstructor = name -> new LogicalOrQuery(regexConstructors.stream().map(c -> c.apply(name)).collect(Collectors.toList()));
                    }
                }
                addQuery(q.substring(type.prefix.length()), negated, queries, constructor, regexConstructor);
            }
            if (!queries.isEmpty()) {
                full.add(new LogicalAndQuery(queries));
            }
            if (!full.isEmpty()) {
                fullQuery = new LogicalOrQuery(full);
            } else {
                fullQuery = null;
            }
        }

        public boolean isEmpty() {
            return fullQuery == null;
        }

        public boolean test(EmiStack stack) {
            if (fullQuery == null) {
                return true;
            } else if (EmiSearch.bakedStacks.contains(stack)) {
                return fullQuery.matches(stack);
            } else {
                return fullQuery.matchesUnbaked(stack);
            }
        }

        private static void addQuery(String s, boolean negated, List<Query> queries, Function<String, Query> normal, Function<String, Query> regex) {
            Query q;
            if (s.length() > 1 && s.startsWith("/") && s.endsWith("/")) {
                q = regex.apply(s.substring(1, s.length() - 1));
            } else if (s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) {
                q = normal.apply(s.substring(1, s.length() - 1));
            } else {
                q = normal.apply(s);
            }
            q.negated = negated;
            queries.add(q);
        }
    }

    private static class SearchWorker implements Runnable {
        private final String query;
        private final List<? extends EmiIngredient> source;

        public SearchWorker(String query, List<? extends EmiIngredient> source) {
            this.query = query;
            this.source = source;
        }

        @Override
        public void run() {
            try {
                CompiledQuery compiled = new CompiledQuery(query);
                compiledQuery = compiled;
                if (compiled.isEmpty()) {
                    apply(this, source);
                    return;
                }
                List<EmiIngredient> stacks = Lists.newArrayList();
                int processed = 0;
                for (EmiIngredient stack : source) {
                    if (processed++ >= 1024) {
                        processed = 0;
                        if (this != currentWorker) {
                            return;
                        }
                    }
                    List<EmiStack> ess = stack.getEmiStacks();
                    // TODO properly support ingredients?
                    if (ess.size() == 1) {
                        EmiStack es = ess.get(0);
                        if (compiled.test(es)) {
                            stacks.add(stack);
                        }
                    }
                }
                apply(this, Lists.newArrayList(stacks));
            } catch (Exception e) {
                EmiLog.error("Error when attempting to search:", e);
            }
        }
    }
}
