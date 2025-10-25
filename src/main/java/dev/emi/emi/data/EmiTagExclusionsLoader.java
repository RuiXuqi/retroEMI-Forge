package dev.emi.emi.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emi.emi.EmiPort;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public class EmiTagExclusionsLoader implements EmiResourceReloadListener, IResourceManagerReloadListener {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation ID = EmiPort.id("emi:tag_exclusions");

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        TagExclusions exclusions = new TagExclusions();
//		for (ResourceLocation id : EmiPort.findResources(manager, "tag/exclusions", i -> i.endsWith(".json"))) {
//			if (!id.getResourceDomain().equals("emi")) {
//				continue;
//			}
        try {
            for (Object o : manager.getAllResources(new ResourceLocation("emi", "tag/exclusions/emi.json"))) {
                IResource resource = (IResource) o;
                InputStreamReader reader = new InputStreamReader(EmiPort.getInputStream(resource));
                JsonObject json = JsonHelper.deserialize(GSON, reader, JsonObject.class);
                try {
                    if (JsonHelper.getBoolean(json, "replace", false)) {
                        exclusions.clear();
                    }
                    for (String key : json.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList())) {
                        ResourceLocation type = new ResourceLocation(key);
                        if (JsonHelper.hasArray(json, key)) {
                            JsonArray arr = JsonHelper.getArray(json, key);
                            for (JsonElement el : arr) {
                                ResourceLocation eid = new ResourceLocation(el.getAsString());
                                if (key.equals("exclusions")) {
                                    exclusions.add(eid);
                                    if (eid.getNamespace().equals("c")) {
                                        exclusions.add(EmiPort.id("forge", eid.getPath()));
                                    }
                                } else {
                                    exclusions.add(type, eid);
                                    if (eid.getNamespace().equals("c")) {
                                        exclusions.add(type, EmiPort.id("forge", eid.getPath()));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    EmiLog.error("Error loading tag exclusions", e);
                }
            }
        } catch (Exception e) {
            EmiLog.error("Error loading tag exclusions", e);
        }
//		}
        EmiTags.exclusions = exclusions;
    }

    @Override
    public ResourceLocation getEmiId() {
        return ID;
    }
}
