package com.rewindmc.retroemi;

import dev.emi.emi.api.EmiPlugin;

import java.util.stream.Stream;

public interface EmiMultiPlugin {

    Stream<EmiPlugin> getChildPlugins();

}
