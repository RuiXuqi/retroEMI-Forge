package dev.emi.emi.data;

import com.google.gson.*;
import dev.emi.emi.EmiPort;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecipeDefaultLoader implements EmiResourceReloadListener, IResourceManagerReloadListener {
    private static final Gson GSON = new Gson();
    public static final ResourceLocation ID = EmiPort.id("emi:recipe_defaults");

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        RecipeDefaults defaults = new RecipeDefaults();
        try {
            for (IResource resource : (List<IResource>) manager.getAllResources(new ResourceLocation("emi", "recipe/defaults/emi.json"))) {
                InputStreamReader reader = new InputStreamReader(EmiPort.getInputStream(resource));
                JsonObject json = JsonHelper.deserialize(GSON, reader, JsonObject.class);
                loadDefaults(defaults, json);
            }
        } catch (Exception e) {
            EmiLog.error("Error loading recipe default file", e);
        }
        BoM.setDefaults(defaults);
    }

    @Override
    public ResourceLocation getEmiId() {
        return ID;
    }

    public static void loadDefaults(RecipeDefaults defaults, JsonObject json) {
        if (JsonHelper.getBoolean(json, "replace", false)) {
            defaults.clear();
        }
        JsonArray disabled = JsonHelper.getArray(json, "disabled", new JsonArray());
        for (JsonElement el : disabled) {
            ResourceLocation id = EmiPort.id(el.getAsString());
            defaults.remove(id);
        }
        JsonArray added = JsonHelper.getArray(json, "added", new JsonArray());
        if (JsonHelper.hasArray(json, "recipes")) {
            added.addAll(JsonHelper.getArray(json, "recipes"));
        }
        for (JsonElement el : added) {
            ResourceLocation id = EmiPort.id(el.getAsString());
            defaults.add(id);
        }
        JsonObject resolutions = JsonHelper.getObject(json, "resolutions", new JsonObject());
        for (String key : resolutions.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet())) {
            ResourceLocation id = EmiPort.id(key);
            if (JsonHelper.hasArray(resolutions, key)) {
                defaults.add(id, JsonHelper.getArray(resolutions, key));
            }
        }
        JsonObject addedTags = JsonHelper.getObject(json, "tags", new JsonObject());
        for (String key : addedTags.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet())) {
            defaults.addTag(new JsonPrimitive(key), addedTags.get(key));
        }
    }
}
