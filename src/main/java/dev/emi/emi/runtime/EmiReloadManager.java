package dev.emi.emi.runtime;

import com.google.common.collect.Lists;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.*;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class EmiReloadManager {
    private static int loadedResourcesMask = 0;
    private static volatile boolean clear = false, restart = false;
    // 0 - empty, 1 - reloading, 2 - loaded, -1 - error
    private static volatile int status = 0;
    private static Thread thread;
    public static volatile Text reloadStep = EmiPort.literal("");
    public static volatile long reloadWorry = Long.MAX_VALUE;

    public static void reloadTags() {
        loadedResourcesMask |= 1;
        if (loadedResourcesMask == 3) {
            EmiLog.info("Recipes synchronized, reloading EMI");
            loadedResourcesMask = 0;
            reload();
        } else {
            EmiLog.info("Recipes synchronized, waiting for tags to reload EMI...");
        }
    }

    public static void reloadRecipes() {
        loadedResourcesMask |= 2;
        if (loadedResourcesMask == 3) {
            EmiLog.info("Tags synchronized, reloading EMI");
            loadedResourcesMask = 0;
            reload();
        } else {
            EmiLog.info("Tags synchronized, waiting for recipes to reload EMI...");
        }
    }

    public static void clear() {
        synchronized (EmiReloadManager.class) {
            loadedResourcesMask = 0;
            clear = true;
            status = 0;
            reloadWorry = Long.MAX_VALUE;
            if (thread != null && thread.isAlive()) {
                restart = true;
            } else {
                thread = new Thread(new ReloadWorker());
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    public static void reload() {
        synchronized (EmiReloadManager.class) {
            step(EmiPort.literal("Starting Reload"));
            status = 1;
            if (thread != null && thread.isAlive()) {
                restart = true;
            } else {
                clear = false;
                thread = new Thread(new ReloadWorker());
                thread.setDaemon(false);
                thread.start();
            }
        }
    }

    public static void step(Text text) {
        step(text, 5_000);
    }

    public static void step(Text text, long worry) {
        EmiLog.info(text.getString());
        reloadStep = text;
        reloadWorry = System.currentTimeMillis() + worry;
    }

    public static boolean isLoaded() {
        return status == 2 && (thread == null || !thread.isAlive());
    }

    public static int getStatus() {
        return status;
    }

    private static class ReloadWorker implements Runnable {

        @Override
        public void run() {
            int retries = 3;
            outer:
            do {
                try {
                    if (!clear) {
                        EmiLog.info("Starting EMI reload...");
                    }
                    long reloadStart = System.currentTimeMillis();
                    restart = false;
                    step(EmiPort.literal("Clearing data"));
                    EmiRecipes.clear();
                    EmiStackList.clear();
                    EmiIngredientSerializers.clear();
                    EmiExclusionAreas.clear();
                    EmiDragDropHandlers.clear();
                    EmiStackProviders.clear();
                    EmiRecipeFiller.clear();
                    EmiHidden.clear();
                    EmiTags.ADAPTERS_BY_CLASS.map().clear();
                    EmiTags.ADAPTERS_BY_REGISTRY.clear();
                    if (clear) {
                        clear = false;
                        continue;
                    }
                    Minecraft client = Minecraft.getMinecraft();
                    if (client.world == null) {
                        EmiReloadLog.warn("World is null");
                        break;
                    }
                    List<EmiPluginContainer> plugins = Lists.newArrayList();
                    plugins.addAll(EmiAgnos.getPlugins().stream()
                            .sorted((a, b) -> Integer.compare(entrypointPriority(a), entrypointPriority(b))).collect(java.util.stream.Collectors.toList()));

//					if (EmiAgnos.isModLoaded("jei")) {
//						plugins.add(new EmiPluginContainer(new JemiPlugin(), "jemi"));
//					}
                    EmiInitRegistry initRegistry = new EmiInitRegistryImpl();
                    for (EmiPluginContainer container : plugins) {
                        step(EmiPort.literal("Initializing plugin from " + container.id()), 5_000);
                        long start = System.currentTimeMillis();
                        try {
                            container.plugin().initialize(initRegistry);
                        } catch (Throwable e) {
                            EmiReloadLog.warn("Exception initializing plugin provided by " + container.id(), e);
                            if (restart) {
                                continue outer;
                            }
                            continue;
                        }
                        EmiLog.info("Initialized plugin from " + container.id() + " in " + (System.currentTimeMillis() - start) + "ms");
                    }
                    EmiHidden.reload();

                    step(EmiPort.literal("Processing tags"));
                    EmiTags.reload();

                    step(EmiPort.literal("Constructing index"));
                    EmiComparisonDefaults.comparisons = new HashMap<>();
                    EmiStackList.reload();
                    if (restart) {
                        continue;
                    }
                    EmiRegistry registry = new EmiRegistryImpl();

                    for (EmiPluginContainer container : plugins) {
                        step(EmiPort.literal("Loading plugin from " + container.id()), 10_000);
                        long start = System.currentTimeMillis();
                        try {
                            container.plugin().register(registry);
                        } catch (Throwable e) {
                            EmiReloadLog.warn("Exception loading plugin provided by " + container.id(), e);
                            if (restart) {
                                continue outer;
                            }
                            continue;
                        }
                        EmiLog.info("Reloaded plugin from " + container.id() + " in " + (System.currentTimeMillis() - start) + "ms");
                        if (restart) {
                            continue outer;
                        }
                    }
                    if (restart) {
                        continue;
                    }
                    step(EmiPort.literal("Baking index"));
                    EmiStackList.bake();
                    step(EmiPort.literal("Registering late recipes"), 10_000);
                    Consumer<EmiRecipe> registerLateRecipe = registry::addRecipe;
                    for (Consumer<Consumer<EmiRecipe>> consumer : EmiRecipes.lateRecipes) {
                        try {
                            consumer.accept(registerLateRecipe);
                        } catch (Exception e) {
                            EmiReloadLog.warn("Exception loading late recipes for plugins:", e);
                            if (restart) {
                                continue outer;
                            }
                        }
                    }
                    step(EmiPort.literal("Baking recipes"), 15_000);
                    EmiRecipes.bake();
                    BoM.reload();
                    EmiPersistentData.load();
                    step(EmiPort.literal("Baking search"), 15_000);
                    EmiSearch.bake();
                    step(EmiPort.literal("Finishing up"));
                    EmiScreenManager.search.update();
                    EmiScreenManager.forceRecalculate();
                    EmiReloadLog.bake();
                    EmiLog.info("Reloaded EMI in " + (System.currentTimeMillis() - reloadStart) + "ms");
                    status = 2;
                } catch (Throwable e) {
                    EmiReloadLog.warn("Critical error occured during reload:", e);
                    status = -1;
                    if (retries-- > 0) {
                        restart = true;
                    }
                }
            } while (restart);
            thread = null;
        }

        private final static int entrypointPriority(EmiPluginContainer container) {
            return container.id().equals("emi") ? 0 : 1;
        }
    }
}
