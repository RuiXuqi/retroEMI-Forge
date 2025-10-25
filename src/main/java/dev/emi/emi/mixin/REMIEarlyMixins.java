package dev.emi.emi.mixin;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("REMIEarlyMixins")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class REMIEarlyMixins implements IFMLLoadingPlugin, IEarlyMixinLoader {

    private String[] transformerClasses;

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.emi.early.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        if (transformerClasses == null) {
//            Namer.initNames();
//            transformerClasses = AsmTransformers.getTransformers();
        }
        return transformerClasses;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
